"""Base async gRPC server with health checking and OpenTelemetry instrumentation."""

from __future__ import annotations

import asyncio
import logging
import signal
from typing import Any

import grpc
from grpc_health.v1 import health, health_pb2, health_pb2_grpc
from opentelemetry.instrumentation.grpc import aio_server_interceptor

logger = logging.getLogger(__name__)


class GrpcServer:
    """Async gRPC server with built-in health checks and graceful shutdown.

    Usage::

        server = GrpcServer(port=50051, service_name="ms-atm-pptx")
        my_pb2_grpc.add_MyServiceServicer_to_server(MyServicer(), server.server)
        await server.start()
        await server.wait_for_termination()

    Attributes:
        port: The port the gRPC server listens on.
        service_name: Identifier used for health check registration and logging.
    """

    def __init__(
        self,
        port: int = 50051,
        service_name: str = "python-service",
        max_workers: int = 10,
        reflection: bool = False,
        metrics_port: int = 9090,
    ) -> None:
        """Initialize the gRPC server.

        Args:
            port: Port to listen on (default 50051).
            service_name: Name used for health check registration and logging.
            max_workers: Maximum number of concurrent RPC handlers.
            reflection: Whether to enable gRPC server reflection (useful for dev).
            metrics_port: Port for Prometheus metrics HTTP endpoint (default 9090, 0 to disable).
        """
        self.port = port
        self.service_name = service_name
        self._max_workers = max_workers
        self._reflection = reflection
        self._metrics_port = metrics_port
        self._shutdown_event = asyncio.Event()

        # Initialize OpenTelemetry tracing before creating the interceptor
        from python_base.tracing import setup_tracing
        setup_tracing(service_name=service_name)

        # Create the OpenTelemetry interceptor for distributed tracing
        otel_interceptor = aio_server_interceptor()

        self.server: grpc.aio.Server = grpc.aio.server(
            interceptors=[otel_interceptor],
            maximum_concurrent_rpcs=max_workers,
        )

        # Health check service
        self._health_servicer = health.HealthServicer()
        health_pb2_grpc.add_HealthServicer_to_server(self._health_servicer, self.server)

    async def start(self) -> None:
        """Start the gRPC server and register signal handlers for graceful shutdown."""
        listen_addr = f"[::]:{self.port}"
        self.server.add_insecure_port(listen_addr)

        # Mark service as SERVING in health check
        await self._health_servicer.set(
            self.service_name,
            health_pb2.HealthCheckResponse.SERVING,
        )
        # Also set the overall server health
        await self._health_servicer.set(
            "",
            health_pb2.HealthCheckResponse.SERVING,
        )

        # Start Prometheus metrics HTTP server
        if self._metrics_port > 0:
            from python_base.metrics import init_service_info, start_metrics_server
            init_service_info(self.service_name)
            start_metrics_server(self._metrics_port)

        await self.server.start()
        logger.info(
            "gRPC server '%s' started on %s (metrics on :%d)",
            self.service_name,
            listen_addr,
            self._metrics_port,
        )

        # Register signal handlers for graceful shutdown
        loop = asyncio.get_running_loop()
        for sig in (signal.SIGINT, signal.SIGTERM):
            try:
                loop.add_signal_handler(sig, self._signal_handler)
            except NotImplementedError:
                # Signal handlers are not supported on Windows event loops;
                # shutdown must be triggered manually or via KeyboardInterrupt.
                pass

    def _signal_handler(self) -> None:
        """Handle OS signals by initiating graceful shutdown."""
        logger.info("Received shutdown signal for '%s'", self.service_name)
        self._shutdown_event.set()

    async def stop(self, grace_period: float = 5.0) -> None:
        """Gracefully stop the gRPC server.

        Args:
            grace_period: Seconds to wait for in-flight RPCs to complete.
        """
        logger.info(
            "Shutting down gRPC server '%s' (grace=%.1fs)...",
            self.service_name,
            grace_period,
        )

        # Mark as NOT_SERVING before draining
        await self._health_servicer.set(
            self.service_name,
            health_pb2.HealthCheckResponse.NOT_SERVING,
        )
        await self._health_servicer.set(
            "",
            health_pb2.HealthCheckResponse.NOT_SERVING,
        )

        await self.server.stop(grace=grace_period)
        logger.info("gRPC server '%s' stopped.", self.service_name)

    async def wait_for_termination(self) -> None:
        """Block until a shutdown signal is received, then stop gracefully."""
        try:
            await self._shutdown_event.wait()
        except asyncio.CancelledError:
            pass
        finally:
            await self.stop()


async def serve(
    service_name: str,
    port: int = 50051,
    register_services: Any = None,
) -> None:
    """Convenience function to create, configure, and run a gRPC server.

    Args:
        service_name: Name of the microservice.
        port: Port to listen on.
        register_services: Optional callable(server) to register gRPC servicers.
    """
    grpc_server = GrpcServer(port=port, service_name=service_name)

    if register_services is not None:
        register_services(grpc_server.server)

    await grpc_server.start()
    await grpc_server.wait_for_termination()
