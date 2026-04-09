# ---------------------------------------------------------------------------
# PROCESSOR-GENERATORS – Consolidated PPTX + Excel + MCP service
# Python 3.12 slim + system fonts for chart rendering
# Build context: repository root
# ---------------------------------------------------------------------------
FROM python:3.12-slim AS base

# System dependencies: LibreOffice for PPTX validation, fonts for matplotlib
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libreoffice-impress \
    libreoffice-common \
    fonts-liberation \
    fonts-dejavu-core \
    libfontconfig1 \
    libpq-dev \
    && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Non-root user
RUN groupadd --gid 1000 appuser && \
    useradd --uid 1000 --gid appuser --shell /bin/bash --create-home appuser

WORKDIR /app

# Install shared python-base package first
COPY packages/python-base /tmp/python-base
RUN pip install --no-cache-dir /tmp/python-base && rm -rf /tmp/python-base

# Install application dependencies
COPY apps/processor/processor-generators/pyproject.toml ./
RUN pip install --no-cache-dir .

# ---------------------------------------------------------------------------
# Compile proto files into Python gRPC stubs
# ---------------------------------------------------------------------------
COPY packages/protos/ /tmp/protos/
COPY packages/java-base/java-base-protos/src/main/proto/ /tmp/java-protos/

RUN mkdir -p /app/generated/generator/v1 /app/generated/common/v1 && \
    # Compile common protos (dependency of generator protos)
    python -m grpc_tools.protoc \
        -I/tmp/protos \
        -I/tmp/java-protos \
        --python_out=/app/generated \
        --grpc_python_out=/app/generated \
        /tmp/protos/common/v1/common.proto && \
    # Compile the unified generator.proto (has both PptxGenerator and ExcelGenerator services)
    python -m grpc_tools.protoc \
        -I/tmp/protos \
        -I/tmp/java-protos \
        --python_out=/app/generated \
        --grpc_python_out=/app/generated \
        /tmp/java-protos/generator/v1/generator.proto && \
    # Create __init__.py files for proper Python package structure
    touch /app/generated/__init__.py \
          /app/generated/generator/__init__.py \
          /app/generated/generator/v1/__init__.py \
          /app/generated/common/__init__.py \
          /app/generated/common/v1/__init__.py && \
    # Clean up proto source files
    rm -rf /tmp/protos /tmp/java-protos

# Copy application source
COPY apps/processor/processor-generators/src/ ./src/

# Own everything by appuser
RUN chown -R appuser:appuser /app

USER appuser

# REST (MCP + health) and gRPC
EXPOSE 8111 50201

ENV GRPC_PORT=50201 \
    HTTP_PORT=8111 \
    SERVICE_NAME=processor-generators \
    DAPR_APP_ID=processor-generators \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app/generated \
    MPLBACKEND=Agg

ENTRYPOINT ["python", "-m", "src.main"]
