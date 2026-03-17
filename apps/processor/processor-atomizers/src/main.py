"""Processor Atomizers – single FastAPI + gRPC entry point.

Starts both an HTTP server (FastAPI with health endpoints) and a gRPC server
(all atomizer services on a single port). The cleanup scheduler runs as an
APScheduler background job within the same process.
"""

from __future__ import annotations

import asyncio
import logging
from contextlib import asynccontextmanager
from typing import AsyncIterator

import uvicorn
from fastapi import FastAPI

from src.common.config import Settings
from src.common.grpc_server import create_grpc_server
from src.common.api_router import api_router, init_settings as init_api_settings
from src.common.health import health_router
from src.common.logging_config import setup_logging
from src.common.tracing import setup_tracing

settings = Settings()

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    """Manage startup and shutdown of gRPC server and cleanup scheduler."""
    # Start gRPC server
    grpc_server = await create_grpc_server(settings)
    await grpc_server.start()
    logger.info("gRPC server started on port %d", settings.grpc_port)

    # Start cleanup scheduler
    from src.atomizers.cleanup.scheduler import start_cleanup_scheduler

    scheduler = start_cleanup_scheduler(settings)
    logger.info("Cleanup scheduler started (interval: %d hours)", settings.cleanup_interval_hours)

    yield

    # Shutdown
    logger.info("Shutting down...")
    scheduler.shutdown(wait=False)
    await grpc_server.stop(grace=5)
    logger.info("Shutdown complete")


app = FastAPI(
    title="Processor Atomizers",
    description="Unified service for PPTX, Excel, PDF, CSV, AI atomization and cleanup.",
    version="0.1.0",
    lifespan=lifespan,
)
app.include_router(health_router)
init_api_settings(settings)
app.include_router(api_router)


if __name__ == "__main__":
    setup_logging(settings)
    setup_tracing(settings)
    uvicorn.run(
        "src.main:app",
        host="0.0.0.0",
        port=settings.http_port,
        log_level=settings.log_level.lower(),
    )
