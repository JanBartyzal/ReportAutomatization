"""
Template aggregation service for identifying and merging tables with identical schemas.

This module provides functionality to:
- Generate schema fingerprints based on column names and data types
- Perform fuzzy matching of column headers
- Detect common schemas across multiple files
- Aggregate data from matching schemas into virtual merged datasets
"""

import hashlib
import json
import logging
import re
from typing import List, Dict, Any, Optional, Set, Tuple
from sqlalchemy.orm import Session
from thefuzz import fuzz
from app.core.models import SlideData, UploadFile, Report


logger = logging.getLogger(__name__)


class AggregationService:
    """
    Service for template aggregation and schema matching.
    
    Handles schema fingerprinting, fuzzy column matching, and virtual
    data aggregation from multiple sources with identical table structures.
    """
    
    def __init__(self, fuzzy_threshold: int = 90) -> None:
        """
        Initialize aggregation service.
        
        Args:
            fuzzy_threshold: Minimum similarity score (0-100) for fuzzy column matching
        """
        self.fuzzy_threshold = fuzzy_threshold
    
    def normalize_column_name(self, col_name: str) -> str:
        """
        Normalize column name for comparison.
        
        Converts to lowercase and removes special characters.
        
        Args:
            col_name: Original column name
            
        Returns:
            Normalized column name (lowercase, alphanumeric + underscores)
        """
        # Remove special characters and convert to lowercase
        normalized = re.sub(r'[^a-zA-Z0-9_]', '_', str(col_name).strip())
        normalized = normalized.lower()
        # Replace multiple underscores with single
        normalized = re.sub(r'_+', '_', normalized)
        # Strip leading/trailing underscores
        return normalized.strip('_')
    
    def infer_column_types(self, table_data: List[Dict[str, Any]]) -> Dict[str, str]:
        """
        Infer data types for each column from table data.
        
        Analyzes first N rows to detect numeric vs string columns.
        
        Args:
            table_data: List of row dictionaries
            
        Returns:
            Mapping of column_name → data_type ('numeric' or 'string')
        """
        if not table_data or not isinstance(table_data, list):
            return {}
        
        # Get column names from first row
        first_row = table_data[0] if table_data else {}
        columns = list(first_row.keys())
        
        type_map: Dict[str, str] = {}
        
        for col in columns:
            # Sample first 10 rows or all rows if fewer
            sample_size = min(10, len(table_data))
            numeric_count = 0
            # Skip iteration if no non-empty values
            valid_samples = 0
            for i in range(sample_size):
                value = table_data[i].get(col)
                if value is not None and str(value).strip() != "" and str(value).strip().lower() not in ["n/a", "none", "null"]:
                    valid_samples += 1
                    # Try to convert to float
                    try:
                        # Clean common currency/unit chars
                        clean_val = str(value).replace(',', '').replace('%', '').replace('$', '').replace('€', '').strip()
                        float(clean_val)
                        numeric_count += 1
                    except (ValueError, AttributeError):
                        pass
            
            # If >70% of non-empty values are numeric, consider it numeric
            if valid_samples > 0 and numeric_count / valid_samples > 0.7:
                type_map[col] = 'numeric'
            else:
                type_map[col] = 'string'
        
        return type_map
    
    def generate_schema_fingerprint(
        self, 
        headers: List[str], 
        data_types: Optional[Dict[str, str]] = None
    ) -> str:
        """
        Generate unique fingerprint for table schema.
        
        Creates SHA-256 hash based on normalized column names and data types.
        
        Args:
            headers: List of column names
            data_types: Optional mapping of column_name → data_type
            
        Returns:
            SHA-256 hash string (64 characters)
        """
        # Normalize and sort column names for consistency
        normalized_cols = sorted([self.normalize_column_name(h) for h in headers])
        
        # Normalize data_types keys if provided
        norm_data_types = {}
        if data_types:
            norm_data_types = {self.normalize_column_name(k): v for k, v in data_types.items()}
        
        # Build fingerprint string
        fingerprint_parts = []
        
        for col in normalized_cols:
            if col in norm_data_types:
                fingerprint_parts.append(f"{col}:{norm_data_types[col]}")
            else:
                fingerprint_parts.append(col)
        
        fingerprint_str = "|".join(fingerprint_parts)
        
        # Generate SHA-256 hash
        return hashlib.sha256(fingerprint_str.encode('utf-8')).hexdigest()
    
    def fuzzy_match_columns(
        self, 
        col1: str, 
        col2: str, 
        threshold: Optional[int] = None
    ) -> bool:
        """
        Check if two column names match using fuzzy string matching.
        
        Args:
            col1: First column name
            col2: Second column name
            threshold: Optional override for similarity threshold (default: self.fuzzy_threshold)
            
        Returns:
            True if similarity score >= threshold
        """
        threshold = threshold or self.fuzzy_threshold
        
        # Normalize both columns
        norm_col1 = self.normalize_column_name(col1)
        norm_col2 = self.normalize_column_name(col2)
        
        # Calculate similarity score (0-100)
        # token_sort_ratio is better for matching headers with different word orders
        similarity = fuzz.token_sort_ratio(norm_col1, norm_col2)
        
        return similarity >= threshold
    
    def extract_schema_from_table_data(
        self, 
        table_data: List[Dict[str, Any]]
    ) -> Optional[Tuple[List[str], Dict[str, str]]]:
        """
        Extract schema information from table data.
        
        Args:
            table_data: List of row dictionaries
            
        Returns:
            Tuple of (column_names, data_types) or None if invalid
        """
        if not table_data or not isinstance(table_data, list) or len(table_data) == 0:
            return None
        
        # Get column names from first row
        columns = list(table_data[0].keys())
        
        if not columns:
            return None
        
        # Infer data types
        data_types = self.infer_column_types(table_data)
        
        return (columns, data_types)
    
    def detect_common_schemas(
        self, 
        file_ids: List[int], 
        user_id: str,
        db: Session
    ) -> Dict[str, Any]:
        """
        Detect common table schemas across multiple files.
        
        Args:
            file_ids: List of file IDs to analyze
            user_id: User's Azure AD Object ID (for RLS)
            db: Database session
            
        Returns:
            Dictionary with schema information:
                - schemas: List of detected schemas with metadata
        """
        # Query SlideData for specified files with RLS
        slide_data_records = (
            db.query(SlideData, UploadFile.filename, Report.region)
            .join(Report, SlideData.report_id == Report.id)
            .join(UploadFile, Report.upload_file_id == UploadFile.id)
            .filter(UploadFile.id.in_(file_ids))
            .filter(UploadFile.owner_id == user_id)  # RLS enforcement
            .all()
        )
        
        if not slide_data_records:
            logger.warning(f"No slide data found for file_ids={file_ids}, user_id={user_id}")
            return {"schemas": []}
        
        # Group by schema fingerprint
        schema_groups: Dict[str, Dict[str, Any]] = {}
        
        for slide_data, filename, region in slide_data_records:
            # Skip if no table data
            if not slide_data.table_data:
                continue
            
            # Extract schema
            schema_info = self.extract_schema_from_table_data(slide_data.table_data)
            
            if not schema_info:
                continue
            
            columns, data_types = schema_info
            
            # Generate fingerprint
            fingerprint = self.generate_schema_fingerprint(columns, data_types)
            
            # Initialize schema group if not exists
            if fingerprint not in schema_groups:
                schema_groups[fingerprint] = {
                    "fingerprint": fingerprint,
                    "column_names": columns,
                    "data_types": data_types,
                    "total_rows": 0,
                    "source_files": set(),
                    "source_file_ids": set()
                }
            
            # Update group stats
            schema_groups[fingerprint]["total_rows"] += len(slide_data.table_data)
            schema_groups[fingerprint]["source_files"].add(filename)
            schema_groups[fingerprint]["source_file_ids"].add(slide_data.id)
        
        # Convert sets to lists for JSON serialization
        schemas_list = []
        for schema in schema_groups.values():
            schemas_list.append({
                "fingerprint": schema["fingerprint"],
                "column_names": schema["column_names"],
                "data_types": schema["data_types"],
                "total_rows": schema["total_rows"],
                "source_files": sorted(list(schema["source_files"]))
            })
        
        logger.info(f"Detected {len(schemas_list)} unique schemas across {len(file_ids)} files")
        
        return {"schemas": schemas_list}
    
    def aggregate_by_fingerprint(
        self, 
        schema_fingerprint: str, 
        user_id: str,
        db: Session
    ) -> Dict[str, Any]:
        """
        Aggregate data from all tables matching the given schema fingerprint.
        
        Performs virtual UNION ALL with source metadata preservation.
        
        Args:
            schema_fingerprint: SHA-256 fingerprint of schema
            user_id: User's Azure AD Object ID (for RLS)
            db: Database session
            
        Returns:
            Aggregated dataset with columns and rows including source metadata
        """
        # Query all SlideData with table_data for this user
        slide_data_records = (
            db.query(SlideData, UploadFile.filename, Report.region)
            .join(Report, SlideData.report_id == Report.id)
            .join(UploadFile, Report.upload_file_id == UploadFile.id)
            .filter(UploadFile.owner_id == user_id)  # RLS enforcement
            .all()
        )
        
        # Find matching tables and aggregate
        aggregated_rows: List[Dict[str, Any]] = []
        master_columns: Optional[List[str]] = None
        all_columns: Set[str] = set()
        
        for slide_data, filename, region in slide_data_records:
            if not slide_data.table_data:
                continue
            
            # Extract schema
            schema_info = self.extract_schema_from_table_data(slide_data.table_data)
            
            if not schema_info:
                continue
            
            columns, data_types = schema_info
            
            # Check if fingerprint matches
            fingerprint = self.generate_schema_fingerprint(columns, data_types)
            
            if fingerprint != schema_fingerprint:
                continue
            
            # Track all columns across all matching tables
            all_columns.update(columns)
            
            # Set master columns from first match
            if master_columns is None:
                master_columns = columns
            
            # Add rows with source metadata
            for row in slide_data.table_data:
                enriched_row = row.copy()
                enriched_row["_source_file"] = filename
                enriched_row["_slide_number"] = slide_data.slide_index
                enriched_row["_region"] = region or ""
                
                aggregated_rows.append(enriched_row)
        
        if not aggregated_rows:
            logger.warning(f"No data found for fingerprint={schema_fingerprint}")
            return {
                "schema_fingerprint": schema_fingerprint,
                "columns": [],
                "data": [],
                "row_count": 0
            }
        
        # Handle missing columns - fill with null
        final_columns = sorted(list(all_columns)) + ["_source_file", "_slide_number", "_region"]
        
        for row in aggregated_rows:
            for col in all_columns:
                if col not in row:
                    row[col] = None
        
        logger.info(
            f"Aggregated {len(aggregated_rows)} rows for fingerprint={schema_fingerprint[:8]}..."
        )
        
        return {
            "schema_fingerprint": schema_fingerprint,
            "columns": final_columns,
            "data": aggregated_rows,
            "row_count": len(aggregated_rows)
        }
