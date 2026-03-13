"""gRPC error handling utilities for atomizer services.

This module provides error handling decorators and utilities for gRPC services
to return proper 422 Unprocessable Entity status codes for validation errors.
"""

from __future__ import annotations

import functools
import logging
from typing import Any, Callable, TypeVar

import grpc

logger = logging.getLogger(__name__)

T = TypeVar("T")


def handle_grpc_errors(func: Callable[..., T]) -> Callable[..., T]:
    """Decorator to handle gRPC errors and return proper status codes.

    This decorator catches common exceptions and translates them into appropriate
    gRPC status codes:
    - ValueError -> INVALID_ARGUMENT (3)
    - RuntimeError -> INTERNAL (13)
    - TimeoutError -> DEADLINE_EXCEEDED (4)
    - FileNotFoundError, IOError -> NOT_FOUND (5)

    Args:
        func: The gRPC handler function to wrap.

    Returns:
        Wrapped function with error handling.
    """

    @functools.wraps(func)
    def wrapper(*args: Any, **kwargs: Any) -> T:
        try:
            return func(*args, **kwargs)
        except ValueError as e:
            logger.warning(f"Invalid argument: {e}")
            raise grpc.RpcError(
                grpc.StatusCode.INVALID_ARGUMENT,
                f"Invalid argument: {str(e)}",
            )
        except RuntimeError as e:
            logger.error(f"Internal error: {e}")
            raise grpc.RpcError(
                grpc.StatusCode.INTERNAL,
                f"Internal error: {str(e)}",
            )
        except TimeoutError as e:
            logger.warning(f"Operation timed out: {e}")
            raise grpc.RpcError(
                grpc.StatusCode.DEADLINE_EXCEEDED,
                f"Operation timed out: {str(e)}",
            )
        except FileNotFoundError as e:
            logger.warning(f"Resource not found: {e}")
            raise grpc.RpcError(
                grpc.StatusCode.NOT_FOUND,
                f"Resource not found: {str(e)}",
            )
        except IOError as e:
            logger.warning(f"I/O error: {e}")
            raise grpc.RpcError(
                grpc.StatusCode.NOT_FOUND,
                f"Resource not found: {str(e)}",
            )
        except PermissionError as e:
            logger.warning(f"Permission denied: {e}")
            raise grpc.RpcError(
                grpc.StatusCode.PERMISSION_DENIED,
                f"Permission denied: {str(e)}",
            )
        except Exception as e:
            logger.exception(f"Unexpected error: {e}")
            raise grpc.RpcError(
                grpc.StatusCode.INTERNAL,
                f"Unexpected error: {str(e)}",
            )

    return wrapper


class GrpcErrorHandler:
    """Context manager for handling gRPC errors with proper status codes."""

    def __init__(self, service_name: str):
        """Initialize the error handler.

        Args:
            service_name: Name of the service for logging purposes.
        """
        self.service_name = service_name

    def __enter__(self) -> "GrpcErrorHandler":
        return self

    def __exit__(self, exc_type: Any, exc_val: Any, exc_tb: Any) -> bool:
        """Handle exceptions and convert to gRPC status codes.

        Returns:
            True if the exception was handled, False otherwise.
        """
        if exc_type is None:
            return True

        if exc_type is ValueError:
            logger.warning(f"{self.service_name}: Invalid argument: {exc_val}")
            raise grpc.RpcError(
                grpc.StatusCode.INVALID_ARGUMENT,
                f"Invalid argument: {str(exc_val)}",
            )
        elif exc_type is RuntimeError:
            logger.error(f"{self.service_name}: Internal error: {exc_val}")
            raise grpc.RpcError(
                grpc.StatusCode.INTERNAL,
                f"Internal error: {str(exc_val)}",
            )
        elif exc_type is TimeoutError:
            logger.warning(f"{self.service_name}: Timeout: {exc_val}")
            raise grpc.RpcError(
                grpc.StatusCode.DEADLINE_EXCEEDED,
                f"Operation timed out: {str(exc_val)}",
            )
        elif exc_type in (FileNotFoundError, IOError):
            logger.warning(f"{self.service_name}: Not found: {exc_val}")
            raise grpc.RpcError(
                grpc.StatusCode.NOT_FOUND,
                f"Resource not found: {str(exc_val)}",
            )
        else:
            logger.exception(f"{self.service_name}: Unexpected error: {exc_val}")
            raise grpc.RpcError(
                grpc.StatusCode.INTERNAL,
                f"Unexpected error: {str(exc_val)}",
            )


def create_rpc_error(code: grpc.StatusCode, message: str) -> grpc.RpcError:
    """Create a gRPC RpcError with the given status code and message.

    Args:
        code: The gRPC status code.
        message: The error message.

    Returns:
        An RpcError instance.
    """
    return grpc.RpcError(code, message)


def raise_invalid_argument(message: str) -> None:
    """Raise a gRPC INVALID_ARGUMENT error.

    Args:
        message: The error message.

    Raises:
        grpc.RpcError: With INVALID_ARGUMENT status code.
    """
    raise create_rpc_error(grpc.StatusCode.INVALID_ARGUMENT, message)


def raise_not_found(message: str) -> None:
    """Raise a gRPC NOT_FOUND error.

    Args:
        message: The error message.

    Raises:
        grpc.RpcError: With NOT_FOUND status code.
    """
    raise create_rpc_error(grpc.StatusCode.NOT_FOUND, message)


def raise_internal_error(message: str) -> None:
    """Raise a gRPC INTERNAL error.

    Args:
        message: The error message.

    Raises:
        grpc.RpcError: With INTERNAL status code.
    """
    raise create_rpc_error(grpc.StatusCode.INTERNAL, message)


def raise_unavailable(message: str) -> None:
    """Raise a gRPC UNAVAILABLE error.

    Args:
        message: The error message.

    Raises:
        grpc.RpcError: With UNAVAILABLE status code.
    """
    raise create_rpc_error(grpc.StatusCode.UNAVAILABLE, message)
