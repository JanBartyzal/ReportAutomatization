"""Azure Blob Storage async client using httpx.

Supports both local development with Azurite and production Azure Blob Storage.
"""

from __future__ import annotations

import logging
import os
import tempfile
from pathlib import Path
from typing import Final

import httpx

logger = logging.getLogger(__name__)

_DEFAULT_AZURITE_URL: Final[str] = "http://127.0.0.1:10000/devstoreaccount1"
_DEFAULT_TIMEOUT: Final[float] = 120.0


class BlobStorageClient:
    """Async Azure Blob Storage client backed by httpx.

    For local development, set ``BLOB_STORAGE_URL`` to point at Azurite
    (e.g. ``http://127.0.0.1:10000/devstoreaccount1``).

    For production, provide ``AZURE_STORAGE_CONNECTION_STRING`` to authenticate
    via shared-key or SAS token derived from the connection string.

    Usage::

        async with BlobStorageClient() as client:
            data = await client.download_bytes("my-container", "path/to/blob.pptx")
            await client.upload_bytes("output-container", "result.json", json_bytes)
    """

    def __init__(
        self,
        blob_storage_url: str | None = None,
        connection_string: str | None = None,
        timeout: float = _DEFAULT_TIMEOUT,
    ) -> None:
        """Initialize the Blob Storage client.

        Args:
            blob_storage_url: Base URL for blob storage (overrides env var).
            connection_string: Azure Storage connection string for production auth.
            timeout: HTTP request timeout in seconds.
        """
        self._blob_storage_url = (
            blob_storage_url
            or os.environ.get("BLOB_STORAGE_URL")
            or _DEFAULT_AZURITE_URL
        ).rstrip("/")

        self._connection_string = connection_string or os.environ.get(
            "AZURE_STORAGE_CONNECTION_STRING"
        )
        self._timeout = timeout
        self._client: httpx.AsyncClient | None = None

    def _build_headers(self) -> dict[str, str]:
        """Build authentication headers from connection string if available.

        Returns:
            Dictionary of HTTP headers for blob storage requests.
        """
        headers: dict[str, str] = {
            "x-ms-version": "2023-11-03",
        }
        # For Azurite local development, no additional auth headers are needed
        # when using the well-known devstoreaccount1 key.
        # For production with SAS tokens, the token is appended to the URL.
        if self._connection_string:
            # Extract SAS token or account key from connection string
            parts = dict(
                part.split("=", 1)
                for part in self._connection_string.split(";")
                if "=" in part
            )
            sas_token = parts.get("SharedAccessSignature")
            if sas_token:
                # SAS token is appended to URLs, not sent in headers
                pass
            account_key = parts.get("AccountKey")
            if account_key:
                # For simplicity, use SAS-based auth in production.
                # Full SharedKey auth requires HMAC signing per request.
                logger.debug("AccountKey-based auth detected; prefer SAS tokens for httpx client.")
        return headers

    def _build_url(self, container: str, blob_path: str) -> str:
        """Construct the full blob URL.

        Args:
            container: Blob container name.
            blob_path: Path within the container.

        Returns:
            Full URL string for the blob.
        """
        url = f"{self._blob_storage_url}/{container}/{blob_path}"
        # Append SAS token if present in connection string
        if self._connection_string:
            parts = dict(
                part.split("=", 1)
                for part in self._connection_string.split(";")
                if "=" in part
            )
            sas_token = parts.get("SharedAccessSignature")
            if sas_token:
                separator = "&" if "?" in url else "?"
                url = f"{url}{separator}{sas_token}"
        return url

    async def _get_client(self) -> httpx.AsyncClient:
        """Get or create the httpx async client.

        Returns:
            An httpx.AsyncClient instance.
        """
        if self._client is None or self._client.is_closed:
            self._client = httpx.AsyncClient(
                timeout=httpx.Timeout(self._timeout),
                headers=self._build_headers(),
            )
        return self._client

    async def download_bytes(self, container: str, blob_path: str) -> bytes:
        """Download a blob and return its contents as bytes.

        Tries Azure SDK first (handles auth for both Azurite and production).
        Falls back to httpx only when the SDK is unavailable or hits a
        connection-level error — *not* when the blob simply doesn't exist.
        """
        if self._connection_string:
            try:
                from azure.storage.blob import BlobServiceClient
                service = BlobServiceClient.from_connection_string(self._connection_string)
                blob_client = service.get_blob_client(container, blob_path)
                data = blob_client.download_blob().readall()
                logger.info("Downloaded blob via SDK: %s/%s (%d bytes)", container, blob_path, len(data))
                return data
            except ImportError:
                logger.debug("azure-storage-blob not installed, falling back to httpx")
            except Exception as sdk_err:
                # Re-raise application-level errors (not-found, auth, etc.)
                # so they are not masked by the httpx fallback.
                err_code = getattr(sdk_err, "error_code", None)
                if err_code:
                    raise
                logger.warning("Azure SDK connection error, falling back to httpx: %s", sdk_err)

        # Fallback to httpx (for non-authenticated endpoints)
        client = await self._get_client()
        url = self._build_url(container, blob_path)
        logger.debug("Downloading blob via httpx: %s/%s", container, blob_path)

        response = await client.get(url)
        response.raise_for_status()

        logger.info("Downloaded blob: %s/%s (%d bytes)", container, blob_path, len(response.content))
        return response.content

    async def download_to_file(self, container: str, blob_path: str, dest_path: str | Path | None = None) -> Path:
        """Download a blob and stream it to a local file.

        Args:
            container: Blob container name.
            blob_path: Path to the blob within the container.
            dest_path: Destination file path. If None, a temporary file is created.

        Returns:
            Path to the downloaded file.

        Raises:
            httpx.HTTPStatusError: If the download request fails.
        """
        client = await self._get_client()
        url = self._build_url(container, blob_path)

        if dest_path is None:
            suffix = Path(blob_path).suffix or ".tmp"
            tmp = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
            dest_path = Path(tmp.name)
            tmp.close()
        else:
            dest_path = Path(dest_path)

        logger.debug("Downloading blob to file: %s/%s -> %s", container, blob_path, dest_path)

        async with client.stream("GET", url) as response:
            response.raise_for_status()
            with open(dest_path, "wb") as f:
                async for chunk in response.aiter_bytes(chunk_size=65536):
                    f.write(chunk)

        logger.info("Downloaded blob to file: %s/%s -> %s", container, blob_path, dest_path)
        return dest_path

    async def upload_bytes(
        self,
        container: str,
        blob_path: str,
        data: bytes,
        content_type: str = "application/octet-stream",
    ) -> str:
        """Upload bytes to a blob.

        Args:
            container: Blob container name.
            blob_path: Destination path within the container.
            data: The bytes to upload.
            content_type: MIME type of the blob content.

        Returns:
            The full URL of the uploaded blob.

        Raises:
            httpx.HTTPStatusError: If the upload request fails.
        """
        client = await self._get_client()
        url = self._build_url(container, blob_path)

        headers = {
            "Content-Type": content_type,
            "x-ms-blob-type": "BlockBlob",
        }

        logger.debug("Uploading blob: %s/%s (%d bytes)", container, blob_path, len(data))

        response = await client.put(url, content=data, headers=headers)
        response.raise_for_status()

        logger.info("Uploaded blob: %s/%s (%d bytes)", container, blob_path, len(data))
        return url

    async def upload_from_file(
        self,
        container: str,
        blob_path: str,
        file_path: str | Path,
        content_type: str = "application/octet-stream",
    ) -> str:
        """Upload a local file to blob storage.

        Args:
            container: Blob container name.
            blob_path: Destination path within the container.
            file_path: Path to the local file to upload.
            content_type: MIME type of the blob content.

        Returns:
            The full URL of the uploaded blob.

        Raises:
            FileNotFoundError: If the source file does not exist.
            httpx.HTTPStatusError: If the upload request fails.
        """
        file_path = Path(file_path)
        if not file_path.exists():
            raise FileNotFoundError(f"Source file not found: {file_path}")

        data = file_path.read_bytes()
        return await self.upload_bytes(container, blob_path, data, content_type)

    async def close(self) -> None:
        """Close the underlying httpx client."""
        if self._client is not None and not self._client.is_closed:
            await self._client.aclose()
            self._client = None

    async def __aenter__(self) -> BlobStorageClient:
        """Enter async context manager."""
        return self

    async def __aexit__(self, *args: object) -> None:
        """Exit async context manager and close the client."""
        await self.close()
