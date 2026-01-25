"""
File import API endpoints.

This module handles PPTX and OPEX file uploads with MD5 deduplication
and user authentication.
"""

import os
import logging
import hashlib
from typing import List, Dict, Any
from sqlalchemy.orm import Session
from database import get_db
from fastapi import UploadFile, File, Depends, HTTPException
from fastapi.routing import APIRouter
from models import User
from userauth import get_current_user
from dbmodels import UploadFile as DBUploadFile
from core.config import settings


router = APIRouter()
logger = logging.getLogger("uvicorn")


@router.post("/upload")
async def upload_file(
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)  # FIX: Use dependency injection
) -> Dict[str, str]:
    """
    Upload PPTX file with MD5 deduplication.
    
    Secured endpoint - requires authentication.
    Files are stored with MD5 hash suffix to prevent duplicates.
    
    Args:
        file: Uploaded file
        user: Authenticated user
        db: Database session
        
    Returns:
        Success message with user ID
        
    Raises:
        HTTPException: If upload fails
    """
    logger.info(f"File uploaded by user: {user.email} (ID: {user.oid})")
    
    # Read file data
    file_data = await file.read()
    
    # Calculate MD5 hash for deduplication
    md5hash = hashlib.md5(file_data).hexdigest()
    
    # Sanitize filename and add hash suffix
    filename = file.filename.replace(" ", "_")
    prefix_filename = filename.rsplit(".", 1)[0]
    extension_filename = filename.rsplit(".", 1)[1] if "." in filename else "pptx"
    filename = f"{prefix_filename}_{md5hash}.{extension_filename}"
    
    # Ensure upload directory exists
    upload_dir = settings.upload_dir
    os.makedirs(upload_dir, exist_ok=True)
    
    file_path = os.path.join(upload_dir, filename)
    
    # Save file to disk
    with open(file_path, "wb") as f:
        f.write(file_data)

    # Save metadata to database with RLS (user OID)
    upload_file = DBUploadFile(
        oid=user.oid,
        filename=filename,
        md5hash=md5hash
    )    
    
    db.add(upload_file)
    db.commit()
    
    return {"message": "File uploaded successfully", "user": user.oid}


@router.post("/uploadopex")
async def upload_opex_file(
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)  # FIX: Use dependency injection
) -> Dict[str, Any]:
    """
    Upload OPEX file with MD5 deduplication.
    
    Secured endpoint - requires authentication.
    Similar to regular upload but tagged as OPEX file.
    
    Args:
        file: Uploaded file
        user: Authenticated user
        db: Database session
        
    Returns:
        Success message with user info
        
    Raises:
        HTTPException: If upload fails
    """
    logger.info(f"OPEX file uploaded by user: {user.email} (ID: {user.oid})")
    
    # Read file data
    file_data = await file.read()
    
    # Calculate MD5 hash for deduplication
    md5hash = hashlib.md5(file_data).hexdigest()
    
    # Sanitize filename and add hash suffix
    filename = file.filename.replace(" ", "_")
    prefix_filename = filename.rsplit(".", 1)[0]
    extension_filename = filename.rsplit(".", 1)[1] if "." in filename else "xlsx"
    filename = f"{prefix_filename}_{md5hash}.{extension_filename}"
    
    # Ensure upload directory exists
    upload_dir = settings.upload_dir
    os.makedirs(upload_dir, exist_ok=True)
    
    file_path = os.path.join(upload_dir, filename)
    
    # Save file to disk
    with open(file_path, "wb") as f:
        f.write(file_data)

    # Save metadata to database with RLS (user OID)
    upload_file = DBUploadFile(
        oid=user.oid,
        filename=filename,
        md5hash=md5hash
    )    
    
    db.add(upload_file)
    db.commit()
    
    return {"message": "File uploaded successfully", "user": user.email}


@router.get("/get-list-uploaded-files")
async def get_list_uploaded_files(
    user: User = Depends(get_current_user),  # SECURITY FIX: Add authentication
    db: Session = Depends(get_db)  # FIX: Use dependency injection
) -> List[Dict[str, Any]]:
    """
    Get list of uploaded files for current user.
    
    Secured endpoint - requires authentication.
    Returns only files uploaded by the authenticated user (RLS).
    
    Args:
        user: Authenticated user
        db: Database session
        
    Returns:
        List of file metadata dictionaries
    """
    # ROW LEVEL SECURITY: Filter by user OID
    db_files = db.query(DBUploadFile).filter(DBUploadFile.oid == user.oid).all()
    
    results = []
    for file in db_files:
        results.append({
            "id": file.id,
            "filename": file.filename,
            "md5hash": file.md5hash,
            "region": file.region.region if file.region else "",
            "created_at": file.created_at.isoformat() if file.created_at else None
        })
    
    return results
