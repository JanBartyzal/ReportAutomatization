"""PDF-specific blob client delegating to UnifiedBlobClient."""

from __future__ import annotations

from src.common.blob_client import UnifiedBlobClient
from src.common.config import Settings


class PdfBlobClient(UnifiedBlobClient):
    """Blob client specialised for PDF atomizer operations."""

    def __init__(self, settings: Settings) -> None:
        super().__init__(settings)

    async def download_pdf_bytes(self, file_id: str, blob_url: str) -> bytes:
        """Download a PDF file as raw bytes."""
        return await self.download_bytes(file_id, blob_url)
