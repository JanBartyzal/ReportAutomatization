"""MCP tool: compare_periods -- compare OPEX data between two periods."""

from __future__ import annotations

import logging
from typing import Any

from src.mcp.db.connection import DatabasePool

logger = logging.getLogger(__name__)


async def compare_periods(
    db: DatabasePool,
    org_id: str,
    period_a: str,
    period_b: str,
    metric: str | None = None,
) -> dict[str, Any]:
    """Compare OPEX data between two reporting periods.

    Args:
        db: Database connection pool with RLS enforcement.
        org_id: Organization ID (from authenticated user).
        period_a: First period identifier.
        period_b: Second period identifier.
        metric: Optional metric to compare (e.g., "amount_czk").

    Returns:
        Dict with comparison data including deltas.
    """
    try:
        sql = """
            SELECT
                metadata->>'period' AS period,
                COUNT(*) AS record_count,
                source_sheet
            FROM parsed_tables
            WHERE org_id = $1
              AND metadata->>'period' IN ($2, $3)
            GROUP BY metadata->>'period', source_sheet
            ORDER BY metadata->>'period', source_sheet
        """

        rows = await db.execute_with_rls(org_id, sql, [org_id, period_a, period_b])

        # Group by period
        period_a_data: list[dict[str, Any]] = []
        period_b_data: list[dict[str, Any]] = []

        for row in rows:
            record = dict(row)
            if record.get("period") == period_a:
                period_a_data.append(record)
            elif record.get("period") == period_b:
                period_b_data.append(record)

        # Compute summary deltas
        a_total = sum(r.get("record_count", 0) for r in period_a_data)
        b_total = sum(r.get("record_count", 0) for r in period_b_data)
        delta = b_total - a_total
        delta_pct = (delta / a_total * 100) if a_total > 0 else 0.0

        return {
            "status": "success",
            "period_a": {
                "id": period_a,
                "record_count": a_total,
                "sheets": period_a_data,
            },
            "period_b": {
                "id": period_b,
                "record_count": b_total,
                "sheets": period_b_data,
            },
            "delta": {
                "absolute": delta,
                "percentage": round(delta_pct, 2),
            },
            "metric": metric,
        }
    except Exception as e:
        logger.error("compare_periods failed: %s", e)
        return {"status": "error", "message": str(e)}
