"""
Analytics API endpoints for template aggregation.

This module provides endpoints for detecting common table schemas
across multiple files and retrieving aggregated data.
"""

import logging
from typing import Dict, Any
from sqlalchemy.orm import Session
from fastapi import Depends, HTTPException
from fastapi.routing import APIRouter

from app.db.session import get_db
from app.schemas.user import User
from app.core.security import get_current_user
from app.services.aggregation_service import AggregationService
from app.schemas.aggregation import (
    AggregationPreviewRequest,
    AggregationPreviewResponse,
    AggregatedDataResponse
)


router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/aggregate/preview", response_model=AggregationPreviewResponse)
async def preview_aggregation(
    request: AggregationPreviewRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """
    Preview common schemas across multiple files.
    
    Analyzes the specified files to detect matching table schemas
    and provides statistics about potential aggregations.
    
    Secured endpoint - requires authentication.
    Only analyzes files owned by the authenticated user (RLS).
    
    Args:
        request: Request with list of file IDs
        user: Authenticated user
        db: Database session
        
    Returns:
        List of detected schemas with metadata
        
    Raises:
        HTTPException: 400 if no files specified, 404 if no schemas found
    """
    logger.info(
        f"Aggregation preview requested by {user.email} "
        f"for files: {request.file_ids}"
    )
    
    if not request.file_ids:
        raise HTTPException(
            status_code=400,
            detail="No file IDs provided for analysis"
        )
    
    # Initialize aggregation service
    aggregation_service = AggregationService(fuzzy_threshold=90)
    
    # Detect common schemas
    result = aggregation_service.detect_common_schemas(
        file_ids=request.file_ids,
        user_oid=user.oid,
        db=db
    )
    
    if not result["schemas"]:
        logger.warning(
            f"No common schemas found for files: {request.file_ids}, "
            f"user: {user.oid}"
        )
        # Return empty list instead of 404 to match specification
        return {"schemas": []}
    
    logger.info(
        f"Found {len(result['schemas'])} common schemas "
        f"for user {user.email}"
    )
    
    return result


@router.get("/aggregate/{schema_fingerprint}", response_model=AggregatedDataResponse)
async def get_aggregated_data(
    schema_fingerprint: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """
    Retrieve aggregated data for a specific schema fingerprint.
    
    Performs virtual UNION ALL across all tables matching the given
    schema fingerprint. Includes source metadata for traceability.
    
    Secured endpoint - requires authentication.
    Only aggregates data from user's own files (RLS).
    
    Args:
        schema_fingerprint: SHA-256 fingerprint of the schema
        user: Authenticated user
        db: Database session
        
    Returns:
        Aggregated dataset with columns, rows, and source metadata
        
    Raises:
        HTTPException: 400 if invalid fingerprint, 404 if no data found
    """
    logger.info(
        f"Aggregated data requested by {user.email} "
        f"for fingerprint: {schema_fingerprint[:16]}..."
    )
    
    # Validate fingerprint format (SHA-256 = 64 hex chars)
    if not schema_fingerprint or len(schema_fingerprint) != 64:
        raise HTTPException(
            status_code=400,
            detail="Invalid schema fingerprint format (expected 64-character SHA-256 hash)"
        )
    
    # Initialize aggregation service
    aggregation_service = AggregationService(fuzzy_threshold=90)
    
    # Aggregate data
    result = aggregation_service.aggregate_by_fingerprint(
        schema_fingerprint=schema_fingerprint,
        user_oid=user.oid,
        db=db
    )
    
    if result["row_count"] == 0:
        raise HTTPException(
            status_code=404,
            detail=f"No data found for schema fingerprint: {schema_fingerprint}"
        )
    
    logger.info(
        f"Returning {result['row_count']} aggregated rows "
        f"for user {user.email}"
    )
    
    return result
