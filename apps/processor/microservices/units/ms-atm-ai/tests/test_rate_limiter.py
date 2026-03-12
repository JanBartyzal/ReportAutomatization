"""Tests for RateLimiter – per-org concurrency control."""

from __future__ import annotations

import asyncio

import pytest

from src.service.rate_limiter import RateLimiter


async def test_single_request_passes() -> None:
    """A single request should acquire and release without blocking."""
    limiter = RateLimiter(max_concurrent=2)

    async with limiter.acquire("org-1"):
        pass  # Should not block


async def test_concurrent_within_limit() -> None:
    """Multiple requests within the limit should all proceed."""
    limiter = RateLimiter(max_concurrent=3)
    results: list[int] = []

    async def task(idx: int) -> None:
        async with limiter.acquire("org-1"):
            results.append(idx)
            await asyncio.sleep(0.01)

    await asyncio.gather(task(1), task(2), task(3))
    assert sorted(results) == [1, 2, 3]


async def test_exceeding_limit_blocks() -> None:
    """Requests beyond the limit should be queued until a slot frees up."""
    limiter = RateLimiter(max_concurrent=1)
    order: list[str] = []

    async def slow_task() -> None:
        async with limiter.acquire("org-1"):
            order.append("slow_start")
            await asyncio.sleep(0.05)
            order.append("slow_end")

    async def fast_task() -> None:
        await asyncio.sleep(0.01)  # Ensure slow_task acquires first
        async with limiter.acquire("org-1"):
            order.append("fast_start")

    await asyncio.gather(slow_task(), fast_task())

    # fast_task should only start after slow_task ends
    assert order.index("fast_start") > order.index("slow_end")


async def test_different_orgs_independent() -> None:
    """Different orgs should have independent semaphores."""
    limiter = RateLimiter(max_concurrent=1)
    concurrent_count = 0
    max_concurrent = 0

    async def task(org: str) -> None:
        nonlocal concurrent_count, max_concurrent
        async with limiter.acquire(org):
            concurrent_count += 1
            max_concurrent = max(max_concurrent, concurrent_count)
            await asyncio.sleep(0.02)
            concurrent_count -= 1

    await asyncio.gather(task("org-a"), task("org-b"))

    # Both orgs can run concurrently since they have separate semaphores
    assert max_concurrent == 2
