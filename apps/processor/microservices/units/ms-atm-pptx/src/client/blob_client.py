"""Blob Storage client for downloading PPTX files and uploading PNG artifacts.

Wraps the shared ``python_base.BlobStorageClient`` with PPTX-specific helpers.
Works with both Azurite (local) and Azure Blob Storage (production).
"""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Final

from python_base import BlobStorageClient

from src.config import (
    ARTIFACTS_CONTAINER,
    AZURE_STORAGE_CONNECTION_STRING,
    BLOB_CONTAINER,
    BLOB_STORAGE_URL,
)

logger = logging.getLogger(__name__)

_PNG_CONTENT_TYPE: Final[str] = "image/png"


class PptxBlobClient:
    """High-level blob client specialised for PPTX atomizer operations.

    Usage::

        async with PptxBlobClient() as client:
            pptx_path = await client.download_pptx("abc-123", "reports/q1.pptx")
            image_url = await client.upload_slide_image("abc-123", 0, png_bytes)
    """

    def __init__(self) -> None:
        self._client = BlobStorageClient(
            blob_storage_url=BLOB_STORAGE_URL,
            connection_string=AZURE_STORAGE_CONNECTION_STRING,
        )

    # -- download ----------------------------------------------------------

    async def download_pptx(self, file_id: str, blob_url: str) -> Path:
        """Download a PPTX file from blob storage to a temporary local file.

        If *blob_url* is a full HTTP URL the container/path is extracted;
        otherwise it is treated as a relative path within the default container.

        Args:
            file_id: Unique file identifier (used for logging).
            blob_url: Blob URL or relative path to the PPTX file.

        Returns:
            Path to the downloaded temporary file.
        """
        container, blob_path = self._parse_blob_url(blob_url)
        logger.info("Downloading PPTX file_id=%s from %s/%s", file_id, container, blob_path)
        return await self._client.download_to_file(container, blob_path)

    async def download_pptx_bytes(self, file_id: str, blob_url: str) -> bytes:
        """Download a PPTX file as raw bytes.

        Args:
            file_id: Unique file identifier (used for logging).
            blob_url: Blob URL or relative path.

        Returns:
            Raw PPTX file bytes.
        """
        container, blob_path = self._parse_blob_url(blob_url)
        logger.info("Downloading PPTX bytes file_id=%s from %s/%s", file_id, container, blob_path)
        return await self._client.download_bytes(container, blob_path)

    # -- upload ------------------------------------------------------------

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
            ARTIFACTS_CONTAINER,
            blob_path,
            image_data,
            content_type=_PNG_CONTENT_TYPE,
        )

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

    async def __aenter__(self) -> PptxBlobClient:
        return self

    async def __aexit__(self, *args: object) -> None:
        await self.close()
