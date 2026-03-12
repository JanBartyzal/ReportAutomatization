"""Tests for RateLimiter -- per-org concurrency control."""

from __future__ import annotations

import asyncio

import pytest

from src.atomizers.ai.service.rate_limiter import RateLimiter


async def test_single_request_passes() -> None:
    limiter = RateLimiter(max_concurrent=2)

    async with limiter.acquire("org-1"):
        pass


async def test_concurrent_within_limit() -> None:
    limiter = RateLimiter(max_concurrent=3)
    results: list[int] = []

    async def task(idx: int) -> None:
        async with limiter.acquire("org-1"):
            results.append(idx)
            await asyncio.sleep(0.01)

    await asyncio.gather(task(1), task(2), task(3))
    assert sorted(results) == [1, 2, 3]


async def test_exceeding_limit_blocks() -> None:
    limiter = RateLimiter(max_concurrent=1)
    order: list[str] = []

    async def slow_task() -> None:
        async with limiter.acquire("org-1"):
            order.append("slow_start")
            await asyncio.sleep(0.05)
            order.append("slow_end")

    async def fast_task() -> None:
        await asyncio.sleep(0.01)
        async with limiter.acquire("org-1"):
            order.append("fast_start")

    await asyncio.gather(slow_task(), fast_task())

    assert order.index("fast_start") > order.index("slow_end")


async def test_different_orgs_independent() -> None:
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

    assert max_concurrent == 2
