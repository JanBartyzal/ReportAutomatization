"""MCP tool: get_report_status -- get submission matrix for a reporting period."""

from __future__ import annotations

import logging
from typing import Any

from src.mcp.db.connection import DatabasePool

logger = logging.getLogger(__name__)


async def get_report_status(
    db: DatabasePool,
    org_id: str,
    period_id: str,
) -> dict[str, Any]:
    """Get the submission status matrix for a reporting period.

    Args:
        db: Database connection pool with RLS enforcement.
        org_id: Organization ID (from authenticated user).
        period_id: Period identifier to check status for.

    Returns:
        Dict with submission matrix data.
    """
    try:
        sql = """
            SELECT
                form_version_id,
                COUNT(DISTINCT field_id) AS fields_submitted,
                MIN(submitted_at) AS first_submitted,
                MAX(submitted_at) AS last_submitted
            FROM form_responses
            WHERE org_id = $1
              AND period_id = $2
            GROUP BY form_version_id
            ORDER BY form_version_id
        """

        rows = await db.execute_with_rls(org_id, sql, [org_id, period_id])

        submissions = []
        for row in rows:
            record = dict(row)
            if record.get("first_submitted"):
                record["first_submitted"] = record["first_submitted"].isoformat()
            if record.get("last_submitted"):
                record["last_submitted"] = record["last_submitted"].isoformat()
            submissions.append(record)

        return {
            "status": "success",
            "period_id": period_id,
            "org_id": org_id,
            "submissions_count": len(submissions),
            "submissions": submissions,
        }
    except Exception as e:
        logger.error("get_report_status failed: %s", e)
        return {"status": "error", "message": str(e), "submissions": []}
