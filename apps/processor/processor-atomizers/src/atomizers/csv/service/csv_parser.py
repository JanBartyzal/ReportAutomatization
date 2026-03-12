"""CSV Parser with auto-detection of delimiter, encoding, and data types.

This module provides:
- Delimiter auto-detection (comma, semicolon, pipe, tab)
- Encoding auto-detection (UTF-8, Windows-1250, ISO-8859-2)
- Header row detection
- Data type inference per column
"""

from __future__ import annotations

import io
import logging
import re
from collections import Counter
from dataclasses import dataclass
from typing import Any

import chardet
import pandas as pd

logger = logging.getLogger(__name__)


# Supported delimiters
DELIMITERS = [",", ";", "|", "\t"]

# Supported encodings
ENCODINGS = ["utf-8", "windows-1250", "iso-8859-2", "cp1250"]


@dataclass
class CsvParsingResult:
    """Represents parsed CSV data with detected metadata."""

    detected_delimiter: str
    detected_encoding: str
    total_rows: int
    headers: list[str]
    rows: list[list[str]]
    data_types: list[str]


class CsvParser:
    """CSV parser with auto-detection of format and content characteristics."""

    def __init__(self, max_rows_to_sample: int = 1000) -> None:
        self._max_rows_to_sample = max_rows_to_sample

    def parse(self, csv_bytes: bytes) -> CsvParsingResult:
        """Parse CSV data with auto-detection."""
        encoding = self._detect_encoding(csv_bytes)
        logger.info("Detected encoding: %s", encoding)

        try:
            content = csv_bytes.decode(encoding)
        except UnicodeDecodeError:
            content = csv_bytes.decode(encoding, errors="replace")
            logger.warning("Unicode decode error, using replacement characters")

        delimiter = self._detect_delimiter(content)
        logger.info("Detected delimiter: %r", delimiter)

        try:
            df = pd.read_csv(
                io.StringIO(content),
                delimiter=delimiter,
                header="infer",
                encoding=encoding,
            )
        except Exception as e:
            logger.error("Failed to parse CSV with detected settings: %s", e)
            df = pd.read_csv(io.StringIO(content), delimiter=",", encoding=encoding)
            delimiter = ","

        headers = list(df.columns) if df.columns.tolist() else []
        rows = df.values.tolist()
        data_types = self._detect_column_types(df)

        return CsvParsingResult(
            detected_delimiter=delimiter,
            detected_encoding=encoding.upper(),
            total_rows=len(rows),
            headers=headers,
            rows=rows,
            data_types=data_types,
        )

    def _detect_encoding(self, csv_bytes: bytes) -> str:
        """Detect the encoding of CSV data."""
        result = chardet.detect(csv_bytes)
        detected = result.get("encoding", "utf-8")

        if detected:
            detected = detected.lower()
            if detected in ["windows-1252", "cp1252"]:
                return "windows-1250"
            if detected in ["iso-8859-1", "latin1"]:
                return "utf-8"

            for enc in ENCODINGS:
                if enc in detected or detected in enc:
                    return enc

        return "utf-8"

    def _detect_delimiter(self, content: str) -> str:
        """Detect the delimiter used in CSV data."""
        lines = content.split("\n")[:10]

        delimiter_counts: dict[str, list[int]] = {d: [] for d in DELIMITERS}

        for line in lines:
            line = line.strip()
            if not line:
                continue
            for delimiter in DELIMITERS:
                count = line.count(delimiter)
                delimiter_counts[delimiter].append(count)

        best_delimiter = ","
        best_score = -1

        for delimiter, counts in delimiter_counts.items():
            if not counts:
                continue

            avg_count = sum(counts) / len(counts)
            consistency = 1.0 if len(set(counts)) == 1 else 0.5
            score = avg_count * consistency

            if score > best_score:
                best_score = score
                best_delimiter = delimiter

        logger.debug("Delimiter scores: %s", {d: sum(c) / len(c) if c else 0 for d, c in delimiter_counts.items()})
        return best_delimiter

    def _detect_column_types(self, df: pd.DataFrame) -> list[str]:
        """Detect data types for each column."""
        types: list[str] = []

        for col in df.columns:
            col_type = self._detect_single_column_type(df[col])
            types.append(col_type)

        return types

    def _detect_single_column_type(self, series: pd.Series) -> str:
        """Detect the data type for a single column."""
        non_null = series.dropna()
        if len(non_null) == 0:
            return "STRING"

        sample_values = non_null.head(100).astype(str).tolist()

        try:
            numeric_count = 0
            for val in sample_values:
                val_clean = val.replace(",", ".").replace(" ", "")
                float(val_clean)
                numeric_count += 1

            if numeric_count / len(sample_values) > 0.8:
                return "NUMBER"
        except ValueError:
            pass

        date_patterns = [
            r"^\d{4}-\d{2}-\d{2}$",
            r"^\d{2}\.\d{2}\.\d{4}$",
            r"^\d{2}/\d{2}/\d{4}$",
        ]

        date_count = 0
        for val in sample_values:
            for pattern in date_patterns:
                if re.match(pattern, val):
                    date_count += 1
                    break

        if date_count / len(sample_values) > 0.8:
            return "DATE"

        currency_patterns = ["$", "\u20ac", "K\u010d", "CZK", "USD", "EUR"]
        currency_count = sum(1 for v in sample_values if any(p in v for p in currency_patterns))
        if currency_count / len(sample_values) > 0.5:
            return "CURRENCY"

        pct_count = sum(1 for v in sample_values if "%" in v)
        if pct_count / len(sample_values) > 0.5:
            return "PERCENTAGE"

        return "STRING"
