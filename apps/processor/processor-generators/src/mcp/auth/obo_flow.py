"""Azure Entra ID On-Behalf-Of (OBO) token flow.

Validates incoming user tokens and exchanges them for downstream API tokens.
The AI agent never gets global access -- always scoped to user's permissions.
"""

from __future__ import annotations

import logging
import time
from dataclasses import dataclass

import httpx
import jwt
from jwt import PyJWKClient, PyJWKClientError

from src.common.config import AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID

logger = logging.getLogger(__name__)

_TOKEN_ENDPOINT = f"https://login.microsoftonline.com/{AZURE_TENANT_ID}/oauth2/v2.0/token"
_JWKS_URI = f"https://login.microsoftonline.com/{AZURE_TENANT_ID}/v2.0/.well-known/jwks"

# JWKS cache with 5-minute TTL as per spec
_JWKS_CACHE_TTL = 300  # seconds
_jwks_client: PyJWKClient | None = None
_jwks_cache_time: float = 0


@dataclass(frozen=True, slots=True)
class TokenClaims:
    """Validated claims from a user's access token."""

    user_id: str
    org_id: str
    roles: list[str]
    name: str = ""


def _get_jwks_client() -> PyJWKClient | None:
    """Get or create JWKS client with caching.

    Returns cached client if within TTL, otherwise creates new one.
    """
    global _jwks_client, _jwks_cache_time

    if not AZURE_CLIENT_ID:
        return None

    current_time = time.time()
    if _jwks_client is None or (current_time - _jwks_cache_time) > _JWKS_CACHE_TTL:
        try:
            _jwks_client = PyJWKClient(_JWKS_URI, lifespan=_JWKS_CACHE_TTL)
            _jwks_cache_time = current_time
            logger.info("JWKS client initialized with URI: %s", _JWKS_URI)
        except PyJWKClientError as e:
            logger.error("Failed to initialize JWKS client: %s", e)
            return None

    return _jwks_client


def validate_token(token: str) -> TokenClaims:
    """Validate a JWT access token and extract claims.

    In local dev mode (no AZURE_CLIENT_ID), uses a simplified validation
    that accepts any well-formed JWT.

    Args:
        token: Bearer token from the request.

    Returns:
        TokenClaims with user identity and permissions.

    Raises:
        ValueError: If the token is invalid or expired.
    """
    if not AZURE_CLIENT_ID:
        # Local dev bypass: decode without verification
        try:
            payload = jwt.decode(token, options={"verify_signature": False})
            return TokenClaims(
                user_id=payload.get("oid", payload.get("sub", "dev-user")),
                org_id=payload.get("tid", payload.get("org_id", "dev-org")),
                roles=payload.get("roles", ["admin"]),
                name=payload.get("name", "Dev User"),
            )
        except jwt.DecodeError:
            # Accept placeholder tokens in dev mode
            logger.debug("Dev mode: accepting token without validation")
            return TokenClaims(
                user_id="dev-user",
                org_id="dev-org",
                roles=["admin"],
                name="Dev User",
            )

    try:
        # Production: validate with Azure Entra ID public keys via JWKS
        jwks_client = _get_jwks_client()

        if jwks_client is not None:
            # Full JWKS validation with Azure AD keys
            signing_key = jwks_client.get_signing_key_from_jwt(token)
            payload = jwt.decode(
                token,
                signing_key.key,
                algorithms=["RS256"],
                audience=AZURE_CLIENT_ID,
                options={
                    "verify_signature": True,
                    "verify_exp": True,
                    "verify_aud": True,
                    "verify_iss": True,
                },
                issuer=f"https://login.microsoftonline.com/{AZURE_TENANT_ID}/v2.0",
            )
        else:
            # Fallback: decode without verification if JWKS unavailable
            logger.warning("JWKS client unavailable, performing limited validation")
            payload = jwt.decode(
                token,
                options={
                    "verify_signature": False,
                    "verify_exp": True,
                    "verify_aud": True,
                },
                audience=AZURE_CLIENT_ID,
            )

        return TokenClaims(
            user_id=payload.get("oid", ""),
            org_id=payload.get("tid", ""),
            roles=payload.get("roles", []),
            name=payload.get("name", ""),
        )
    except jwt.ExpiredSignatureError:
        raise ValueError("Token has expired") from None
    except jwt.InvalidAudienceError as e:
        raise ValueError(f"Invalid audience: {e}") from None
    except jwt.InvalidIssuerError as e:
        raise ValueError(f"Invalid issuer: {e}") from None
    except jwt.InvalidTokenError as e:
        raise ValueError(f"Invalid token: {e}") from e


async def exchange_token_obo(user_token: str) -> str:
    """Exchange a user's access token for a downstream API token via OBO flow.

    Args:
        user_token: The user's original access token.

    Returns:
        A new access token scoped to the user's permissions.

    Raises:
        httpx.HTTPStatusError: If the token exchange fails.
    """
    if not AZURE_CLIENT_ID:
        logger.debug("Dev mode: returning original token as OBO token")
        return user_token

    async with httpx.AsyncClient() as client:
        response = await client.post(
            _TOKEN_ENDPOINT,
            data={
                "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
                "client_id": AZURE_CLIENT_ID,
                "client_secret": AZURE_CLIENT_SECRET,
                "assertion": user_token,
                "scope": f"api://{AZURE_CLIENT_ID}/access_as_user",
                "requested_token_use": "on_behalf_of",
            },
        )
        response.raise_for_status()
        data = response.json()
        return data["access_token"]
