# ---------------------------------------------------------------------------
# MS-ATM-CLN – Cleanup Worker CronJob
# Python 3.12 slim
# ---------------------------------------------------------------------------
FROM python:3.12-slim AS base

# Non-root user
RUN groupadd --gid 1000 appuser && \
    useradd --uid 1000 --gid appuser --shell /bin/bash --create-home appuser

WORKDIR /app

# Install Python dependencies
COPY pyproject.toml ./
RUN pip install --no-cache-dir .

# Copy application source
COPY src/ ./src/

# Own everything by appuser
RUN chown -R appuser:appuser /app

USER appuser

ENV PYTHONUNBUFFERED=1

# Run as cron job - entrypoint will be the cleanup script
ENTRYPOINT ["python", "-m", "src.main"]
