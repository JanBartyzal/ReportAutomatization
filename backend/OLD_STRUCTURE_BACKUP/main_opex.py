"""
OPEX (Operational Expenditure) API endpoints.

This module provides REST API endpoints for processing OPEX PowerPoint files.
"""

import os
import logging
from typing import List, Dict, Any
from sqlalchemy.orm import Session
from database import get_db
from fastapi import Depends, HTTPException
from fastapi.routing import APIRouter
from models import User
from userauth import get_current_user
from opex import OpexManager


router = APIRouter()
logger = logging.getLogger("uvicorn")


@router.get("/data")
async def opex_data(
    opex_id: str,
    user: User = Depends(get_current_user)
) -> Dict[str, str]:
    """
    Get OPEX data for specific report.
    
    Secured endpoint - requires authentication.
    
    Args:
        opex_id: OPEX report ID
        user: Authenticated user
        
    Returns:
        Processing status message
    """
    logger.info(f"OPEX data requested by user: {user.email} (ID: {user.oid})")
    return {"message": "Processing started", "user": user.oid}


@router.get("/run_opex")
async def run_opex(
    file_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> Dict[str, str]:
    """
    Process OPEX file.
    
    Secured endpoint - requires authentication.
    
    Args:
        file_id: ID of uploaded file to process
        user: Authenticated user
        db: Database session
        
    Returns:
        Processing status message
    """
    opex_manager = OpexManager()
    opex_manager.process_opex(file_id, db)
    return {"message": "Processing started", "user": user.oid}


@router.get("/get_file_header")
async def get_file_header(
    file_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> List[Dict[str, Any]]:
    """
    Get presentation header information (slide summaries).
    
    Secured endpoint - requires authentication.
    
    Args:
        file_id: ID of uploaded file
        user: Authenticated user
        db: Database session
        
    Returns:
        List of slide summaries
    """
    opex_manager = OpexManager()
    result = opex_manager.get_presetation_header(file_id, db)
    return result


@router.get("/get_slide_data")
async def get_slide_data(
    file_id: str,
    slide_id: int,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> List[Dict[str, Any]]:
    """
    Get detailed data for a specific slide.
    
    Secured endpoint - requires authentication.
    
    Args:
        file_id: ID of uploaded file
        slide_id: Slide index
        user: Authenticated user
        db: Database session
        
    Returns:
        List containing slide data
    """
    opex_manager = OpexManager()
    result = opex_manager.get_slide_data(file_id, slide_id, db)
    return result