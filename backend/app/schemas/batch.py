from pydantic import BaseModel, Field
from uuid import UUID
from datetime import datetime
from typing import Optional, List
from enum import Enum


class BatchStatus(str, Enum):
    """Status of a batch processing workflow."""
    OPEN = "OPEN"
    PROCESSING = "PROCESSING"
    CLOSED = "CLOSED"


class BatchBase(BaseModel):
    """Base schema for a batch."""
    name: str = Field(..., description="Name of the batch (e.g., 'Q1 Review')")


class BatchCreate(BatchBase):
    """Schema for creating a new batch."""
    pass


class BatchUpdate(BaseModel):
    """Schema for updating a batch status."""
    name: Optional[str] = None
    status: Optional[BatchStatus] = None


class BatchOut(BatchBase):
    """Schema for batch output/response."""
    id: UUID
    status: BatchStatus
    owner_id: str
    created_at: datetime

    class Config:
        from_attributes = True
