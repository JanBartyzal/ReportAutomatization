"""Processor-generators entry point.

Starts both:
- FastAPI app on HTTP_PORT (8111) for MCP REST endpoints and health checks
- gRPC server on GRPC_PORT (50201) for PptxGeneratorService and ExcelGeneratorService

Dapr app-id: processor-generators
"""

from __future__ import annotations

import asyncio
import logging

import uvicorn
from fastapi import FastAPI

from src.common.config import GRPC_PORT, HTTP_PORT, SERVICE_NAME
from src.common.grpc_server import create_and_start_grpc_server
from src.common.health import router as health_router
from src.common.logging_config import setup_logging
from src.common.tracing import setup_tracing
from src.generators.xls.router import router as xls_router
from src.mcp.db.connection import DatabasePool
from src.mcp.mcp_server import create_mcp_app

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    """Create and configure the FastAPI application.

    Includes:
    - Health check routes at /health and /ready
    - MCP server mounted at /mcp
    """
    app = FastAPI(
        title="Processor Generators – PPTX, Excel & MCP",
        description="Consolidated report generation and AI agent interface service",
        version="0.1.0",
    )

    db_pool = DatabasePool()

    @app.on_event("startup")
    async def startup() -> None:
        try:
            await db_pool.connect()
            logger.info("Database pool connected")
        except Exception as exc:
            logger.warning("Database pool connection failed (MCP tools will be unavailable): %s", exc)

    @app.on_event("shutdown")
    async def shutdown() -> None:
        await db_pool.close()
        logger.info("Database pool closed")

    # Health check routes
    app.include_router(health_router)

    # Excel HTTP endpoints (UpdateSheet, etc.)
    app.include_router(xls_router)

    # Mount the MCP server
    mcp_app = create_mcp_app(db_pool)
    app.mount("/mcp", mcp_app)

    return app


async def _run_grpc() -> None:
    """Start and run the gRPC server until termination."""
    grpc_server = await create_and_start_grpc_server()
    logger.info("gRPC server started on port %d", GRPC_PORT)
    await grpc_server.wait_for_termination()


async def _run_http(app: FastAPI) -> None:
    """Start and run the HTTP/FastAPI server."""
    config = uvicorn.Config(
        app,
        host="0.0.0.0",
        port=HTTP_PORT,
        log_level="info",
    )
    server = uvicorn.Server(config)
    await server.serve()


async def _serve() -> None:
    """Run both HTTP and gRPC servers concurrently."""
    setup_logging()
    setup_tracing()

    logger.info(
        "Starting %s: HTTP on port %d, gRPC on port %d",
        SERVICE_NAME,
        HTTP_PORT,
        GRPC_PORT,
    )

    app = create_app()

    # Run both servers concurrently
    await asyncio.gather(
        _run_http(app),
        _run_grpc(),
    )


def main() -> None:
    """Synchronous wrapper to launch the async servers."""
    asyncio.run(_serve())


if __name__ == "__main__":
    main()
