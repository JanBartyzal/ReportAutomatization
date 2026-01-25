"""
Pydantic schemas for template aggregation API.

This module defines request and response models for the analytics
aggregation endpoints.
"""

from typing import List, Dict, Any
from pydantic import BaseModel, Field


class AggregationPreviewRequest(BaseModel):
    """
    Request model for aggregation preview endpoint.
    
    Attributes:
        file_ids: List of file IDs to analyze for common schemas
    """
    file_ids: List[int] = Field(
        ..., 
        description="List of file IDs to analyze for schema matching",
        min_length=1
    )


class SchemaInfo(BaseModel):
    """
    Information about a detected table schema.
    
    Attributes:
        fingerprint: SHA-256 hash of the schema
        column_names: List of column names in the schema
        data_types: Mapping of column names to data types
        total_rows: Total number of rows across all matching tables
        source_files: List of filenames containing this schema
    """
    fingerprint: str = Field(..., description="SHA-256 schema fingerprint")
    column_names: List[str] = Field(..., description="Column names")
    data_types: Dict[str, str] = Field(..., description="Column data types (numeric/string)")
    total_rows: int = Field(..., description="Total rows across all sources")
    source_files: List[str] = Field(..., description="Source filenames")


class AggregationPreviewResponse(BaseModel):
    """
    Response model for aggregation preview endpoint.
    
    Attributes:
        schemas: List of detected schemas with metadata
    """
    schemas: List[SchemaInfo] = Field(..., description="Detected common schemas")


class AggregatedDataResponse(BaseModel):
    """
    Response model for aggregated data endpoint.
    
    Attributes:
        schema_fingerprint: The schema fingerprint being aggregated
        columns: List of all column names (including metadata columns)
        data: List of row dictionaries with source metadata
        row_count: Total number of rows in aggregated dataset
    """
    schema_fingerprint: str = Field(..., description="Schema fingerprint")
    columns: List[str] = Field(..., description="All column names")
    data: List[Dict[str, Any]] = Field(..., description="Aggregated row data")
    row_count: int = Field(..., description="Total row count")
