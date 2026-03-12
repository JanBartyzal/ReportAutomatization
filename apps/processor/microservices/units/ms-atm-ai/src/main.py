"""MS-ATM-AI entry point.

Starts the async gRPC server on the configured port, registers the
``AiGatewayService`` servicer and health check, and waits for
termination signals.
"""

from __future__ import annotations

import asyncio
import logging

from python_base import GrpcServer, setup_logging

from atomizer.v1 import ai_pb2_grpc  # type: ignore[import-untyped]

from src.config import GRPC_PORT, SERVICE_NAME
from src.service.ai_gateway_grpc import AiGatewayGrpcService

logger = logging.getLogger(__name__)


async def _serve() -> None:
    """Create, configure, and run the gRPC server."""
    setup_logging(SERVICE_NAME)

    logger.info("Starting %s on port %d", SERVICE_NAME, GRPC_PORT)

    grpc_server = GrpcServer(port=GRPC_PORT, service_name=SERVICE_NAME)

    servicer = AiGatewayGrpcService()
    ai_pb2_grpc.add_AiGatewayServiceServicer_to_server(servicer, grpc_server.server)

    await grpc_server.start()
    await grpc_server.wait_for_termination()


def main() -> None:
    """Synchronous wrapper to launch the async server."""
    asyncio.run(_serve())


if __name__ == "__main__":
    main()
