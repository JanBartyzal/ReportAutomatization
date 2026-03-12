"""Blob Storage client for downloading PPTX templates and uploading generated reports.

Wraps the shared ``python_base.BlobStorageClient`` with generator-specific helpers.
Works with both Azurite (local) and Azure Blob Storage (production).
"""

from __future__ import annotations

import logging
from typing import Final
from urllib.parse import urlparse

from python_base import BlobStorageClient

from src.config import (
    AZURE_STORAGE_CONNECTION_STRING,
    BLOB_STORAGE_URL,
    GENERATED_CONTAINER,
    TEMPLATES_CONTAINER,
)

logger = logging.getLogger(__name__)

_PPTX_CONTENT_TYPE: Final[str] = "application/vnd.openxmlformats-officedocument.presentationml.presentation"


class GenPptxBlobClient:
    """High-level blob client for PPTX report generation.

    Usage::

        async with GenPptxBlobClient() as client:
            template_bytes = await client.download_template("tmpl-123", "templates/tmpl-123/v1/report.pptx")
            url = await client.upload_generated("rpt-456", pptx_bytes)
    """

    def __init__(self) -> None:
        self._client = BlobStorageClient(
            blob_storage_url=BLOB_STORAGE_URL,
            connection_string=AZURE_STORAGE_CONNECTION_STRING,
        )

    async def download_template(self, template_id: str, blob_url: str) -> bytes:
        """Download a PPTX template as raw bytes.

        Args:
            template_id: Template identifier (for logging).
            blob_url: Blob URL or relative path to the template file.

        Returns:
            Raw PPTX file bytes.
        """
        container, blob_path = self._parse_blob_url(blob_url, TEMPLATES_CONTAINER)
        logger.info("Downloading template template_id=%s from %s/%s", template_id, container, blob_path)
        return await self._client.download_bytes(container, blob_path)

    async def upload_generated(self, report_id: str, pptx_bytes: bytes) -> str:
        """Upload a generated PPTX report to blob storage.

        Args:
            report_id: Report identifier.
            pptx_bytes: Generated PPTX file bytes.

        Returns:
            The full blob URL of the uploaded report.
        """
        blob_path = f"{report_id}/report.pptx"
        logger.info(
            "Uploading generated report report_id=%s (%d bytes)",
            report_id,
            len(pptx_bytes),
        )
        return await self._client.upload_bytes(
            GENERATED_CONTAINER,
            blob_path,
            pptx_bytes,
            content_type=_PPTX_CONTENT_TYPE,
        )

    @staticmethod
    def _parse_blob_url(blob_url: str, default_container: str) -> tuple[str, str]:
        """Split a blob URL into (container, path).

        Handles both full HTTP URLs and plain relative paths.
        """
        if blob_url.startswith("http://") or blob_url.startswith("https://"):
            parsed = urlparse(blob_url)
            parts = parsed.path.lstrip("/").split("/", 2)
            if len(parts) >= 3:
                return parts[1], parts[2]
            if len(parts) == 2:
                return parts[0], parts[1]
            return default_container, parts[0] if parts else blob_url
        return default_container, blob_url

    async def close(self) -> None:
        """Release underlying HTTP resources."""
        await self._client.close()

    async def __aenter__(self) -> GenPptxBlobClient:
        return self

    async def __aexit__(self, *args: object) -> None:
        await self.close()
