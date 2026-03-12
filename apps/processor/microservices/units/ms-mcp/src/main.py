"""MS-MCP entry point.

Starts the FastAPI application with MCP server endpoints and health check.
"""

from __future__ import annotations

import asyncio
import logging

import uvicorn
from fastapi import FastAPI
from fastapi.responses import JSONResponse

from python_base import setup_logging

from src.config import HTTP_PORT, SERVICE_NAME
from src.db.connection import DatabasePool
from src.mcp_server import create_mcp_app

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    """Create and configure the FastAPI application."""
    app = FastAPI(
        title="MS-MCP – MCP Server",
        description="AI agent interface for querying OPEX data and reports",
        version="0.1.0",
    )

    db_pool = DatabasePool()

    @app.on_event("startup")
    async def startup() -> None:
        await db_pool.connect()
        logger.info("Database pool connected")

    @app.on_event("shutdown")
    async def shutdown() -> None:
        await db_pool.close()
        logger.info("Database pool closed")

    @app.get("/health")
    async def health() -> JSONResponse:
        return JSONResponse({"status": "healthy", "service": SERVICE_NAME})

    # Mount the MCP server
    mcp_app = create_mcp_app(db_pool)
    app.mount("/mcp", mcp_app)

    return app


def main() -> None:
    """Start the application."""
    setup_logging(SERVICE_NAME)
    logger.info("Starting %s on port %d", SERVICE_NAME, HTTP_PORT)

    app = create_app()
    uvicorn.run(app, host="0.0.0.0", port=HTTP_PORT, log_level="info")


if __name__ == "__main__":
    main()
