"""Health check endpoints for processor-generators."""

from __future__ import annotations

from fastapi import APIRouter
from fastapi.responses import JSONResponse

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
