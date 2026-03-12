# Python base image for ReportAutomatization microservices
# Usage: extend this image in individual service Dockerfiles
#   FROM python-base:latest
#   COPY ./my_service /app/my_service
#   CMD ["python", "-m", "my_service"]

FROM python:3.12-slim AS base

# Prevent Python from writing .pyc files and enable unbuffered stdout/stderr
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Install system dependencies required by grpcio and other native extensions
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        build-essential \
        curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd --gid 1001 appuser && \
    useradd --uid 1001 --gid appuser --shell /bin/bash --create-home appuser

WORKDIR /app

# Copy and install dependencies first (layer caching)
COPY pyproject.toml ./
RUN pip install --no-cache-dir --upgrade pip setuptools wheel && \
    pip install --no-cache-dir .

# Copy the shared library source
COPY python_base/ ./python_base/

# Remove build-essential after compilation to reduce image size
RUN apt-get purge -y build-essential && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

# Switch to non-root user
USER appuser

# Default gRPC port
EXPOSE 50051

# Health check using grpc_health_probe or curl
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import grpc; ch = grpc.insecure_channel('localhost:50051'); grpc.channel_ready_future(ch).result(timeout=3)" || exit 1

# Default command (override in child Dockerfiles)
CMD ["python", "-m", "python_base"]
