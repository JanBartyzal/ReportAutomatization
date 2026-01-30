import requests
from typing import Optional, List
from datetime import datetime, timedelta, timezone
from jose import jwt, JWTError
from fastapi import Depends, HTTPException, status, Request
from fastapi.security import OAuth2PasswordBearer
from app.schemas.user import User
from app.core.context import organization_context
from app.core.database import SessionLocal
from app.core.models import Users
from app.core.config import settings
import logging

logger = logging.getLogger(__name__)

# Configuration
TENANT_ID = settings.azure_tenant_id
CLIENT_ID = settings.azure_client_id
AUTHORITY = f"https://login.microsoftonline.com/{TENANT_ID}/v2.0"
JWKS_URL = f"{AUTHORITY}/.well-known/openid-configuration"

# Dependency Scheme
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token", auto_error=False)

# Cache for JWKS keys
_jwks_keys = {}

def get_jwks():
    global _jwks_keys
    if not _jwks_keys:
        try:
            # 1. Get OpenID Config
            resp = requests.get(JWKS_URL, timeout=5)
            if resp.status_code != 200:
                logger.error(f"Failed to fetch idC config: {resp.text}")
                return {}
            jwks_uri = resp.json().get("jwks_uri")
            
            # 2. Get Keys
            keys_resp = requests.get(jwks_uri, timeout=5)
            if keys_resp.status_code == 200:
                _jwks_keys = keys_resp.json()
                logger.info("Refreshed JWKS keys from Azure Entra ID")
        except Exception as e:
            logger.error(f"Error fetching JWKS: {e}")
            return {}
    return _jwks_keys

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=settings.access_token_expire_minutes)
    
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, settings.secret_key, algorithm=settings.algorithm)
    return encoded_jwt

def get_token_from_request(request: Request) -> Optional[str]:
    """Extract token from Cookie or Authorization header."""
    # 1. Try secure HTTP-only cookie
    token = request.cookies.get("access_token")
    if token:
        return token
        
    # 2. Try Authorization Header
    auth_header = request.headers.get("Authorization")
    if auth_header and auth_header.startswith("Bearer "):
        return auth_header.split(" ")[1]
        
    return None

def verify_token(token: str) -> dict:
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing authentication token",
            headers={"WWW-Authenticate": "Bearer"},
        )
        
    try:
        # Get Header to find Key ID (kid) to determine verification strategy
        header = jwt.get_unverified_header(token)
        alg = header.get("alg")
        
        # Strategy A: Local Session Token (HS256)
        # Used for BFF session management (Cookie-based)
        if alg == settings.algorithm:  # e.g., HS256
            try:
                payload = jwt.decode(
                    token, 
                    settings.secret_key, 
                    algorithms=[settings.algorithm]
                )
                return payload
            except JWTError as e:
                raise HTTPException(status_code=401, detail=f"Invalid session token: {str(e)}")

        # Strategy B: Azure Entra ID Token (RS256)
        # Used for service-to-service or direct frontend access (Legacy)
        kid = header.get("kid")
        if not kid:
             raise HTTPException(status_code=401, detail="Invalid token header")

        # Get Public Keys
        jwks = get_jwks()
        rsa_key = {}
        for key in jwks.get("keys", []):
            if key["kid"] == kid:
                rsa_key = {
                    "kty": key["kty"],
                    "kid": key["kid"],
                    "use": key["use"],
                    "n": key["n"],
                    "e": key["e"]
                }
                break
        
        if not rsa_key:
             # Fallback: Force refresh keys once if key not found
             logger.warning(f"Key {kid} not found, refreshing JWKS...")
             global _jwks_keys
             _jwks_keys = {}
             jwks = get_jwks()
             for key in jwks.get("keys", []):
                if key["kid"] == kid:
                    rsa_key = key
                    break
             
             if not rsa_key:
                raise HTTPException(status_code=401, detail="Invalid token key")

        # Validate Azure Token
        payload = jwt.decode(
            token,
            rsa_key,
            algorithms=["RS256"],
            audience=CLIENT_ID,
            # We allow multiple issuers (v1 or v2 endpoints) logic could be stricter
            options={"verify_iss": False} 
        )
        return payload
        
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.JWTClaimsError:
        raise HTTPException(status_code=401, detail="Invalid claims")
    except Exception as e:
        logger.error(f"Token verification unexpected error: {e}")
        raise HTTPException(status_code=401, detail="Authentication failed")

def get_current_user(
    request: Request,
    # Keep oauth2_scheme for Swagger UI compatibility, but prioritize get_token_from_request
    token_param: str = Depends(oauth2_scheme) 
) -> User:
    """
    Validates token and returns User object with context.
    """
    # Prioritize our extraction logic (Cookies > Header)
    token = get_token_from_request(request)
    if not token:
        # Fallback to param from Depends (Header-only standard FastAPI)
        token = token_param
        
    if not token:
         # Anonymous access checking could happen here
         raise HTTPException(status_code=401, detail="Not authenticated")

    payload = verify_token(token)
    
    # Create temporary session to resolve Organization based on tenant/user
    db = SessionLocal()
    org_id = None
    try:
        # Map Payload to User
        # Logic: 
        # 1. Custom session token has "user_id" and "sub" (email)
        # 2. Azure token has "id", "email"/"preferred_username"
        
        user_id = payload.get("user_id") # Present in our local tokens
        user_id = payload.get("id")    # Present in Azure tokens
        email = payload.get("sub") or payload.get("preferred_username") or payload.get("email")
        
        local_user = None
        if user_id:
            local_user = db.query(Users).filter(Users.id == user_id).first()
        elif user_id:
            local_user = db.query(Users).filter(Users.user_sid == user_id).first()
        elif email:
             local_user = db.query(Users).filter(Users.email == email).first()

        if local_user:
            # Found local user context
            if local_user.organization_id:
                org_id = local_user.organization_id
                organization_context.set(org_id)
            
            return User(
                id=str(local_user.id),
                name=local_user.username or email.split("@")[0],
                email=local_user.email,
                tenant_id=local_user.user_sid or "local",
                roles=[local_user.role] if local_user.role else ["viewer"]
            )
            
        # User not found in local DB but has valid Token (e.g. first time Azure SSO)
        # In a strict system we might deny or JIT provision
        # For now, return a basic User object from Token claims
        return User(
            id=user_id or email,
            name=payload.get("name", "Unknown"),
            email=email,
            tenant_id=payload.get("tid", "azure"),
            roles=payload.get("roles", ["viewer"])
        )

    except Exception as e:
        logger.error(f"Error resolving user context: {e}")
        raise HTTPException(status_code=500, detail="User resolution error")
    finally:
        db.close()