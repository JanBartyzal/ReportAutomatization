"""
Azure Entra ID (formerly Azure AD) OIDC Integration

Provides OpenID Connect authentication flow for Azure Entra ID:
- Discovery of OIDC configuration from well-known endpoint
- JWT token validation (iss, aud, exp claims)
- Token refresh logic
- Organization mapping from Azure AD groups or claims
"""

import jwt
import requests
from typing import Optional, Dict, Any
from datetime import datetime, timedelta
from functools import lru_cache
from core.config import Get_Key
import logging

logger = logging.getLogger(__name__)

# Azure Entra ID Configuration from environment
AZURE_TENANT_ID = Get_Key("AZURE_TENANT_ID")
AZURE_CLIENT_ID = Get_Key("AZURE_CLIENT_ID")
AZURE_CLIENT_SECRET = Get_Key("AZURE_CLIENT_SECRET")
REDIRECT_URI = Get_Key("REDIRECT_URI", "http://localhost/api/auth/sso/callback")

# OIDC endpoints
AUTHORITY = f"https://login.microsoftonline.com/{AZURE_TENANT_ID}"
OIDC_DISCOVERY_URL = f"{AUTHORITY}/v2.0/.well-known/openid-configuration"


class OIDCError(Exception):
    """Base exception for OIDC-related errors"""
    pass


class OIDCConfigurationError(OIDCError):
    """Raised when OIDC configuration cannot be retrieved"""
    pass


class OIDCTokenValidationError(OIDCError):
    """Raised when token validation fails"""
    pass


@lru_cache(maxsize=1)
def get_oidc_configuration() -> Dict[str, Any]:
    """
    Fetch OIDC configuration from Azure Entra ID well-known endpoint.
    
    Cached to avoid repeated network calls.
    
    Returns:
        Dictionary containing OIDC configuration (issuer, jwks_uri, etc.)
    
    Raises:
        OIDCConfigurationError: If configuration cannot be retrieved
    """
    try:
        logger.info(f"Fetching OIDC configuration from {OIDC_DISCOVERY_URL}")
        response = requests.get(OIDC_DISCOVERY_URL, timeout=10)
        response.raise_for_status()
        config = response.json()
        logger.info("OIDC configuration retrieved successfully")
        return config
    except requests.RequestException as e:
        logger.error(f"Failed to fetch OIDC configuration: {e}")
        raise OIDCConfigurationError(f"Cannot retrieve OIDC configuration: {e}")


@lru_cache(maxsize=1)
def get_jwks() -> Dict[str, Any]:
    """
    Fetch JSON Web Key Set (JWKS) from Azure Entra ID.
    
    JWKS contains public keys used to verify JWT signatures.
    Cached to avoid repeated network calls.
    
    Returns:
        Dictionary containing JWKS
    
    Raises:
        OIDCConfigurationError: If JWKS cannot be retrieved
    """
    try:
        config = get_oidc_configuration()
        jwks_uri = config["jwks_uri"]
        
        logger.info(f"Fetching JWKS from {jwks_uri}")
        response = requests.get(jwks_uri, timeout=10)
        response.raise_for_status()
        jwks = response.json()
        logger.info("JWKS retrieved successfully")
        return jwks
    except (requests.RequestException, KeyError) as e:
        logger.error(f"Failed to fetch JWKS: {e}")
        raise OIDCConfigurationError(f"Cannot retrieve JWKS: {e}")


def get_authorization_url(state: str, nonce: str) -> str:
    """
    Generate Azure Entra ID authorization URL for OAuth2/OIDC flow.
    
    Args:
        state: Random state value for CSRF protection
        nonce: Random nonce value for replay attack protection
    
    Returns:
        Authorization URL to redirect user to
    """
    config = get_oidc_configuration()
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
    
    query_string = "&".join([f"{k}={v}" for k, v in params.items()])
    url = f"{auth_endpoint}?{query_string}"
    logger.info(f"Generated authorization URL with state={state}")
    return url


def exchange_code_for_token(code: str) -> Dict[str, Any]:
    """
    Exchange authorization code for access token and ID token.
    
    Args:
        code: Authorization code from OAuth2 callback
    
    Returns:
        Dictionary containing access_token, id_token, refresh_token, etc.
    
    Raises:
        OIDCError: If token exchange fails
    """
    config = get_oidc_configuration()
    token_endpoint = config["token_endpoint"]
    
    data = {
        "client_id": AZURE_CLIENT_ID,
        "client_secret": AZURE_CLIENT_SECRET,
        "code": code,
        "redirect_uri": REDIRECT_URI,
        "grant_type": "authorization_code",
    }
    
    try:
        logger.info("Exchanging authorization code for tokens")
        response = requests.post(token_endpoint, data=data, timeout=10)
        response.raise_for_status()
        tokens = response.json()
        logger.info("Token exchange successful")
        return tokens
    except requests.RequestException as e:
        logger.error(f"Token exchange failed: {e}")
        raise OIDCError(f"Token exchange failed: {e}")


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
        OIDCTokenValidationError: If token validation fails
    """
    try:
        # Get expected issuer from OIDC config
        config = get_oidc_configuration()
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
            raise OIDCTokenValidationError(f"No matching key found for kid: {kid}")
        
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
        raise OIDCTokenValidationError("Token expired")
    except jwt.InvalidAudienceError:
        logger.warning("Token validation failed: Invalid audience")
        raise OIDCTokenValidationError("Invalid audience")
    except jwt.InvalidIssuerError:
        logger.warning("Token validation failed: Invalid issuer")
        raise OIDCTokenValidationError("Invalid issuer")
    except jwt.PyJWTError as e:
        logger.error(f"Token validation failed: {e}")
        raise OIDCTokenValidationError(f"Token validation failed: {e}")
    except Exception as e:
        logger.error(f"Unexpected error during token validation: {e}")
        raise OIDCTokenValidationError(f"Token validation error: {e}")


def refresh_access_token(refresh_token: str) -> Dict[str, Any]:
    """
    Refresh access token using refresh token.
    
    Args:
        refresh_token: Refresh token from previous authentication
    
    Returns:
        Dictionary containing new access_token, id_token, etc.
    
    Raises:
        OIDCError: If token refresh fails
    """
    config = get_oidc_configuration()
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
        raise OIDCError(f"Token refresh failed: {e}")


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
        "oid": claims.get("oid"),  # Object ID (unique user identifier)
        "tid": claims.get("tid"),  # Tenant ID
        "sub": claims.get("sub"),  # Subject (unique identifier)
    }
