"""CSV-specific blob client delegating to UnifiedBlobClient."""

from __future__ import annotations

from src.common.blob_client import UnifiedBlobClient
from src.common.config import Settings


class CsvBlobClient(UnifiedBlobClient):
    """Blob client specialised for CSV atomizer operations."""

    def __init__(self, settings: Settings) -> None:
        super().__init__(settings)

    async def download_csv_bytes(self, file_id: str, blob_url: str) -> bytes:
        """Download a CSV file as raw bytes."""
        return await self.download_bytes(file_id, blob_url)
