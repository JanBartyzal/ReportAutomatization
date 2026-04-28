"""ServiceNow-specific blob client delegating to UnifiedBlobClient."""

from __future__ import annotations

from src.common.blob_client import UnifiedBlobClient
from src.common.config import Settings


class ServiceNowBlobClient(UnifiedBlobClient):
    """Blob client specialised for ServiceNow export atomizer operations."""

    def __init__(self, settings: Settings) -> None:
        super().__init__(settings)

    async def download_export_bytes(self, file_id: str, blob_url: str) -> bytes:
        """Download a ServiceNow export file as raw bytes."""
        return await self.download_bytes(file_id, blob_url)
