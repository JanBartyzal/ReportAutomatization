"""Tests for MCP tools -- mock DB and AI client."""

from __future__ import annotations

from datetime import datetime, timezone
from unittest.mock import AsyncMock

import pytest

from src.mcp.tools.query_opex import query_opex_data
from src.mcp.tools.report_status import get_report_status
from src.mcp.tools.compare_periods import compare_periods


@pytest.fixture
def mock_db() -> AsyncMock:
    return AsyncMock()


async def test_query_opex_data_returns_results(mock_db: AsyncMock) -> None:
    """query_opex_data returns filtered data."""
    mock_db.execute_with_rls.return_value = [
        {
            "file_id": "file-1",
            "source_sheet": "Sheet1",
            "headers": ["Cost", "Date"],
            "metadata": {"period": "2024-Q1"},
            "created_at": datetime(2024, 3, 15, tzinfo=timezone.utc),
        }
    ]

    result = await query_opex_data(mock_db, "org-1", period="2024-Q1")

    assert result["status"] == "success"
    assert result["count"] == 1
    assert result["data"][0]["file_id"] == "file-1"


async def test_query_opex_data_empty_result(mock_db: AsyncMock) -> None:
    """query_opex_data returns empty when no data matches."""
    mock_db.execute_with_rls.return_value = []

    result = await query_opex_data(mock_db, "org-1")

    assert result["status"] == "success"
    assert result["count"] == 0


async def test_get_report_status(mock_db: AsyncMock) -> None:
    """get_report_status returns submission matrix."""
    mock_db.execute_with_rls.return_value = [
        {
            "form_version_id": "form-v1",
            "fields_submitted": 10,
            "first_submitted": datetime(2024, 3, 1, tzinfo=timezone.utc),
            "last_submitted": datetime(2024, 3, 5, tzinfo=timezone.utc),
        }
    ]

    result = await get_report_status(mock_db, "org-1", "2024-Q1")

    assert result["status"] == "success"
    assert result["submissions_count"] == 1
    assert result["submissions"][0]["form_version_id"] == "form-v1"


async def test_compare_periods(mock_db: AsyncMock) -> None:
    """compare_periods returns delta analysis."""
    mock_db.execute_with_rls.return_value = [
        {"period": "2024-Q1", "record_count": 50, "source_sheet": "Sheet1"},
        {"period": "2024-Q2", "record_count": 75, "source_sheet": "Sheet1"},
    ]

    result = await compare_periods(mock_db, "org-1", "2024-Q1", "2024-Q2")

    assert result["status"] == "success"
    assert result["delta"]["absolute"] == 25
    assert result["delta"]["percentage"] == 50.0
