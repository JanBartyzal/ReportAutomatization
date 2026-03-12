"""Blob Storage client for downloading CSV files.

Wraps the shared ``python_base.BlobStorageClient`` with CSV-specific helpers.
Works with both Azurite (local) and Azure Blob Storage (production).
"""

from __future__ import annotations

import logging

from python_base import BlobStorageClient

from src.config import (
    AZURE_STORAGE_CONNECTION_STRING,
    BLOB_CONTAINER,
    BLOB_STORAGE_URL,
)

logger = logging.getLogger(__name__)


class CsvBlobClient:
    """High-level blob client specialised for CSV atomizer operations.

    Usage::

        async with CsvBlobClient() as client:
            csv_bytes = await client.download_csv_bytes("abc-123", "reports/data.csv")
    """

    def __init__(self) -> None:
        self._client = BlobStorageClient(
            blob_storage_url=BLOB_STORAGE_URL,
            connection_string=AZURE_STORAGE_CONNECTION_STRING,
        )

    # -- download ----------------------------------------------------------

    async def download_csv_bytes(self, file_id: str, blob_url: str) -> bytes:
        """Download a CSV file as raw bytes.

        Args:
            file_id: Unique file identifier (used for logging).
            blob_url: Blob URL or relative path.

        Returns:
            Raw CSV file bytes.
        """
        container, blob_path = self._parse_blob_url(blob_url)
        logger.info("Downloading CSV bytes file_id=%s from %s/%s", file_id, container, blob_path)
        return await self._client.download_bytes(container, blob_path)

    # -- helpers -----------------------------------------------------------

    @staticmethod
    def _parse_blob_url(blob_url: str) -> tuple[str, str]:
        """Split a blob URL into (container, path).

        Handles both full HTTP URLs and plain relative paths.

        Args:
            blob_url: Full blob URL or container-relative path.

        Returns:
            Tuple of (container_name, blob_path).
        """
        if blob_url.startswith("http://") or blob_url.startswith("https://"):
            # URL format: http://host:port/account/container/path...
            from urllib.parse import urlparse

            parsed = urlparse(blob_url)
            parts = parsed.path.lstrip("/").split("/", 2)
            if len(parts) >= 3:
                # /devstoreaccount1/container/path
                return parts[1], parts[2]
            if len(parts) == 2:
                return parts[0], parts[1]
            return BLOB_CONTAINER, parts[0] if parts else blob_url
        # Relative path: assume default container
        return BLOB_CONTAINER, blob_url

    # -- context manager ---------------------------------------------------

    async def close(self) -> None:
        """Release underlying HTTP resources."""
        await self._client.close()

    async def __aenter__(self) -> CsvBlobClient:
        return self

    async def __aexit__(self, *args: object) -> None:
        await self.close()
