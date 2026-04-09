# ---------------------------------------------------------------------------
# Processor Atomizers – Unified PPTX/Excel/PDF/CSV/AI/Cleanup service
# Python 3.12 slim + LibreOffice headless + Tesseract OCR
# Build context: repository root
# ---------------------------------------------------------------------------
FROM python:3.12-slim AS base

# System dependencies:
#  - LibreOffice headless for PPTX slide rendering
#  - Tesseract OCR with Czech, English, German language packs
#  - libgl1 for PDF image processing
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libreoffice-impress \
        libreoffice-common \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-ces \
        tesseract-ocr-deu \
        fonts-liberation \
        fonts-dejavu-core \
        libgl1 \
        libglib2.0-0 \
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
COPY apps/processor/processor-atomizers/pyproject.toml ./
RUN pip install --no-cache-dir .

# ---------------------------------------------------------------------------
# Compile proto files into Python gRPC stubs
# ---------------------------------------------------------------------------
COPY packages/protos/ /tmp/protos/

RUN mkdir -p /app/generated/atomizer/v1 /app/generated/common/v1 && \
    # Compile common protos (dependency)
    python -m grpc_tools.protoc \
        -I/tmp/protos \
        --python_out=/app/generated \
        --grpc_python_out=/app/generated \
        /tmp/protos/common/v1/common.proto && \
    # Compile all atomizer protos
    python -m grpc_tools.protoc \
        -I/tmp/protos \
        --python_out=/app/generated \
        --grpc_python_out=/app/generated \
        /tmp/protos/atomizer/v1/ai.proto \
        /tmp/protos/atomizer/v1/csv.proto \
        /tmp/protos/atomizer/v1/excel.proto \
        /tmp/protos/atomizer/v1/pdf.proto \
        /tmp/protos/atomizer/v1/pptx.proto && \
    # Create __init__.py files for proper Python package structure
    touch /app/generated/__init__.py \
          /app/generated/atomizer/__init__.py \
          /app/generated/atomizer/v1/__init__.py \
          /app/generated/common/__init__.py \
          /app/generated/common/v1/__init__.py && \
    # Clean up proto source files
    rm -rf /tmp/protos

# Copy application source
COPY apps/processor/processor-atomizers/src/ ./src/

# Own everything by appuser
RUN chown -R appuser:appuser /app

USER appuser

# HTTP (FastAPI) and gRPC ports
EXPOSE 8088 50200

ENV HTTP_PORT=8088 \
    GRPC_PORT=50200 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app/generated

CMD ["python", "-m", "src.main"]
