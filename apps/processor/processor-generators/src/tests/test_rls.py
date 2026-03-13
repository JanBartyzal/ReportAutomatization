"""Tests for RLS enforcement -- cross-tenant queries return empty."""

from __future__ import annotations

from unittest.mock import AsyncMock


from src.mcp.tools.query_opex import query_opex_data


async def test_cross_tenant_query_returns_empty() -> None:
    """AC: Cross-tenant query returns empty (not error).

    When RLS is properly enforced, querying data for a different org
    should return zero results, not an error.
    """
    mock_db = AsyncMock()
    # RLS filtering means no rows returned for this org
    mock_db.execute_with_rls.return_value = []

    result = await query_opex_data(mock_db, "org-different")

    assert result["status"] == "success"
    assert result["count"] == 0
    assert result["data"] == []

    # Verify RLS context was set with the requesting org's ID
    mock_db.execute_with_rls.assert_called_once()
    call_args = mock_db.execute_with_rls.call_args
    assert call_args[0][0] == "org-different"
