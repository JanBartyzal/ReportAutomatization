# ---------------------------------------------------------------------------
# MS-GEN-PPTX – PPTX Report Generator
# Python 3.12 slim + LibreOffice headless (validation) + matplotlib fonts
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
    PYTHONUNBUFFERED=1 \
    MPLBACKEND=Agg

ENTRYPOINT ["python", "-m", "src.main"]
