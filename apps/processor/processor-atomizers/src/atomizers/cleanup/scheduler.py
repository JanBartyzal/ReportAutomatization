"""APScheduler-based cleanup scheduler.

Replaces the standalone CronJob with an in-process scheduled task
that runs within the unified processor-atomizers service.
"""

from __future__ import annotations

import asyncio
import logging
from datetime import datetime, timezone
from typing import TYPE_CHECKING

from apscheduler.schedulers.background import BackgroundScheduler

from src.atomizers.cleanup.cleanup_worker import CleanupWorker

if TYPE_CHECKING:
    from src.common.config import Settings

logger = logging.getLogger(__name__)


def _run_cleanup_sync(settings: "Settings") -> None:
    """Synchronous wrapper to run the async cleanup worker.

    APScheduler's BackgroundScheduler runs jobs in a thread pool,
    so we create a new event loop for the async cleanup.
    """
    worker = CleanupWorker(settings)

    try:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            result = loop.run_until_complete(worker.run())
            logger.info(
                "Cleanup job completed: deleted %d files, freed %.2f MB, errors: %d",
                result.files_deleted,
                result.bytes_freed / (1024 * 1024),
                len(result.errors),
            )
        finally:
            loop.close()
    except Exception:
        logger.error("Cleanup job failed", exc_info=True)


def start_cleanup_scheduler(settings: "Settings") -> BackgroundScheduler:
    """Start an APScheduler BackgroundScheduler for periodic cleanup.

    The cleanup runs on a fixed interval (configurable via
    ``CLEANUP_INTERVAL_HOURS`` env var, default 6 hours).

    Args:
        settings: Application settings.

    Returns:
        The started BackgroundScheduler instance.
    """
    scheduler = BackgroundScheduler()

    scheduler.add_job(
        _run_cleanup_sync,
        trigger="interval",
        hours=settings.cleanup_interval_hours,
        args=[settings],
        id="cleanup_worker",
        name="Blob Storage Cleanup Worker",
        replace_existing=True,
        next_run_time=None,  # Do not run immediately on startup
    )

    scheduler.start()
    logger.info(
        "Cleanup scheduler started: interval=%d hours, dry_run=%s",
        settings.cleanup_interval_hours,
        settings.cleanup_dry_run,
    )

    return scheduler
