"""
Azure Entra ID (formerly Azure AD) idC Integration

Provides OpenID Connect authentication flow for Azure Entra ID:
- Discovery of idC configuration from well-known endpoint
- JWT token validation (iss, aud, exp claims)
- Token refresh logic
- Organization mapping from Azure AD groups or claims
"""

import jwt
import requests
from typing import Optional, Dict, Any
from datetime import datetime, timedelta
from functools import lru_cache
from app.core.configmanager import Get_Key
import logging

logger = logging.getLogger(__name__)

# Azure Entra ID Configuration from environment
AZURE_TENANT_ID = Get_Key("AZURE_TENANT_ID")
AZURE_CLIENT_ID = Get_Key("AZURE_CLIENT_ID")
AZURE_CLIENT_SECRET = Get_Key("AZURE_CLIENT_SECRET")
# Update default to match port 8000 and router prefix /api/auth/sso/azure
REDIRECT_URI = Get_Key("REDIRECT_URI", "http://localhost:8000/api/auth/sso/azure/callback")

# idC endpoints
AUTHORITY = f"https://login.microsoftonline.com/{AZURE_TENANT_ID}"
idC_DISCOVERY_URL = f"{AUTHORITY}/v2.0/.well-known/openid-configuration"


class idCError(Exception):
    """Base exception for idC-related errors"""
    pass


class idCConfigurationError(idCError):
    """Raised when idC configuration cannot be retrieved"""
    pass


class idCTokenValidationError(idCError):
    """Raised when token validation fails"""
    pass


@lru_cache(maxsize=1)
def get_idc_configuration() -> Dict[str, Any]:
    """
    Fetch idC configuration from Azure Entra ID well-known endpoint.
    
    Cached to avid repeated network calls.
    
    Returns:
        Dictionary containing idC configuration (issuer, jwks_uri, etc.)
    
    Raises:
        idCConfigurationError: If configuration cannot be retrieved
    """
    try:
        logger.info(f"Fetching idC configuration from {idC_DISCOVERY_URL}")
        response = requests.get(idC_DISCOVERY_URL, timeout=10)
        response.raise_for_status()
        config = response.json()
        logger.info("idC configuration retrieved successfully")
        return config
    except requests.RequestException as e:
        logger.error(f"Failed to fetch idC configuration: {e}")
        raise idCConfigurationError(f"Cannot retrieve idC configuration: {e}")


@lru_cache(maxsize=1)
def get_jwks() -> Dict[str, Any]:
    """
    Fetch JSON Web Key Set (JWKS) from Azure Entra ID.
    
    JWKS contains public keys used to verify JWT signatures.
    Cached to avid repeated network calls.
    
    Returns:
        Dictionary containing JWKS
    
    Raises:
        idCConfigurationError: If JWKS cannot be retrieved
    """
    try:
        config = get_idc_configuration()
        jwks_uri = config["jwks_uri"]
        
        logger.info(f"Fetching JWKS from {jwks_uri}")
        response = requests.get(jwks_uri, timeout=10)
        response.raise_for_status()
        jwks = response.json()
        logger.info("JWKS retrieved successfully")
        return jwks
    except (requests.RequestException, KeyError) as e:
        logger.error(f"Failed to fetch JWKS: {e}")
        raise idCConfigurationError(f"Cannot retrieve JWKS: {e}")


import base64
import hashlib
import secrets

def generate_pkce_verifier() -> str:
    """Generate PKCE code verifier (random string)."""
    return secrets.token_urlsafe(32)

def generate_pkce_challenge(verifier: str) -> str:
    """Generate PKCE code challenge from verifier (S256)."""
    digest = hashlib.sha256(verifier.encode("utf-8")).digest()
    return base64.urlsafe_b64encode(digest).decode("utf-8").rstrip("=")

def get_authorization_url(state: str, nonce: str, code_challenge: Optional[str] = None) -> str:
    """
    Generate Azure Entra ID authorization URL for OAuth2/idC flow.
    """
    config = get_idc_configuration()
    auth_endpoint = config["authorization_endpoint"]
    
    params = {
        "client_id": AZURE_CLIENT_ID,
        "response_type": "code",
        "redirect_uri": REDIRECT_URI,
        "response_mode": "query",
        "scope": "openid profile email",
        "state": state,
        "nonce": nonce,
    }
    
    if code_challenge:
        params["code_challenge"] = code_challenge
        params["code_challenge_method"] = "S256"
    
    query_string = "&".join([f"{k}={v}" for k, v in params.items()])
    url = f"{auth_endpoint}?{query_string}"
    logger.info(f"Generated authorization URL with state={state}")
    return url


def exchange_code_for_token(code: str, code_verifier: Optional[str] = None) -> Dict[str, Any]:
    """
    Exchange authorization code for access token and ID token.
    """
    config = get_idc_configuration()
    token_endpoint = config["token_endpoint"]
    
    data = {
        "client_id": AZURE_CLIENT_ID,
        "client_secret": AZURE_CLIENT_SECRET,
        "code": code,
        "redirect_uri": REDIRECT_URI,
        "grant_type": "authorization_code",
    }
    
    if code_verifier:
        data["code_verifier"] = code_verifier
    
    try:
        logger.info("Exchanging authorization code for tokens")
        response = requests.post(token_endpoint, data=data, timeout=10)
        
        # Check for Public Client error (AADSTS700025) and retry without secret if needed
        if response.status_code in [400, 401]:
            try:
                error_body = response.json()
                if "700025" in str(error_body.get("error_description", "")):
                    logger.warning("Detected Public Client (AADSTS700025), retrying token exchange without client_secret")
                    if "client_secret" in data:
                        del data["client_secret"]
                        response = requests.post(token_endpoint, data=data, timeout=10)
            except Exception:
                # If json parse fails or other error, ignore and let raise_for_status handle it
                pass

        # Detailed error logging for debugging
        if not response.ok:
             logger.error(f"Token exchange failed. Status: {response.status_code}, Body: {response.text}")
        response.raise_for_status()
        tokens = response.json()
        logger.info("Token exchange successful")
        return tokens
    except requests.RequestException as e:
        logger.error(f"Token exchange failed: {e}")
        raise idCError(f"Token exchange failed: {e}")


def validate_id_token(id_token: str) -> Dict[str, Any]:
    """
    Validate Azure Entra ID ID token (JWT).
    
    Validates:
    - Signature using JWKS public keys
    - Issuer (iss claim)
    - Audience (aud claim)
    - Expiration (exp claim)
    - Not before (nbf claim)
    
    Args:
        id_token: JWT ID token from Azure Entra ID
    
    Returns:
        Decoded token claims
    
    Raises:
        idCTokenValidationError: If token validation fails
    """
    try:
        # Get expected issuer from idC config
        config = get_idc_configuration()
        expected_issuer = config["issuer"]
        
        # Get JWKS for signature verification
        jwks = get_jwks()
        
        # Decode token header to get key ID (kid)
        unverified_header = jwt.get_unverified_header(id_token)
        kid = unverified_header.get("kid")
        
        # Find matching public key in JWKS
        signing_key = None
        for key in jwks.get("keys", []):
            if key.get("kid") == kid:
                # Convert JWK to PEM format for PyJWT
                signing_key = jwt.algorithms.RSAAlgorithm.from_jwk(key)
                break
        
        if not signing_key:
            raise idCTokenValidationError(f"No matching key found for kid: {kid}")
        
        # Validate and decode token
        decoded = jwt.decode(
            id_token,
            signing_key,
            algorithms=["RS256"],
            audience=AZURE_CLIENT_ID,
            issuer=expected_issuer,
            options={
                "verify_signature": True,
                "verify_exp": True,
                "verify_nbf": True,
                "verify_iat": True,
                "verify_aud": True,
                "verify_iss": True,
            }
        )
        
        logger.info(f"Token validated successfully for user: {decoded.get('preferred_username')}")
        return decoded
        
    except jwt.ExpiredSignatureError:
        logger.warning("Token validation failed: Token expired")
        raise idCTokenValidationError("Token expired")
    except jwt.InvalidAudienceError:
        logger.warning("Token validation failed: Invalid audience")
        raise idCTokenValidationError("Invalid audience")
    except jwt.InvalidIssuerError:
        logger.warning("Token validation failed: Invalid issuer")
        raise idCTokenValidationError("Invalid issuer")
    except jwt.PyJWTError as e:
        logger.error(f"Token validation failed: {e}")
        raise idCTokenValidationError(f"Token validation failed: {e}")
    except Exception as e:
        logger.error(f"Unexpected error during token validation: {e}")
        raise idCTokenValidationError(f"Token validation error: {e}")


def refresh_access_token(refresh_token: str) -> Dict[str, Any]:
    """
    Refresh access token using refresh token.
    
    Args:
        refresh_token: Refresh token from previous authentication
    
    Returns:
        Dictionary containing new access_token, id_token, etc.
    
    Raises:
        idCError: If token refresh fails
    """
    config = get_idc_configuration()
    token_endpoint = config["token_endpoint"]
    
    data = {
        "client_id": AZURE_CLIENT_ID,
        "client_secret": AZURE_CLIENT_SECRET,
        "refresh_token": refresh_token,
        "grant_type": "refresh_token",
    }
    
    try:
        logger.info("Refreshing access token")
        response = requests.post(token_endpoint, data=data, timeout=10)
        response.raise_for_status()
        tokens = response.json()
        logger.info("Token refresh successful")
        return tokens
    except requests.RequestException as e:
        logger.error(f"Token refresh failed: {e}")
        raise idCError(f"Token refresh failed: {e}")


def extract_organization_from_token(claims: Dict[str, Any]) -> Optional[int]:
    """
    Extract organization ID from token claims.
    
    Strategies (in order of precedence):
    1. Custom claim 'organization_id' (if configured in Azure AD)
    2. Map from Azure AD group membership
    3. Map from tenant ID (for single-tenant apps)
    
    Args:
        claims: Decoded token claims
    
    Returns:
        Organization ID or None if not found
    """
    # Strategy 1: Direct organization_id claim (requires custom claim mapping in Azure AD)
    if "organization_id" in claims:
        return claims["organization_id"]
    
    # Strategy 2: Group-based mapping (requires groups claim)
    # This would need a mapping table: azure_group_id -> organization_id
    groups = claims.get("groups", [])
    if groups:
        # TODO: Implement group-to-organization mapping
        # For now, log and return None
        logger.info(f"User belongs to Azure AD groups: {groups}")
    
    # Strategy 3: Tenant-based (for simple single-tenant scenarios)
    # If your app only serves one organization, you could hard-code this
    tenant_id = claims.get("tid")
    if tenant_id == AZURE_TENANT_ID:
        # TODO: Implement tenant-to-organization mapping
        logger.info(f"User authenticated from tenant: {tenant_id}")
    
    logger.warning("Could not extract organization_id from token claims")
    return None


def get_user_info_from_token(claims: Dict[str, Any]) -> Dict[str, Any]:
    """
    Extract user information from validated token claims.
    
    Args:
        claims: Decoded token claims
    
    Returns:
        Dictionary with user info (email, name, etc.)
    """
    return {
        "email": claims.get("email") or claims.get("preferred_username"),
        "name": claims.get("name"),
        "given_name": claims.get("given_name"),
        "family_name": claims.get("family_name"),
        "id": claims.get("id"),  # Object ID (unique user identifier)
        "tid": claims.get("tid"),  # Tenant ID
        "sub": claims.get("sub"),  # Subject (unique identifier)
    }
