"""MS-ATM-CLN entry point.

Runs the cleanup worker on a cron schedule to remove temporary files
from blob storage.
"""

from __future__ import annotations

import asyncio
import logging
import signal
import time
from datetime import datetime, timezone

from croniter import croniter

from src.cleanup_worker import CleanupWorker
from src.config import CRON_SCHEDULE, SERVICE_NAME

logger = logging.getLogger(__name__)


class CleanupScheduler:
    """Scheduler that runs cleanup tasks based on cron expression."""

    def __init__(self, schedule: str) -> None:
        """Initialize the scheduler.

        Args:
            schedule: Cron expression (e.g., "0 * * * *" for hourly).
        """
        self._schedule = schedule
        self._worker = CleanupWorker()
        self._running = False

    async def start(self) -> None:
        """Start the cleanup scheduler."""
        self._running = True
        logger.info("Starting cleanup scheduler with cron: %s", self._schedule)

        # Register signal handlers
        loop = asyncio.get_running_loop()
        for sig in (signal.SIGINT, signal.SIGTERM):
            try:
                loop.add_signal_handler(sig, self._shutdown)
            except NotImplementedError:
                pass

        while self._running:
            await self._run_once()
            # Sleep until next cron time
            await self._sleep_until_next()

    def _shutdown(self) -> None:
        """Handle shutdown signal."""
        logger.info("Shutdown signal received")
        self._running = False

    async def _run_once(self) -> None:
        """Run the cleanup worker once."""
        try:
            logger.info("Starting cleanup job at %s", datetime.now(timezone.utc))
            result = await self._worker.run()
            logger.info(
                "Cleanup job completed: deleted %d files, freed %.2f MB",
                result.files_deleted,
                result.bytes_freed / (1024 * 1024),
            )
        except Exception as e:
            logger.error("Cleanup job failed: %s", e, exc_info=True)

    async def _sleep_until_next(self) -> None:
        """Sleep until the next cron schedule time."""
        now = datetime.now(timezone.utc)
        cron = croniter(self._schedule, now)
        next_run = cron.get_next(datetime)

        delay = (next_run - now).total_seconds()
        logger.debug("Next cleanup run at %s (in %.1f seconds)", next_run, delay)

        if delay > 0:
            try:
                await asyncio.sleep(delay)
            except asyncio.CancelledError:
                pass


async def _run_continuous() -> None:
    """Run the scheduler continuously."""
    scheduler = CleanupScheduler(CRON_SCHEDULE)
    await scheduler.start()


def main() -> None:
    """Main entry point."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )

    logger.info("Starting %s", SERVICE_NAME)
    asyncio.run(_run_continuous())


if __name__ == "__main__":
    main()
