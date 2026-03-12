"""Database connection pool with RLS context enforcement.

Every query is preceded by ``SELECT rls.set_org_context(org_id)``
to enforce row-level security scoping to the user's organization.
"""

from __future__ import annotations

import logging
from typing import Any

import asyncpg

from src.config import DB_HOST, DB_NAME, DB_PASSWORD, DB_PORT, DB_USER

logger = logging.getLogger(__name__)


class DatabasePool:
    """Manages an asyncpg connection pool with RLS enforcement."""

    def __init__(self) -> None:
        self._pool: asyncpg.Pool | None = None

    async def connect(self) -> None:
        """Initialize the connection pool."""
        dsn = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
        self._pool = await asyncpg.create_pool(dsn, min_size=2, max_size=10)
        logger.info("Database pool created: %s@%s:%d/%s", DB_USER, DB_HOST, DB_PORT, DB_NAME)

    async def close(self) -> None:
        """Close the connection pool."""
        if self._pool:
            await self._pool.close()
            self._pool = None

    async def execute_with_rls(
        self,
        org_id: str,
        query: str,
        params: list[Any] | None = None,
    ) -> list[dict[str, Any]]:
        """Execute a query with RLS context set to the given org_id.

        Sets the session org context before executing the query,
        ensuring row-level security policies are enforced.

        Args:
            org_id: Organization ID to scope the query to.
            query: SQL query to execute.
            params: Query parameters (positional $1, $2, etc.).

        Returns:
            List of result rows as dicts.
        """
        if not self._pool:
            raise RuntimeError("Database pool not connected")

        async with self._pool.acquire() as conn:
            async with conn.transaction():
                # Set RLS context for this transaction
                await conn.execute("SELECT rls.set_org_context($1::uuid)", org_id)

                # Execute the actual query
                if params:
                    rows = await conn.fetch(query, *params)
                else:
                    rows = await conn.fetch(query)

                return [dict(row) for row in rows]
