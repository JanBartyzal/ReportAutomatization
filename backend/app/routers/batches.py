from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List
from uuid import UUID

from app.core.database import get_db
from app.identity.auth import get_current_user
from app.core.models import Batch as DBBatch, BatchStatus, UploadFile, Document_chunks
from app.schemas.user import User
from app.schemas.batch import BatchCreate, BatchOut, BatchUpdate

router = APIRouter()


@router.post("/", response_model=BatchOut, status_code=status.HTTP_201_CREATED)
async def create_batch(
    batch_in: BatchCreate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Create a new batch for the authenticated user."""
    db_batch = DBBatch(
        name=batch_in.name,
        id=user.id,
        status=BatchStatus.OPEN
    )
    db.add(db_batch)
    db.commit()
    db.refresh(db_batch)
    return db_batch


@router.get("/", response_model=List[BatchOut])
async def list_batches(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """List all batches owned by the authenticated user."""
    return db.query(DBBatch).filter(DBBatch.id == user.id).all()


@router.get("/{batch_id}", response_model=BatchOut)
async def get_batch(
    batch_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get details of a specific batch."""
    batch = db.query(DBBatch).filter(DBBatch.id == batch_id, DBBatch.id == user.id).first()
    if not batch:
        raise HTTPException(status_code=404, detail="Batch not found")
    return batch


@router.post("/{batch_id}/close", response_model=BatchOut)
async def close_batch(
    batch_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Close an open batch."""
    batch = db.query(DBBatch).filter(DBBatch.id == batch_id, DBBatch.id == user.id).first()
    if not batch:
        raise HTTPException(status_code=404, detail="Batch not found")
    
    if batch.status != BatchStatus.OPEN:
        raise HTTPException(status_code=400, detail=f"Batch is already {batch.status}")
    
    batch.status = BatchStatus.CLOSED
    db.commit()
    db.refresh(batch)
    return batch


@router.delete("/{batch_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_batch(
    batch_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Delete a batch and all its associated data (cascading)."""
    batch = db.query(DBBatch).filter(DBBatch.id == batch_id, DBBatch.id == user.id).first()
    if not batch:
        raise HTTPException(status_code=404, detail="Batch not found")
    
    # Cascade delete is handled by SQLAlchemy relationship or manual cleanup
    # Given the existing structure, we should ensure associated files and chunks are cleared.
    # If using DB-level cascades, this is simple. Here we do explicit check or rely on SQLAlchemy.
    
    db.delete(batch)
    db.commit()
    return None
