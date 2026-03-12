"""Azure Blob Storage client for downloading and uploading files.

Wraps the shared ``python_base.BlobStorageClient`` and provides a unified
interface for all atomizers.
"""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Final
from urllib.parse import urlparse

from python_base import BlobStorageClient

from src.common.config import Settings

logger = logging.getLogger(__name__)

_PNG_CONTENT_TYPE: Final[str] = "image/png"


class UnifiedBlobClient:
    """High-level blob client for all atomizer operations.

    Usage::

        async with UnifiedBlobClient(settings) as client:
            data = await client.download_bytes("abc-123", "reports/q1.xlsx")
    """

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._client = BlobStorageClient(
            blob_storage_url=settings.blob_storage_url,
            connection_string=settings.azure_storage_connection_string,
        )

    # -- download ----------------------------------------------------------

    async def download_bytes(self, file_id: str, blob_url: str) -> bytes:
        """Download a file as raw bytes.

        Args:
            file_id: Unique file identifier (used for logging).
            blob_url: Blob URL or relative path.

        Returns:
            Raw file bytes.
        """
        container, blob_path = self._parse_blob_url(blob_url)
        logger.info("Downloading bytes file_id=%s from %s/%s", file_id, container, blob_path)
        return await self._client.download_bytes(container, blob_path)

    async def download_to_file(self, file_id: str, blob_url: str) -> Path:
        """Download a file to a temporary local path.

        Args:
            file_id: Unique file identifier (used for logging).
            blob_url: Blob URL or relative path.

        Returns:
            Path to the downloaded temporary file.
        """
        container, blob_path = self._parse_blob_url(blob_url)
        logger.info("Downloading file file_id=%s from %s/%s", file_id, container, blob_path)
        return await self._client.download_to_file(container, blob_path)

    # -- upload ------------------------------------------------------------

    async def upload_bytes(
        self,
        container: str,
        blob_path: str,
        data: bytes,
        content_type: str = "application/octet-stream",
    ) -> str:
        """Upload bytes to blob storage.

        Args:
            container: Blob container name.
            blob_path: Destination path within the container.
            data: The bytes to upload.
            content_type: MIME type of the blob content.

        Returns:
            The full URL of the uploaded blob.
        """
        return await self._client.upload_bytes(container, blob_path, data, content_type=content_type)

    async def upload_slide_image(
        self,
        file_id: str,
        slide_index: int,
        image_data: bytes,
    ) -> str:
        """Upload a rendered slide PNG to blob storage.

        Args:
            file_id: Unique file identifier.
            slide_index: Zero-based slide index.
            image_data: PNG image bytes.

        Returns:
            The full blob URL of the uploaded image.
        """
        blob_path = f"{file_id}/slides/slide_{slide_index}.png"
        logger.info(
            "Uploading slide image file_id=%s slide=%d (%d bytes)",
            file_id,
            slide_index,
            len(image_data),
        )
        return await self._client.upload_bytes(
            self._settings.artifacts_container,
            blob_path,
            image_data,
            content_type=_PNG_CONTENT_TYPE,
        )

    # -- helpers -----------------------------------------------------------

    def _parse_blob_url(self, blob_url: str) -> tuple[str, str]:
        """Split a blob URL into (container, path).

        Handles both full HTTP URLs and plain relative paths.

        Args:
            blob_url: Full blob URL or container-relative path.

        Returns:
            Tuple of (container_name, blob_path).
        """
        if blob_url.startswith("http://") or blob_url.startswith("https://"):
            parsed = urlparse(blob_url)
            parts = parsed.path.lstrip("/").split("/", 2)
            if len(parts) >= 3:
                return parts[1], parts[2]
            if len(parts) == 2:
                return parts[0], parts[1]
            return self._settings.blob_container, parts[0] if parts else blob_url
        return self._settings.blob_container, blob_url

    # -- context manager ---------------------------------------------------

    async def close(self) -> None:
        """Release underlying HTTP resources."""
        await self._client.close()

    async def __aenter__(self) -> UnifiedBlobClient:
        return self

    async def __aexit__(self, *args: object) -> None:
        await self.close()
