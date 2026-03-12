"""Cleanup Worker - removes temporary files from blob storage.

Cleanup rules:
- Delete temporary PNG slides from Blob > 24 hours old
- Delete temporary CSV exports > 24 hours old
- Delete `_raw/` original files > 90 days old
- Delete temporary generator output files > 24 hours old
"""

from __future__ import annotations

import asyncio
import logging
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import TYPE_CHECKING, AsyncIterator

from azure.storage.blob import BlobServiceClient, ContainerClient

if TYPE_CHECKING:
    from src.common.config import Settings

logger = logging.getLogger(__name__)


@dataclass
class CleanupResult:
    """Result of a cleanup operation."""

    files_deleted: int
    bytes_freed: int
    errors: list[str]


@dataclass
class CleanupRule:
    """A single cleanup rule."""

    container: str
    prefix: str
    max_age: timedelta
    description: str


class CleanupWorker:
    """Worker that cleans up temporary files from blob storage."""

    def __init__(self, settings: "Settings") -> None:
        self._settings = settings
        self._rules = self._build_rules()
        self._service_client: BlobServiceClient | None = None

    def _build_rules(self) -> list[CleanupRule]:
        """Build the list of cleanup rules."""
        s = self._settings
        return [
            CleanupRule(
                container=s.temp_container,
                prefix=s.temp_png_prefix,
                max_age=timedelta(hours=s.temp_png_age_hours),
                description="Temporary PNG slides",
            ),
            CleanupRule(
                container=s.temp_container,
                prefix=s.temp_csv_prefix,
                max_age=timedelta(hours=s.temp_csv_age_hours),
                description="Temporary CSV exports",
            ),
            CleanupRule(
                container=s.files_container,
                prefix=s.raw_prefix,
                max_age=timedelta(days=s.raw_age_days),
                description="Raw original files",
            ),
            CleanupRule(
                container=s.generator_container,
                prefix=s.generator_prefix,
                max_age=timedelta(hours=s.generator_output_age_hours),
                description="Generator output files",
            ),
        ]

    async def run(self) -> CleanupResult:
        """Run all cleanup rules.

        Returns:
            CleanupResult with statistics.
        """
        dry_run = self._settings.cleanup_dry_run
        logger.info("Starting cleanup (dry_run=%s)", dry_run)

        total_files = 0
        total_bytes = 0
        errors: list[str] = []

        for rule in self._rules:
            try:
                files_deleted, bytes_freed = await self._run_rule(rule, dry_run)
                total_files += files_deleted
                total_bytes += bytes_freed
                logger.info(
                    "Cleaned %s: %d files, %.2f MB",
                    rule.description,
                    files_deleted,
                    bytes_freed / (1024 * 1024),
                )
            except Exception as e:
                error_msg = f"Failed to clean {rule.description}: {e}"
                logger.error(error_msg)
                errors.append(error_msg)

        logger.info(
            "Cleanup completed: %d files, %.2f MB (errors: %d)",
            total_files,
            total_bytes / (1024 * 1024),
            len(errors),
        )

        return CleanupResult(
            files_deleted=total_files,
            bytes_freed=total_bytes,
            errors=errors,
        )

    async def _run_rule(self, rule: CleanupRule, dry_run: bool) -> tuple[int, int]:
        """Run a single cleanup rule."""
        container = self._get_container_client(rule.container)

        cutoff_time = datetime.now(timezone.utc) - rule.max_age
        files_deleted = 0
        bytes_freed = 0

        async for blob in self._list_blobs(container, rule.prefix):
            if blob.creation_time and blob.creation_time < cutoff_time:
                if dry_run:
                    logger.info(
                        "[DRY RUN] Would delete: %s/%s (age: %s)",
                        rule.container,
                        blob.name,
                        datetime.now(timezone.utc) - blob.creation_time,
                    )
                    files_deleted += 1
                else:
                    try:
                        blob_client = container.get_blob_client(blob.name)
                        size = blob.size or 0
                        blob_client.delete_blob()
                        files_deleted += 1
                        bytes_freed += size
                        logger.debug("Deleted: %s/%s (%d bytes)", rule.container, blob.name, size)
                    except Exception as e:
                        logger.warning("Failed to delete %s: %s", blob.name, e)

        return files_deleted, bytes_freed

    def _get_container_client(self, container_name: str) -> ContainerClient:
        """Get a container client."""
        if self._service_client is None:
            conn_str = self._settings.azure_storage_connection_string
            if conn_str:
                self._service_client = BlobServiceClient.from_connection_string(conn_str)
            else:
                self._service_client = BlobServiceClient(
                    account_url="http://127.0.0.1:10000/devstoreaccount1",
                    credential="Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==",
                )

        return self._service_client.get_container_client(container_name)

    async def _list_blobs(
        self, container: ContainerClient, prefix: str
    ) -> AsyncIterator:
        """List blobs in a container with a prefix."""
        loop = asyncio.get_event_loop()
        blobs = await loop.run_in_executor(
            None, lambda: list(container.list_blobs(name_starts_with=prefix))
        )
        for blob in blobs:
            yield blob
