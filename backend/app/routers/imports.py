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
from app.core.database import get_db
from fastapi import UploadFile, File, Depends, HTTPException, BackgroundTasks, Form
from fastapi.routing import APIRouter
from app.schemas.user import User
from app.core.models import UploadFile as DBUploadFile, Batch, BatchStatus, Document_chunks as DBDocument_chunks, Report
from uuid import UUID
import json
import datetime
from app.services.rag_service import get_embedding, json_to_markdown
from app.core.config import settings
from app.services.parsers.excel import ExcelProcessor
from app.core.security import get_current_user


router = APIRouter()
logger = logging.getLogger("uvicorn")


@router.post("/upload")
async def upload_file(
    batch_id: UUID,
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
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
    logger.info(f"File uploaded by user: {user.email} (ID: {user.oid}) to batch: {batch_id}")
    
    # Validate batch
    batch = db.query(Batch).filter(Batch.id == batch_id, Batch.oid == user.oid).first()
    if not batch:
        raise HTTPException(status_code=404, detail="Batch not found")
    if batch.status != BatchStatus.OPEN:
        raise HTTPException(status_code=400, detail=f"Batch is {batch.status}. Upload allowed only for OPEN batches.")

    # Read file data
    file_data = await file.read()
    
    # Calculate MD5 hash for deduplication
    md5hash = hashlib.md5(file_data).hexdigest()
    
    # Sanitize filename and add hash suffix
    filename = file.filename.replace(" ", "_")
    prefix_filename = filename.rsplit(".", 1)[0]
    extension_filename = filename.rsplit(".", 1)[1].lower() if "." in filename else "pptx"
    
    # Supported extensions
    if extension_filename not in ["pptx", "xlsx", "xls"]:
        raise HTTPException(status_code=400, detail=f"Unsupported file extension: {extension_filename}")
        
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
        md5hash=md5hash,
        batch_id=batch_id
    )    
    
    db.add(upload_file)
    db.commit()
    
    # Process and vectorize in background if it's an Excel file
    if extension_filename in ["xlsx", "xls"]:
        background_tasks.add_task(process_excel_background, file_path, batch_id, upload_file.id, db)
    
    return {"message": "File uploaded successfully", "user": user.oid}


async def process_excel_background(
    file_path: str,
    batch_id: UUID,
    report_id: int,
    db: Session
):
    """Process Excel in background and index for RAG."""
    try:
        processor = ExcelProcessor()
        slides = processor.parse(file_path)
        
        for slide in slides:
            # Convert to RAG-optimized format
            md_text = json_to_markdown(slide.model_dump())
            vector = get_embedding(md_text)
            
            # Store chunk with metadata
            chunk = DBDocument_chunks(
                content=md_text,
                mdata=json.dumps({
                    "report_id": report_id, 
                    "slide_index": slide.slide_index, 
                    "batch_id": str(batch_id),
                    "type": "excel_supplement"
                }),
                batch_id=batch_id,
                embedding=str(vector)
            )
            db.add(chunk)
        
        db.commit()
        logger.info(f"Successfully vectorized Excel file: {file_path}")
    except Exception as e:
        logger.error(f"Error in background processing for {file_path}: {e}")


@router.post("/uploadopex")
async def upload_opex_file(
    batch_id: UUID,
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
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
    logger.info(f"OPEX file uploaded by user: {user.email} (ID: {user.oid}) to batch: {batch_id}")
    
    # Validate batch
    batch = db.query(Batch).filter(Batch.id == batch_id, Batch.oid == user.oid).first()
    if not batch:
        raise HTTPException(status_code=404, detail="Batch not found")
    if batch.status != BatchStatus.OPEN:
        raise HTTPException(status_code=400, detail=f"Batch is {batch.status}. Upload allowed only for OPEN batches.")

    # Read file data
    file_data = await file.read()
    
    # Calculate MD5 hash for deduplication
    md5hash = hashlib.md5(file_data).hexdigest()
    
    # Sanitize filename and add hash suffix
    filename = file.filename.replace(" ", "_")
    prefix_filename = filename.rsplit(".", 1)[0]
    extension_filename = filename.rsplit(".", 1)[1].lower() if "." in filename else "xlsx"
    
    # Supported extensions
    if extension_filename not in ["pptx", "xlsx", "xls"]:
        raise HTTPException(status_code=400, detail=f"Unsupported file extension: {extension_filename}")
        
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
        md5hash=md5hash,
        batch_id=batch_id
    )    
    
    db.add(upload_file)
    db.commit()
    
    # Process and vectorize in background if it's an Excel file
    if extension_filename in ["xlsx", "xls"]:
        background_tasks.add_task(process_excel_background, file_path, batch_id, upload_file.id, db)
    
    return {"message": "File uploaded successfully", "user": user.email}


@router.post("/upload/opex/excel")
async def upload_opex_excel(
    report_id: int = Form(...),
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """
    Upload Excel file and attach it as appendix to a Report.
    
    Args:
        report_id: ID of the Report (Plan) to attach the appendix to.
        file: Excel file (.xlsx)
        user: Authenticated user
        db: Database session
        
    Returns:
        Success message and summary of processed sheets.
    """
    logger.info(f"Excel appendix upload for Report {report_id} by user {user.email}")
    
    # 1. Validate Report exists and belongs to user
    report = db.query(Report).filter(Report.id == report_id, Report.oid == user.oid).first()
    if not report:
        raise HTTPException(status_code=404, detail="Report not found or access denied")
        
    # 2. Validate File
    filename = file.filename
    if not filename.endswith((".xlsx", ".xls")):
         raise HTTPException(status_code=400, detail="Only Excel files (.xlsx, .xls) are allowed.")
         
    # 3. Save File
    upload_dir = settings.upload_dir
    os.makedirs(upload_dir, exist_ok=True)
    
    # Generate unique filename
    timestamp = datetime.datetime.now().strftime("%Y%m%d%H%M%S")
    safe_filename = f"appendix_{report_id}_{timestamp}_{filename.replace(' ', '_')}"
    file_path = os.path.join(upload_dir, safe_filename)
    
    content = await file.read()
    with open(file_path, "wb") as f:
        f.write(content)
        
    # 4. Parse Excel
    try:
        processor = ExcelProcessor()
        appendix_data = processor.parse_appendix(file_path)
        
        # Add metadata
        appendix_data["source_file"] = filename
        appendix_data["uploaded_at"] = datetime.datetime.utcnow().isoformat()
        
        # 5. Update Report
        # Ensure we don't overwrite blindly if we wanted to merge, but requirement implies 
        # "attach as appendix", usually replacing previous or adding to list.
        # The Model has 'appendix' as a JSONB dict. 
        # Specification says "The Excel data will be stored under a new appendix key".
        # We will replace the current appendix content with this new file's content.
        
        report.appendix = appendix_data
        db.commit()
        
        sheet_count = len(appendix_data.get("sheets", []))
        return {
            "message": "Excel data successfully appended.",
            "sheets_processed": sheet_count,
            "report_id": report_id
        }
        
    except Exception as e:
        logger.error(f"Failed to process Excel appendix: {e}")
        raise HTTPException(status_code=500, detail=str(e))



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
