"""PPTX-specific blob client.

Delegates to the shared UnifiedBlobClient from common.
"""

from __future__ import annotations

import logging
from pathlib import Path

from src.common.blob_client import UnifiedBlobClient
from src.common.config import Settings

logger = logging.getLogger(__name__)


class PptxBlobClient(UnifiedBlobClient):
    """Blob client specialised for PPTX atomizer operations.

    Inherits all functionality from UnifiedBlobClient and adds
    PPTX-specific convenience methods.

    Usage::

        async with PptxBlobClient(settings) as client:
            pptx_bytes = await client.download_bytes("abc-123", "reports/q1.pptx")
            image_url = await client.upload_slide_image("abc-123", 0, png_bytes)
    """

    def __init__(self, settings: Settings) -> None:
        super().__init__(settings)

    async def download_pptx(self, file_id: str, blob_url: str) -> Path:
        """Download a PPTX file to a temporary local path."""
        return await self.download_to_file(file_id, blob_url)

    async def download_pptx_bytes(self, file_id: str, blob_url: str) -> bytes:
        """Download a PPTX file as raw bytes."""
        return await self.download_bytes(file_id, blob_url)
