"""gRPC server setup for processor-generators.

Registers PptxGeneratorService and ExcelGeneratorService on a single gRPC server.
"""

from __future__ import annotations

import logging

from python_base import GrpcServer

# Generated proto stubs
from generator.v1 import pptx_generator_pb2_grpc  # type: ignore[import-untyped]
from generator.v1 import excel_generator_pb2_grpc  # type: ignore[import-untyped]

from src.common.config import GRPC_PORT, SERVICE_NAME
from src.generators.pptx.service.generator_service import PptxGeneratorServiceImpl
from src.generators.xls.service.generator_service import ExcelGeneratorServiceImpl

logger = logging.getLogger(__name__)


async def create_and_start_grpc_server() -> GrpcServer:
    """Create, configure, and start the gRPC server with both generator services.

    Returns:
        The running GrpcServer instance.
    """
    logger.info("Starting gRPC server on port %d for %s", GRPC_PORT, SERVICE_NAME)

    grpc_server = GrpcServer(port=GRPC_PORT, service_name=SERVICE_NAME)

    # Register PPTX generator servicer
    pptx_servicer = PptxGeneratorServiceImpl()
    pptx_generator_pb2_grpc.add_PptxGeneratorServiceServicer_to_server(
        pptx_servicer, grpc_server.server,
    )
    logger.info("Registered PptxGeneratorService")

    # Register Excel generator servicer
    excel_servicer = ExcelGeneratorServiceImpl()
    excel_generator_pb2_grpc.add_ExcelGeneratorServiceServicer_to_server(
        excel_servicer, grpc_server.server,
    )
    logger.info("Registered ExcelGeneratorService")

    await grpc_server.start()
    return grpc_server
