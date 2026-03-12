"""Column data type detection by sampling rows.

Supports STRING, NUMBER, DATE, CURRENCY, and PERCENTAGE detection with
Czech locale awareness (space as thousands separator, comma as decimal).
"""

from __future__ import annotations

import re
from typing import Final

# ---------------------------------------------------------------------------
# Patterns
# ---------------------------------------------------------------------------

# Czech-style number: 1 234,56 or -1 234,56
CZECH_NUMBER: Final[re.Pattern[str]] = re.compile(r"^-?\d{1,3}(\s\d{3})*(,\d+)?$")

# Standard number: 1234.56 or -1234.56
STANDARD_NUMBER: Final[re.Pattern[str]] = re.compile(r"^-?\d+(\.\d+)?$")

# Percentage: 12.5% or 12,5 %
PERCENTAGE: Final[re.Pattern[str]] = re.compile(r"^-?\d+([.,]\d+)?\s*%$")

# Currency: $100, 100 CZK, 1 234,56 Kc, E100.00
CURRENCY: Final[re.Pattern[str]] = re.compile(
    r"^[$\u20ac\u00a3\u00a5]?\s*-?\d[\d\s.,]*\s*(K\u010d|CZK|USD|EUR|GBP)?$",
    re.IGNORECASE,
)

# Date patterns
DATE_PATTERNS: Final[list[re.Pattern[str]]] = [
    re.compile(r"^\d{4}-\d{2}-\d{2}"),  # ISO: 2024-01-15
    re.compile(r"^\d{1,2}[./]\d{1,2}[./]\d{2,4}$"),  # DD.MM.YYYY or DD/MM/YYYY
]

# Minimum proportion of samples that must match for a type to be selected
_MATCH_THRESHOLD: Final[float] = 0.6

# Maximum number of non-empty values to sample per column
_MAX_SAMPLE_SIZE: Final[int] = 20


# ---------------------------------------------------------------------------
# Type names
# ---------------------------------------------------------------------------

TYPE_STRING: Final[str] = "STRING"
TYPE_NUMBER: Final[str] = "NUMBER"
TYPE_DATE: Final[str] = "DATE"
TYPE_CURRENCY: Final[str] = "CURRENCY"
TYPE_PERCENTAGE: Final[str] = "PERCENTAGE"


# ---------------------------------------------------------------------------
# Detector
# ---------------------------------------------------------------------------


class DataTypeDetector:
    """Detect column data types by sampling cell values."""

    def detect_column_type(self, values: list[str]) -> str:
        """Sample non-empty values and vote on the most likely type."""
        non_empty = [v.strip() for v in values if v.strip()]
        if not non_empty:
            return TYPE_STRING

        sample = non_empty[:_MAX_SAMPLE_SIZE]
        sample_size = len(sample)

        percentage_count = sum(1 for v in sample if self._is_percentage(v))
        if percentage_count / sample_size > _MATCH_THRESHOLD:
            return TYPE_PERCENTAGE

        currency_count = sum(1 for v in sample if self._is_currency(v))
        if currency_count / sample_size > _MATCH_THRESHOLD:
            return TYPE_CURRENCY

        number_count = sum(1 for v in sample if self._is_number(v))
        if number_count / sample_size > _MATCH_THRESHOLD:
            return TYPE_NUMBER

        date_count = sum(1 for v in sample if self._is_date(v))
        if date_count / sample_size > _MATCH_THRESHOLD:
            return TYPE_DATE

        return TYPE_STRING

    @staticmethod
    def _is_percentage(value: str) -> bool:
        return bool(PERCENTAGE.match(value))

    @staticmethod
    def _is_currency(value: str) -> bool:
        if not CURRENCY.match(value):
            return False
        has_symbol = any(c in value for c in "$\u20ac\u00a3\u00a5")
        has_suffix = bool(
            re.search(r"(K\u010d|CZK|USD|EUR|GBP)\s*$", value, re.IGNORECASE)
        )
        return has_symbol or has_suffix

    @staticmethod
    def _is_number(value: str) -> bool:
        return bool(STANDARD_NUMBER.match(value) or CZECH_NUMBER.match(value))

    @staticmethod
    def _is_date(value: str) -> bool:
        return any(pattern.match(value) for pattern in DATE_PATTERNS)
