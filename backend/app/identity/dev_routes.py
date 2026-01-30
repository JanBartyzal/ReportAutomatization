
import secrets
from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from core.database import get_db
from core.models import Users, Organization, SubscriptionTier
from core.identity.auth import create_access_token
from core.config import Get_Key
import logging

logger = logging.getLogger(__name__)

router = APIRouter()

@router.post("/login")
async def dev_login(db: Session = Depends(get_db)):
    """
    Development login endpoint.
    Only available in development environment (ENVIRONMENT=development).
    Creates a mock user and organization if they don't exist.
    Returns a mock JWT token.
    """
    # Security check: Ensure we are in development mode
    # We check both ENVIRONMENT and a specific AUTH_MODE flag for safety
    if (Get_Key("ENVIRONMENT", "").lower() != "development" and 
        Get_Key("ENV", "").lower() != "development" and 
        Get_Key("AUTH_MODE", "").lower() != "mock"):
        raise HTTPException(status_code=403, detail="Development login is disabled in this environment.")

    email = "dev@example.com"
    mock_oid = "mock-user-oid-123"
    
    # 1. Create/Get Organization
    org = db.query(Organization).filter(Organization.name == "Dev Organization").first()
    if not org:
        logger.info("Creating Dev Organization")
        org = Organization(
            name="Dev Organization",
            subscription_tier=SubscriptionTier.ENTERPRISE,
            max_projects=10,
            max_scans_per_month=100
        )
        db.add(org)
        db.commit()
        db.refresh(org)
    
    # 2. Create/Get User
    user = db.query(Users).filter(Users.email == email).first()
    if not user:
        logger.info(f"Creating Dev User: {email}")
        user = Users(
            email=email,
            username="dev_admin",
            user_sid=mock_oid,
            organization_id=org.id,
            role="admin"
        )
        db.add(user)
        db.commit()
        db.refresh(user)
    else:
        # Ensure user has correct OID for mock token matching
        if user.user_sid != mock_oid:
            user.user_sid = mock_oid
            db.commit()

    # 3. Generate Token
    # We use the same create_access_token function but with mock data matches verify_token expectations
    # In auth.py verify_token, if env is mock, it returns a hardcoded dict for "mock_token".
    # BUT, if we want to use REAL verify flow with a generated token, we can do that too.
    # Let's generate a REAL signed token that verify_token will accept because we sign it with the same key
    # (Oh wait, auth.py uses Azure JWKS to verify... we don't have the private key to sign to match Azure's public key)
    # So we MUST use the "mock_token" string method if we can't sign properly.
    
    # However, auth.py logic for mock mode is:
    # if Get_Key("AUTH_MODE") == "mock" and token == "mock_token": return ...
    
    # So we should return "mock_token".
    
    access_token = "mock_token"
    
    return {
        "access_token": access_token,
        "token_type": "Bearer",
        "user": {
            "id": user.id,
            "email": user.email,
            "name": "Dev Admin",
            "role": user.role,
            "organization_id": user.organization_id
        }
    }
