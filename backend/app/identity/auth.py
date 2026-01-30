import requests
from typing import Optional
from jose import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from core.schemas import User
from core.context import organization_context
from core.database import SessionLocal
from core.models import Users, Organization
from core.config import Get_Key


# Configuration
TENANT_ID = Get_Key("AZURE_TENANT_ID")
CLIENT_ID = Get_Key("AZURE_CLIENT_ID")
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
            resp = requests.get(JWKS_URL)
            if resp.status_code != 200:
                print(f"Failed to fetch OIDC config: {resp.text}")
                return {}
            jwks_uri = resp.json().get("jwks_uri")
            
            # 2. Get Keys
            keys_resp = requests.get(jwks_uri)
            if keys_resp.status_code == 200:
                _jwks_keys = keys_resp.json()
        except Exception as e:
            print(f"Error fetching JWKS: {e}")
            return {}
    return _jwks_keys

def verify_token(token: str) -> dict:
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing authentication token",
            headers={"WWW-Authenticate": "Bearer"},
        )
        
    try:
        # Get Header to find Key ID (kid)
        header = jwt.get_unverified_header(token)
        kid = header.get("kid")
        
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
             # Fallback or strict fail
             # In dev without real Entra ID config, we might want to allow a "mock" token
             if Get_Key("AUTH_MODE") == "mock":
                 return {"oid": "mock-user-id", "name": "Mock User", "tid": "mock-tenant", "roles": ["admin"]}
             
             raise HTTPException(status_code=401, detail="Invalid token key")

        # Validate
        payload = jwt.decode(
            token,
            rsa_key,
            algorithms=["RS256"],
            audience=CLIENT_ID,
            issuer=f"https://sts.windows.net/{TENANT_ID}/" # Or v2.0 endpoint format
        )
        return payload
        
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.JWTClaimsError:
        raise HTTPException(status_code=401, detail="Invalid claims")
    except Exception as e:
        # Mock mode fallback for easy dev testing if configured
        if (Get_Key("AUTH_MODE") == "mock" or Get_Key("ENVIRONMENT") == "development" or Get_Key("ENV") == "development") and token == "mock_token":
             return {"oid": "mock-user-oid-123", "name": "Dev Admin", "tid": "mock-tenant", "roles": ["admin"]}
             
        raise HTTPException(status_code=401, detail=f"Authentication failed: {str(e)}")

def get_current_user(token: str = Depends(oauth2_scheme)) -> User:
    """
    Validates token and returns User object with context.
    """
    if not token and Get_Key("AUTH_DISABLED") == "true":
         return User(id="anon", name="Anonymous", tenant_id="default")

    payload = verify_token(token)
    
    # Create temporary session to resolve Organization based on tenant/user
    # Note: In a real Scenario, we might want to cache this lookup to avoid DB hit every request
    db = SessionLocal()
    org_id = None
    try:
        # Assuming payload['tid'] maps to Organization.azure_tenant_id or we lookup User by oid
        # For this implementation, we will lookup local User by OID
        user_oid = payload.get("oid") or payload.get("sub")
        local_user = db.query(Users).filter(Users.user_sid == user_oid).first()
        
        if local_user and local_user.organization_id:
            org_id = local_user.organization_id
            # Set Context!
            organization_context.set(org_id)
        
        # If user not found, we might want to auto-provision or fail
        # For now, if no local user, context is None (Global/Admin??) or Isolated to own?
        # Let's assume strict mode:
        # if not org_id:
        #     raise HTTPException(status_code=403, detail="User not assigned to an Organization")

    except Exception as e:
        print(f"Error resolving organization: {e}")
    finally:
        db.close()

    return User(
        id=payload.get("oid") or payload.get("sub"),
        name=payload.get("name"),
        email=payload.get("preferred_username") or payload.get("email"),
        tenant_id=payload.get("tid"), # Keep Azure Tenant ID for reference
        roles=payload.get("roles", [])
    )

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt