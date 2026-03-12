"""Entry point for running python_base as a module (used in Dockerfile CMD)."""

from __future__ import annotations

import asyncio
import logging

from python_base.grpc_server import GrpcServer
from python_base.logging_config import setup_logging

logger = logging.getLogger(__name__)


async def main() -> None:
    """Start a bare gRPC server (health check only). Override in child services."""
    setup_logging(service_name="python-base")
    logger.info("Starting python-base gRPC server (health check only)")

    server = GrpcServer(service_name="python-base")
    await server.start()
    await server.wait_for_termination()


if __name__ == "__main__":
    asyncio.run(main())
