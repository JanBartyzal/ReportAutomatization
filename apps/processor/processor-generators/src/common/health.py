"""Health check and metrics endpoints for processor-generators."""

from __future__ import annotations

from fastapi import APIRouter
from fastapi.responses import JSONResponse, Response
from prometheus_client import generate_latest, CONTENT_TYPE_LATEST

from src.common.config import SERVICE_NAME

router = APIRouter(tags=["health"])


@router.get("/health")
async def health() -> JSONResponse:
    """Liveness probe."""
    return JSONResponse({"status": "healthy", "service": SERVICE_NAME})


@router.get("/ready")
async def readiness() -> JSONResponse:
    """Readiness probe."""
    return JSONResponse({"status": "ready", "service": SERVICE_NAME})


@router.get("/metrics")
async def metrics() -> Response:
    """Prometheus metrics endpoint."""
    return Response(content=generate_latest(), media_type=CONTENT_TYPE_LATEST)
