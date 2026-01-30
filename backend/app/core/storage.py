from azure.storage.blob import BlobServiceClient, BlobClient, ContainerClient, generate_blob_sas, BlobSasPermissions
from datetime import datetime, timedelta
from typing import Optional
from core.config import Get_Key
import logging

logger = logging.getLogger(__name__)

class BlobStorageClient:
    def __init__(self):
        self.connection_string = Get_Key("AZURE_STORAGE_CONNECTION_STRING")
        self.account_name = Get_Key("AZURE_STORAGE_ACCOUNT")
        self.account_key = Get_Key("AZURE_STORAGE_KEY")
        self.container_name = Get_Key("AZURE_STORAGE_CONTAINER", "ingestion-upload")
        
        self.service_client = None
        try:
            if self.connection_string:
                self.service_client = BlobServiceClient.from_connection_string(self.connection_string)
            elif self.account_name and self.account_key:
                self.service_client = BlobServiceClient(account_url=f"https://{self.account_name}.blob.core.windows.net", credential=self.account_key)
            else:
                logger.warning("BlobStorageClient initialized without credentials. Upload/Download will fail.")
        except Exception as e:
            logger.error(f"Failed to initialize BlobServiceClient: {e}")

    def ensure_container_exists(self):
        if not self.service_client:
            raise ValueError("BlobStorageClient not initialized with credentials")
        
        try:
            container_client = self.service_client.get_container_client(self.container_name)
            if not container_client.exists():
                container_client.create_container()
        except Exception as e:
            logger.error(f"Error checking/creating container {self.container_name}: {e}")
            raise

    def upload_file(self, content: bytes, blob_name: str) -> str:
        """
        Uploads bytes to blob storage and returns the blob URL.
        """
        if not self.service_client:
            raise ValueError("BlobStorageClient not initialized with credentials")

        self.ensure_container_exists()
        blob_client = self.service_client.get_blob_client(container=self.container_name, blob=blob_name)
        blob_client.upload_blob(content, overwrite=True)
        return blob_client.url

    def download_file(self, blob_name: str) -> bytes:
        """
        Downloads bytes from blob storage.
        """
        if not self.service_client:
            raise ValueError("BlobStorageClient not initialized with credentials")

        blob_client = self.service_client.get_blob_client(container=self.container_name, blob=blob_name)
        return blob_client.download_blob().readall()

    def generate_sas_url(self, blob_name: str, expiry_hours: int = 1) -> str:
        """
        Generates a SAS URL for the blob.
        """
        if not self.account_name or not self.account_key:
             # Try to extract from connection string if meaningful, otherwise fail
             # For simpler logic, we demand account_name/key for SAS or parse conn string
             raise ValueError("Account name and key are required for SAS generation")

        sas_token = generate_blob_sas(
            account_name=self.account_name,
            container_name=self.container_name,
            blob_name=blob_name,
            account_key=self.account_key,
            permission=BlobSasPermissions(read=True),
            expiry=datetime.utcnow() + timedelta(hours=expiry_hours)
        )
        
        return f"https://{self.account_name}.blob.core.windows.net/{self.container_name}/{blob_name}?{sas_token}"
