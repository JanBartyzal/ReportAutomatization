"""Health check and metrics endpoints for FastAPI."""

from __future__ import annotations

from fastapi import APIRouter
from fastapi.responses import Response
from prometheus_client import generate_latest, CONTENT_TYPE_LATEST

health_router = APIRouter(tags=["health"])


@health_router.get("/health")
async def health_check() -> dict[str, str]:
    """Liveness probe – returns 200 if the process is running."""
    return {"status": "healthy"}


@health_router.get("/ready")
async def readiness_check() -> dict[str, str]:
    """Readiness probe – returns 200 when the service is ready to accept traffic."""
    return {"status": "ready"}


@health_router.get("/metrics")
async def metrics() -> Response:
    """Prometheus metrics endpoint."""
    return Response(content=generate_latest(), media_type=CONTENT_TYPE_LATEST)
