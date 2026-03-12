# ---------------------------------------------------------------------------
# MS-ATM-PDF – PDF/OCR Atomizer
# Python 3.12 slim + Tesseract OCR + language packs
# ---------------------------------------------------------------------------
FROM python:3.12-slim AS base

# System dependencies: Tesseract OCR with Czech, English, German language packs
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-ces \
    tesseract-ocr-deu \
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

EXPOSE 50051

ENV GRPC_PORT=50051 \
    PYTHONUNBUFFERED=1

ENTRYPOINT ["python", "-m", "src.main"]
