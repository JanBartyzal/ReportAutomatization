# ---------------------------------------------------------------------------
# MS-MCP – MCP Server (AI Agent Interface)
# Python 3.12 slim – FastAPI HTTP server
# ---------------------------------------------------------------------------
FROM python:3.12-slim AS base

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

EXPOSE 8000

ENV HTTP_PORT=8000 \
    PYTHONUNBUFFERED=1

ENTRYPOINT ["python", "-m", "src.main"]
