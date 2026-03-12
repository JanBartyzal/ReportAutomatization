# ---------------------------------------------------------------------------
# MS-ATM-AI – AI Gateway (LiteLLM integration)
# Python 3.12 slim
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

EXPOSE 50051

ENV GRPC_PORT=50051 \
    PYTHONUNBUFFERED=1

ENTRYPOINT ["python", "-m", "src.main"]
