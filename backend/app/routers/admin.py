"""
Administrative API endpoints.

This module provides admin-only functionality including statistics
and system-wide operations.
"""

import logging
from sqlalchemy.orm import Session
from app.db.session import get_db
from typing import Dict, Any
from fastapi import Depends
from fastapi.routing import APIRouter
from app.schemas.user import User
from app.core.security import get_current_user, verify_admin


router = APIRouter()
logger = logging.getLogger(__name__)


@router.get("/all-stats")
async def admin_stats(
    user: User = Depends(verify_admin),
    db: Session = Depends(get_db)
) -> Dict[str, str]:
    """
    Get system-wide statistics.
    
    Restricted to admin users only.
    
    Args:
        user: Authenticated admin user
        db: Database session
        
    Returns:
        Statistics summary dictionary
    """
    return {"message": "Welcome admin, you see everything."}


@router.get("/items")
async def read_items(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """
    Test endpoint for authenticated access.
    
    Args:
        user: Authenticated user
        db: Database session
        
    Returns:
        User information dictionary
    """
    return {"user": user.dict()}

@router.get("/protected")
async def protected_route(user: User = Depends(get_current_user)):
    return {"message": f"Hello {user.name}"}