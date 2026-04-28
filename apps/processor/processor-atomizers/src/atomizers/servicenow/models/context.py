"""Domain models for the ServiceNow export atomizer."""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum


class ServiceNowExportFormat(str, Enum):
    """Supported ServiceNow export formats."""

    UNSPECIFIED = "UNSPECIFIED"
    CSV = "CSV"     # /api/now/table/<name>.csv
    JSON = "JSON"   # /api/now/table/<name> (Accept: application/json)
    EXCEL = "EXCEL" # ServiceNow-generated .xlsx report


@dataclass
class ServiceNowTableMetadata:
    """High-level metadata for a single table within a ServiceNow export."""

    table_index: int
    name: str
    row_count: int
    col_count: int
    snow_table_name: str = ""  # sys_table_name, if present in the export


@dataclass
class ServiceNowStructureData:
    """Result of the structure-detection pass (before full row extraction)."""

    detected_format: ServiceNowExportFormat
    tables: list[ServiceNowTableMetadata]


@dataclass
class ServiceNowTableRow:
    """A single row of extracted data."""

    row_index: int
    cells: list[str]


@dataclass
class ServiceNowParsingResult:
    """Fully extracted table content – mirrors the Excel atomizer's output."""

    table_index: int
    table_name: str
    headers: list[str]
    rows: list[ServiceNowTableRow]
    data_types: list[str] = field(default_factory=list)
    snow_table_name: str = ""
    detected_format: ServiceNowExportFormat = ServiceNowExportFormat.UNSPECIFIED
