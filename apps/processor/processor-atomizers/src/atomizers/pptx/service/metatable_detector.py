"""MetaTable reconstruction from unstructured text blocks.

Detects table-like structures in free-form text by analysing visual delimiters
(tabs, consistent spacing) and scoring the confidence of each candidate table.
Only tables meeting the confidence threshold are returned.
"""

from __future__ import annotations

import logging
import re
import uuid
from dataclasses import dataclass, field

from src.atomizers.pptx.service.pptx_parser import TableDataParsed, TableRowData, TextBlockData

logger = logging.getLogger(__name__)

# Minimum number of rows (including header) to consider something a table
_MIN_TABLE_ROWS = 2
# Minimum number of columns to consider something a table
_MIN_TABLE_COLS = 2


@dataclass(slots=True)
class _CandidateTable:
    """Internal representation of a potential table found in text."""

    lines: list[str]
    delimiter: str
    column_count: int
    header: list[str] = field(default_factory=list)
    rows: list[list[str]] = field(default_factory=list)
    confidence: float = 0.0


class MetaTableDetector:
    """Detect and reconstruct tables from unstructured text blocks.

    The detector applies multiple heuristics in order:
    1. Tab-delimited detection
    2. Pipe-delimited detection (``|``)
    3. Consistent multi-space detection (2+ spaces between "columns")

    Each candidate is scored; only those above the configured threshold are returned.

    Usage::

        detector = MetaTableDetector()
        tables = detector.detect(text_blocks)
    """

    def __init__(self, confidence_threshold: float | None = None) -> None:
        from src.common.config import Settings
        if confidence_threshold is not None:
            self._threshold = confidence_threshold
        else:
            self._threshold = Settings().metatable_confidence_threshold

    def detect(self, text_blocks: list[TextBlockData]) -> list[TableDataParsed]:
        """Analyse text blocks and return any detected tables.

        Args:
            text_blocks: Text blocks extracted from a single slide.

        Returns:
            List of ``TableDataParsed`` for blocks that look like tables,
            each with a confidence score >= threshold.
        """
        results: list[TableDataParsed] = []

        for block in text_blocks:
            text = block.text
            if not text or "\n" not in text:
                continue

            candidate = self._try_detect(text)
            if candidate is None:
                continue

            if candidate.confidence >= self._threshold:
                results.append(
                    TableDataParsed(
                        table_id=str(uuid.uuid4()),
                        headers=candidate.header,
                        rows=[TableRowData(cells=r) for r in candidate.rows],
                        confidence=candidate.confidence,
                    )
                )
                logger.debug(
                    "MetaTable detected in shape '%s' confidence=%.2f cols=%d rows=%d",
                    block.shape_name,
                    candidate.confidence,
                    candidate.column_count,
                    len(candidate.rows),
                )

        return results

    # -- detection strategies ----------------------------------------------

    def _try_detect(self, text: str) -> _CandidateTable | None:
        """Try each delimiter strategy and return the best candidate."""
        lines = [line for line in text.splitlines() if line.strip()]
        if len(lines) < _MIN_TABLE_ROWS:
            return None

        candidates: list[_CandidateTable] = []

        tab_candidate = self._try_delimiter(lines, "\t", "tab")
        if tab_candidate is not None:
            candidates.append(tab_candidate)

        pipe_candidate = self._try_pipe(lines)
        if pipe_candidate is not None:
            candidates.append(pipe_candidate)

        space_candidate = self._try_multispace(lines)
        if space_candidate is not None:
            candidates.append(space_candidate)

        if not candidates:
            return None

        return max(candidates, key=lambda c: c.confidence)

    @staticmethod
    def _try_delimiter(lines: list[str], delimiter: str, label: str) -> _CandidateTable | None:
        """Try splitting lines by a single delimiter character."""
        split_lines = [line.split(delimiter) for line in lines]
        col_counts = [len(parts) for parts in split_lines]

        if not col_counts or max(col_counts) < _MIN_TABLE_COLS:
            return None

        most_common_count = max(set(col_counts), key=col_counts.count)
        if most_common_count < _MIN_TABLE_COLS:
            return None

        matching = sum(1 for c in col_counts if c == most_common_count)
        consistency = matching / len(col_counts)
        if consistency < 0.7:
            return None

        valid_lines = [
            [cell.strip() for cell in parts]
            for parts, cc in zip(split_lines, col_counts)
            if cc == most_common_count
        ]
        if len(valid_lines) < _MIN_TABLE_ROWS:
            return None

        header = valid_lines[0]
        rows = valid_lines[1:]

        confidence = _compute_confidence(consistency, most_common_count, len(valid_lines))

        return _CandidateTable(
            lines=lines,
            delimiter=label,
            column_count=most_common_count,
            header=header,
            rows=rows,
            confidence=confidence,
        )

    @staticmethod
    def _try_pipe(lines: list[str]) -> _CandidateTable | None:
        """Detect pipe-delimited tables (e.g. Markdown tables)."""
        pipe_lines = [line for line in lines if "|" in line]
        if len(pipe_lines) < _MIN_TABLE_ROWS:
            return None

        split_lines: list[list[str]] = []
        for line in pipe_lines:
            stripped = line.strip().strip("|")
            parts = [cell.strip() for cell in stripped.split("|")]
            if all(re.fullmatch(r"-+:?|:?-+:?", p) for p in parts if p):
                continue
            split_lines.append(parts)

        if len(split_lines) < _MIN_TABLE_ROWS:
            return None

        col_counts = [len(p) for p in split_lines]
        most_common = max(set(col_counts), key=col_counts.count)
        if most_common < _MIN_TABLE_COLS:
            return None

        matching = sum(1 for c in col_counts if c == most_common)
        consistency = matching / len(col_counts)

        valid = [p for p in split_lines if len(p) == most_common]
        if len(valid) < _MIN_TABLE_ROWS:
            return None

        confidence = _compute_confidence(consistency, most_common, len(valid))
        confidence = min(confidence + 0.05, 1.0)

        return _CandidateTable(
            lines=lines,
            delimiter="pipe",
            column_count=most_common,
            header=valid[0],
            rows=valid[1:],
            confidence=confidence,
        )

    @staticmethod
    def _try_multispace(lines: list[str]) -> _CandidateTable | None:
        """Detect tables where columns are separated by 2+ spaces."""
        pattern = re.compile(r"\s{2,}")
        split_lines = [pattern.split(line.strip()) for line in lines]
        col_counts = [len(parts) for parts in split_lines]

        if not col_counts or max(col_counts) < _MIN_TABLE_COLS:
            return None

        most_common = max(set(col_counts), key=col_counts.count)
        if most_common < _MIN_TABLE_COLS:
            return None

        matching = sum(1 for c in col_counts if c == most_common)
        consistency = matching / len(col_counts)
        if consistency < 0.75:
            return None

        valid = [parts for parts, cc in zip(split_lines, col_counts) if cc == most_common]
        if len(valid) < _MIN_TABLE_ROWS:
            return None

        confidence = _compute_confidence(consistency, most_common, len(valid)) * 0.9

        return _CandidateTable(
            lines=lines,
            delimiter="multispace",
            column_count=most_common,
            header=valid[0],
            rows=valid[1:],
            confidence=confidence,
        )


def _compute_confidence(consistency: float, col_count: int, row_count: int) -> float:
    """Compute a confidence score for a candidate table."""
    score = consistency * 0.6

    if col_count >= 3:
        score += 0.2
    elif col_count >= 2:
        score += 0.1

    if row_count >= 5:
        score += 0.2
    elif row_count >= 3:
        score += 0.15
    elif row_count >= 2:
        score += 0.1

    return min(score, 1.0)
