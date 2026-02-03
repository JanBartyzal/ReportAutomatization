"""
SSO Routes for Azure Entra ID OAuth2/idC Authentication

Provides endpoints for:
- Initiating SSO login flow
- Handling OAuth2 callback
- Token refresh
"""

import os
import secrets
from fastapi import APIRouter, HTTPException, Request, Response, Depends
from fastapi.responses import RedirectResponse
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.core.models import Users, Organization
from app.identity.oidc import (
    get_authorization_url,
    exchange_code_for_token,
    validate_id_token,
    refresh_access_token,
    get_user_info_from_token,
    extract_organization_from_token,
    generate_pkce_verifier,
    generate_pkce_challenge,
    idCError,
    idCTokenValidationError
)
from pydantic import BaseModel
import logging
from app.core.configmanager import Get_Key


logger = logging.getLogger(__name__)

router = APIRouter()

# In-memory state storage (for development)
# In production, use Redis or database with expiration
_state_storage = {}


class TokenRefreshRequest(BaseModel):
    refresh_token: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "Bearer"
    expires_in: int
    refresh_token: str | None = None


@router.get("/login")
async def sso_login(request: Request):
    """
    Initiate Azure Entra ID SSO login flow.
    
    Generates authorization URL and redirects user to Azure login page.
    
    Query Parameters:
        redirect_after_login (optional): URL to redirect to after successful login
    
    Returns:
        Redirect to Azure Entra ID authorization endpoint
    """
    # Generate state and nonce for security
    state = secrets.token_urlsafe(32)
    nonce = secrets.token_urlsafe(32)
    
    # Generate PKCE verifier and challenge
    pkce_verifier = generate_pkce_verifier()
    pkce_challenge = generate_pkce_challenge(pkce_verifier)
    
    # Store state and optional redirect URL
    redirect_after = request.query_params.get("redirect_after_login", "/")
    _state_storage[state] = {
        "nonce": nonce,
        "redirect_after": redirect_after,
        "pkce_verifier": pkce_verifier, # Store verifier for callback
        "created_at": os.times().elapsed  # Simple timestamp
    }
    
    # Generate authorization URL with PKCE challenge
    auth_url = get_authorization_url(state, nonce, pkce_challenge)
    
    logger.info(f"Initiating SSO login, redirecting to Azure Entra ID with state={state}")
    return RedirectResponse(url=auth_url)


@router.get("/callback")
async def sso_callback(
    request: Request,
    response: Response,
    db: Session = Depends(get_db)
):
    """
    Handle OAuth2 callback from Azure Entra ID.
    
    Validates state, exchanges code for tokens, validates ID token,
    creates or updates user in database, and issues session cookie.
    
    Query Parameters:
        code: Authorization code from Azure
        state: State parameter for CSRF protection
        error (optional): Error from Azure if authorization failed
    
    Returns:
        Redirect to application with session cookie set
    """
    # Check for errors from Azure
    error = request.query_params.get("error")
    if error:
        error_description = request.query_params.get("error_description", "Unknown error")
        logger.error(f"SSO callback error: {error} - {error_description}")
        raise HTTPException(
            status_code=400,
            detail=f"Authentication failed: {error_description}"
        )
    
    # Get code and state
    code = request.query_params.get("code")
    state = request.query_params.get("state")
    
    if not code or not state:
        raise HTTPException(status_code=400, detail="Missing code or state parameter")
    
    # Validate state (CSRF protection)
    stored_state = _state_storage.pop(state, None)
    if not stored_state:
        logger.warning(f"Invalid or expired state: {state}")
        raise HTTPException(status_code=400, detail="Invalid or expired state parameter")
    
    nonce = stored_state["nonce"]
    redirect_after = stored_state.get("redirect_after", "/")
    pkce_verifier = stored_state.get("pkce_verifier") # Retrieve verifier
    
    try:
        # Exchange code for tokens (with verifier)
        tokens = exchange_code_for_token(code, pkce_verifier)
        id_token = tokens["id_token"]
        access_token = tokens["access_token"]
        refresh_token = tokens.get("refresh_token")
        
        # Validate ID token
        claims = validate_id_token(id_token)
        
        # Validate nonce
        if claims.get("nonce") != nonce:
            logger.warning("Nonce mismatch in ID token")
            raise HTTPException(status_code=400, detail="Invalid nonce")
        
        # Extract user info
        user_info = get_user_info_from_token(claims)
        email = user_info["email"]
        name = user_info["name"]
        azure_id = user_info["id"]  # Azure Object ID
        
        # Extract organization
        organization_id = extract_organization_from_token(claims)
        
        # Find or create user
        user = db.query(Users).filter(Users.email == email).first()
        
        if not user:
            logger.info(f"Creating new user from SSO: {email}")
            
            # If organization_id not in token, check if user should be assigned to default org
            if not organization_id:
                # Option 1: Create a default organization for new SSO users
                # Option 2: Require organization_id in token
                # For now, we'll require it
                # Pokud chceš povolit automatický vznik organizace:
                org_id = claims.get("tid")
                if not org_id:
                    org_id=Get_Key("AZURE_TENANT_ID","Default")
                org = Organization(azure_tenant_id=org_id, name="Auto-Created Org")
                db.add(org)
                db.commit()
                db.refresh(org)
                organization_id = org.id
            
            user = Users(
                email=email,
                username=email.split("@")[0],  # Use email prefix as username
                user_sid=azure_id,
                organization_id=organization_id,
                role="viewer",  # Default role for new SSO users
            )
            db.add(user)
            db.commit()
            db.refresh(user)
            logger.info(f"Created new user: {user.id} ({email})")
        else:
            # Update user's Azure id if not set
            if not user.user_sid:
                user.user_sid = azure_id
                db.commit()
            logger.info(f"Existing user logged in via SSO: {user.id} ({email})")
        
        # Generate application session token
        # (You would use your existing JWT generation logic here)
        from app.identity.auth import create_access_token
        app_token = create_access_token({"sub": user.email, "user_id": user.id})
        
        # Prepare redirect URL with token
        from urllib.parse import urlparse, parse_qs, urlencode, urlunparse
        
        # If redirect_after is absolute, use it. If relative, assume frontend at localhost:5173 for dev fallback
        # Ideally, redirect_after should always be provided correctly by the frontend.
        target_url = redirect_after
        if target_url == "/":
             # Fallback default to standard frontend port if no specific redirect was given
             target_url = "http://localhost:5173/"
             
        parsed_url = urlparse(target_url)
        query_params = parse_qs(parsed_url.query)
        query_params["access_token"] = [app_token] # Add token to query params
        
        new_query = urlencode(query_params, doseq=True)
        final_redirect_url = urlunparse((
            parsed_url.scheme,
            parsed_url.netloc,
            parsed_url.path,
            parsed_url.params,
            new_query,
            parsed_url.fragment
        ))
        
        # Set session cookie AND redirect with token
        from app.core.config import settings
        
        response = RedirectResponse(url=final_redirect_url)
        response.set_cookie(
            key="access_token",
            value=app_token,
            httponly=True,
            secure=settings.is_production,  # Only over HTTPS in production
            samesite="lax",
            max_age=3600  # 1 hour
        )
        
        # Optionally store refresh token securely (e.g., in database)
        if refresh_token:
            logger.info(f"Refresh token available for user {user.id}")
        
        logger.info(f"SSO login successful for user {user.id}, redirecting to {final_redirect_url}")
        return response
        
    except idCTokenValidationError as e:
        logger.error(f"Token validation failed: {e}")
        raise HTTPException(status_code=401, detail=f"Token validation failed: {e}")
    except idCError as e:
        logger.error(f"idC error: {e}")
        raise HTTPException(status_code=500, detail=f"Authentication error: {e}")
    except Exception as e:
        logger.error(f"Unexpected error in SSO callback: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal server error during authentication")


@router.post("/refresh")
async def refresh_token_endpoint(request: TokenRefreshRequest) -> TokenResponse:
    """
    Refresh access token using refresh token.
    
    Request Body:
        refresh_token: The refresh token from previous authentication
    
    Returns:
        New access token and refresh token
    """
    try:
        tokens = refresh_access_token(request.refresh_token)
        
        return TokenResponse(
            access_token=tokens["access_token"],
            token_type="Bearer",
            expires_in=tokens.get("expires_in", 3600),
            refresh_token=tokens.get("refresh_token")
        )
    except idCError as e:
        logger.error(f"Token refresh failed: {e}")
        raise HTTPException(status_code=401, detail="Token refresh failed")


@router.get("/logout")
async def sso_logout(response: Response):
    """
    Logout from SSO session.
    
    Clears session cookie and optionally redirects to Azure logout endpoint.
    
    Returns:
        Redirect to home page with cleared cookies
    """
    response = RedirectResponse(url="/")
    response.delete_cookie(key="access_token")
    
    # Optionally, redirect to Azure logout endpoint for full logout
    # logout_url = f"https://login.microsoftonline.com/{AZURE_TENANT_ID}/oauth2/v2.0/logout"
    # return RedirectResponse(url=logout_url)
    
    logger.info("User logged out from SSO")
    return response
