"""
Administrative API endpoints.

This module provides admin-only functionality including statistics
and system-wide operations.
"""

import os
import logging
from sqlalchemy import text
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.core.config import settings
from app.core.models import UploadFile, Batch, BatchStatus
from typing import Dict, Any
from fastapi import Depends
from fastapi.routing import APIRouter
from app.schemas.user import User
from app.identity.auth import get_current_user
from app.identity.rbac import verify_admin


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
    stats = {}
    
    try:
        # 1. Total Users (Count distinct ids from uploads as proxy)
        # Note: In a real Entra ID scenario, we might want to query Graph API,
        # but for now we track active users who have uploaded files.
        total_users = db.query(UploadFile.id).distinct().count()
        stats["total_users"] = total_users
    except Exception as e:
        logger.error(f"Error calculating total_users: {e}")
        stats["total_users"] = 0

    try:
        # 2. Total Files
        total_files = db.query(UploadFile).count()
        stats["total_files"] = total_files
    except Exception as e:
        logger.error(f"Error calculating total_files: {e}")
        stats["total_files"] = 0

    try:
        # 3. Total Storage
        upload_dir = settings.upload_dir
        total_size = 0
        if os.path.exists(upload_dir):
            for dirpath, dirnames, filenames in os.walk(upload_dir):
                for f in filenames:
                    fp = os.path.join(dirpath, f)
                    if not os.path.islink(fp):
                        total_size += os.path.getsize(fp)
        
        # Format size
        if total_size < 1024:
            stats["total_storage"] = f"{total_size} B"
        elif total_size < 1024**2:
            stats["total_storage"] = f"{total_size/1024:.1f} KB"
        elif total_size < 1024**3:
            stats["total_storage"] = f"{total_size/1024**2:.1f} MB"
        else:
            stats["total_storage"] = f"{total_size/1024**3:.2f} GB"
            
    except Exception as e:
        logger.error(f"Error calculating total_storage: {e}")
        stats["total_storage"] = "Unknown"

    try:
        # 4. System Health
        db.execute(text("SELECT 1"))
        stats["system_health"] = "Healthy"
    except Exception as e:
        logger.error(f"Database health check failed: {e}")
        stats["system_health"] = "Unhealthy"

    try:
        # 5. Active Jobs
        active_jobs = db.query(Batch).filter(Batch.status == BatchStatus.PROCESSING).count()
        stats["active_jobs"] = active_jobs
    except Exception as e:
        logger.error(f"Error calculating active_jobs: {e}")
        stats["active_jobs"] = 0

    return stats


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