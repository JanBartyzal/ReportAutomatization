"""Health check endpoints for FastAPI."""

from __future__ import annotations

from fastapi import APIRouter

health_router = APIRouter(tags=["health"])


@health_router.get("/health")
async def health_check() -> dict[str, str]:
    """Liveness probe – returns 200 if the process is running."""
    return {"status": "healthy"}


@health_router.get("/ready")
async def readiness_check() -> dict[str, str]:
    """Readiness probe – returns 200 when the service is ready to accept traffic."""
    return {"status": "ready"}
