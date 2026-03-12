# ---------------------------------------------------------------------------
# Processor Atomizers – Unified PPTX/Excel/PDF/CSV/AI/Cleanup service
# Python 3.12 slim + LibreOffice headless + Tesseract OCR
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
        libgl1-mesa-glx \
        libglib2.0-0 \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Non-root user
RUN groupadd --gid 1000 appuser && \
    useradd --uid 1000 --gid appuser --shell /bin/bash --create-home appuser

WORKDIR /app

# Install Python dependencies
COPY pyproject.toml ./
RUN pip install --no-cache-dir . && \
    pip install --no-cache-dir python-base

# Copy application source
COPY src/ ./src/

# Own everything by appuser
RUN chown -R appuser:appuser /app

USER appuser

# HTTP (FastAPI) and gRPC ports
EXPOSE 8088 50090

ENV HTTP_PORT=8088 \
    GRPC_PORT=50090 \
    PYTHONUNBUFFERED=1

CMD ["python", "-m", "src.main"]
