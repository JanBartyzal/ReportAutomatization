"""Per-organization rate limiter using asyncio semaphores."""

from __future__ import annotations

import asyncio
import logging
from contextlib import asynccontextmanager
from typing import AsyncIterator

logger = logging.getLogger(__name__)


class RateLimiter:
    """Manages per-org concurrency via asyncio.Semaphore instances."""

    def __init__(self, max_concurrent: int = 5) -> None:
        self._max = max_concurrent
        self._semaphores: dict[str, asyncio.Semaphore] = {}

    def _get_semaphore(self, org_id: str) -> asyncio.Semaphore:
        """Get or create a semaphore for the given org."""
        if org_id not in self._semaphores:
            self._semaphores[org_id] = asyncio.Semaphore(self._max)
        return self._semaphores[org_id]

    @asynccontextmanager
    async def acquire(self, org_id: str) -> AsyncIterator[None]:
        """Context manager that acquires and releases the org semaphore."""
        sem = self._get_semaphore(org_id)
        logger.debug("Acquiring rate limit slot for org=%s", org_id)
        async with sem:
            yield
