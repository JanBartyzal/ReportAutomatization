"""MCP tool: query_opex_data -- query aggregated OPEX data.

Queries parsed_tables with RLS scoping to the user's organization.
"""

from __future__ import annotations

import logging
from typing import Any

from src.mcp.db.connection import DatabasePool

logger = logging.getLogger(__name__)


async def query_opex_data(
    db: DatabasePool,
    org_id: str,
    period: str | None = None,
    metric: str | None = None,
) -> dict[str, Any]:
    """Query aggregated OPEX data for the user's organization.

    Args:
        db: Database connection pool with RLS enforcement.
        org_id: Organization ID (from authenticated user).
        period: Optional period filter (e.g., "2024-Q1").
        metric: Optional metric to aggregate (e.g., "amount_czk").

    Returns:
        Dict with query results and metadata.
    """
    # Build query with optional filters
    conditions: list[str] = ["org_id = $1"]
    params: list[Any] = [org_id]
    param_idx = 2

    if period:
        conditions.append(f"metadata->>'period' = ${param_idx}")
        params.append(period)
        param_idx += 1

    where_clause = " AND ".join(conditions)

    query = f"""
        SELECT
            file_id,
            source_sheet,
            headers,
            metadata,
            created_at
        FROM parsed_tables
        WHERE {where_clause}
        ORDER BY created_at DESC
        LIMIT 100
    """

    try:
        rows = await db.execute_with_rls(org_id, query, params)

        # Serialize datetime objects
        results = []
        for row in rows:
            record = dict(row)
            if record.get("created_at"):
                record["created_at"] = record["created_at"].isoformat()
            results.append(record)

        return {
            "status": "success",
            "count": len(results),
            "data": results,
            "filters": {"org_id": org_id, "period": period, "metric": metric},
        }
    except Exception as e:
        logger.error("query_opex_data failed: %s", e)
        return {"status": "error", "message": str(e), "data": []}
