"""MS-ATM-PDF entry point.

Starts the async gRPC server on the configured port, registers the
``PdfAtomizerService`` servicer and health check, and waits for
termination signals.
"""

from __future__ import annotations

import asyncio
import logging

from python_base import GrpcServer, setup_logging

# Generated proto stubs (placeholder import path)
from atomizer.v1 import pdf_pb2_grpc  # type: ignore[import-untyped]

from src.config import GRPC_PORT, SERVICE_NAME
from src.service.pdf_service import PdfAtomizerService

logger = logging.getLogger(__name__)


async def _serve() -> None:
    """Create, configure, and run the gRPC server."""
    setup_logging()

    logger.info("Starting %s on port %d", SERVICE_NAME, GRPC_PORT)

    grpc_server = GrpcServer(port=GRPC_PORT, service_name=SERVICE_NAME)

    # Register the PDF atomizer servicer
    servicer = PdfAtomizerService()
    pdf_pb2_grpc.add_PdfAtomizerServiceServicer_to_server(servicer, grpc_server.server)

    await grpc_server.start()
    await grpc_server.wait_for_termination()


def main() -> None:
    """Synchronous wrapper to launch the async server."""
    asyncio.run(_serve())


if __name__ == "__main__":
    main()
