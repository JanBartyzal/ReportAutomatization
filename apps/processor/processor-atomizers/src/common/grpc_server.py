"""Shared gRPC server setup that registers all atomizer services."""

from __future__ import annotations

import logging
from typing import TYPE_CHECKING

import grpc
from grpc_health.v1 import health, health_pb2, health_pb2_grpc

# Generated proto stubs (placeholder import paths)
from atomizer.v1 import (  # type: ignore[import-untyped]
    ai_pb2_grpc,
    csv_pb2_grpc,
    excel_pb2_grpc,
    pdf_pb2_grpc,
    pptx_pb2_grpc,
)

from src.atomizers.ai.service.ai_gateway_grpc import AiGatewayGrpcService
from src.atomizers.csv.service.csv_service import CsvAtomizerService
from src.atomizers.pdf.service.pdf_service import PdfAtomizerService
from src.atomizers.pptx.service.pptx_service import PptxAtomizerService
from src.atomizers.xls.service.excel_service import ExcelAtomizerService

if TYPE_CHECKING:
    from src.common.config import Settings

logger = logging.getLogger(__name__)


async def create_grpc_server(settings: "Settings") -> grpc.aio.Server:
    """Create and configure a gRPC server with all atomizer services registered.

    Args:
        settings: Application settings.

    Returns:
        A configured (but not yet started) gRPC async server.
    """
    server = grpc.aio.server()
    server.add_insecure_port(f"[::]:{settings.grpc_port}")

    # -- Register atomizer servicers --

    # PPTX atomizer
    pptx_servicer = PptxAtomizerService(settings)
    pptx_pb2_grpc.add_PptxAtomizerServiceServicer_to_server(pptx_servicer, server)
    logger.info("Registered PptxAtomizerService")

    # Excel atomizer
    excel_servicer = ExcelAtomizerService(settings)
    excel_pb2_grpc.add_ExcelAtomizerServiceServicer_to_server(excel_servicer, server)
    logger.info("Registered ExcelAtomizerService")

    # PDF atomizer
    pdf_servicer = PdfAtomizerService(settings)
    pdf_pb2_grpc.add_PdfAtomizerServiceServicer_to_server(pdf_servicer, server)
    logger.info("Registered PdfAtomizerService")

    # CSV atomizer
    csv_servicer = CsvAtomizerService(settings)
    csv_pb2_grpc.add_CsvAtomizerServiceServicer_to_server(csv_servicer, server)
    logger.info("Registered CsvAtomizerService")

    # AI Gateway
    ai_servicer = AiGatewayGrpcService(settings)
    ai_pb2_grpc.add_AiGatewayServiceServicer_to_server(ai_servicer, server)
    logger.info("Registered AiGatewayGrpcService")

    # -- Health check --
    health_servicer = health.HealthServicer()
    health_pb2_grpc.add_HealthServicer_to_server(health_servicer, server)
    health_servicer.set(
        "",
        health_pb2.HealthCheckResponse.SERVING,
    )
    logger.info("Registered gRPC health check")

    return server
