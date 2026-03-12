"""Cleanup Worker - removes temporary files from blob storage.

Cleanup rules:
- Delete temporary PNG slides from Blob > 24 hours old
- Delete temporary CSV exports > 24 hours old
- Delete `_raw/` original files > 90 days old
- Delete temporary generator output files > 24 hours old
"""

from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import AsyncIterator

from azure.storage.blob import BlobServiceClient, ContainerClient

from src.config import (
    DRY_RUN,
    FILES_CONTAINER,
    GENERATOR_CONTAINER,
    GENERATOR_OUTPUT_AGE_HOURS,
    GENERATOR_PREFIX,
    RAW_AGE_DAYS,
    RAW_PREFIX,
    TEMP_CONTAINER,
    TEMP_CSV_AGE_HOURS,
    TEMP_CSV_PREFIX,
    TEMP_PNG_AGE_HOURS,
    TEMP_PNG_PREFIX,
)

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

    def __init__(self) -> None:
        """Initialize the cleanup worker."""
        self._rules = self._build_rules()
        self._service_client: BlobServiceClient | None = None

    def _build_rules(self) -> list[CleanupRule]:
        """Build the list of cleanup rules."""
        return [
            CleanupRule(
                container=TEMP_CONTAINER,
                prefix=TEMP_PNG_PREFIX,
                max_age=timedelta(hours=TEMP_PNG_AGE_HOURS),
                description="Temporary PNG slides",
            ),
            CleanupRule(
                container=TEMP_CONTAINER,
                prefix=TEMP_CSV_PREFIX,
                max_age=timedelta(hours=TEMP_CSV_AGE_HOURS),
                description="Temporary CSV exports",
            ),
            CleanupRule(
                container=FILES_CONTAINER,
                prefix=RAW_PREFIX,
                max_age=timedelta(days=RAW_AGE_DAYS),
                description="Raw original files",
            ),
            CleanupRule(
                container=GENERATOR_CONTAINER,
                prefix=GENERATOR_PREFIX,
                max_age=timedelta(hours=GENERATOR_OUTPUT_AGE_HOURS),
                description="Generator output files",
            ),
        ]

    async def run(self) -> CleanupResult:
        """Run all cleanup rules.

        Returns:
            CleanupResult with statistics.
        """
        logger.info("Starting cleanup (dry_run=%s)", DRY_RUN)

        total_files = 0
        total_bytes = 0
        errors: list[str] = []

        for rule in self._rules:
            try:
                files_deleted, bytes_freed = await self._run_rule(rule)
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

    async def _run_rule(self, rule: CleanupRule) -> tuple[int, int]:
        """Run a single cleanup rule.

        Args:
            rule: The cleanup rule to run.

        Returns:
            Tuple of (files_deleted, bytes_freed).
        """
        container = self._get_container_client(rule.container)

        cutoff_time = datetime.now(timezone.utc) - rule.max_age
        files_deleted = 0
        bytes_freed = 0

        async for blob in self._list_blobs(container, rule.prefix):
            # Check if blob is old enough
            if blob.creation_time and blob.creation_time < cutoff_time:
                if DRY_RUN:
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
                        # Get size before deletion
                        size = blob.size or 0
                        blob_client.delete_blob()
                        files_deleted += 1
                        bytes_freed += size
                        logger.debug("Deleted: %s/%s (%d bytes)", rule.container, blob.name, size)
                    except Exception as e:
                        logger.warning("Failed to delete %s: %s", blob.name, e)

        return files_deleted, bytes_freed

    def _get_container_client(self, container_name: str) -> ContainerClient:
        """Get a container client.

        Args:
            container_name: Name of the container.

        Returns:
            ContainerClient instance.
        """
        if self._service_client is None:
            from src.config import AZURE_STORAGE_CONNECTION_STRING

            if AZURE_STORAGE_CONNECTION_STRING:
                self._service_client = BlobServiceClient.from_connection_string(
                    AZURE_STORAGE_CONNECTION_STRING
                )
            else:
                # Use Azurite for local development
                self._service_client = BlobServiceClient(
                    account_url="http://127.0.0.1:10000/devstoreaccount1",
                    credential="Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==",
                )

        return self._service_client.get_container_client(container_name)

    async def _list_blobs(
        self, container: ContainerClient, prefix: str
    ) -> AsyncIterator[any]:
        """List blobs in a container with a prefix.

        Args:
            container: Container client.
            prefix: Blob name prefix.

        Yields:
            Blob properties.
        """
        # Azure SDK is synchronous, so we run it in a thread pool
        loop = asyncio.get_event_loop()
        blobs = await loop.run_in_executor(
            None, lambda: list(container.list_blobs(name_starts_with=prefix))
        )
        for blob in blobs:
            yield blob
