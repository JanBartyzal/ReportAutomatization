"""Client for calling MS-ATM-AI via Dapr for embedding generation.

Used by the search_documents tool for semantic search via pgVector.
"""

from __future__ import annotations

import logging
from typing import Any

import httpx

from src.config import DAPR_HOST, DAPR_HTTP_PORT

logger = logging.getLogger(__name__)


class AiClient:
    """Async client for MS-ATM-AI via Dapr HTTP service invocation."""

    def __init__(
        self,
        dapr_host: str = DAPR_HOST,
        dapr_port: int = DAPR_HTTP_PORT,
    ) -> None:
        self._base_url = f"http://{dapr_host}:{dapr_port}"
        self._timeout = 30.0

    async def generate_embedding(
        self,
        text: str,
        org_id: str,
        user_id: str,
    ) -> list[float]:
        """Generate a vector embedding via MS-ATM-AI.

        Uses Dapr service invocation to call the AI gateway.

        Args:
            text: Text to generate embedding for.
            org_id: Organization ID for quota tracking.
            user_id: User ID for audit.

        Returns:
            1536-dimensional embedding vector.
        """
        # Dapr HTTP service invocation to ms-atm-ai
        url = f"{self._base_url}/v1.0/invoke/ms-atm-ai/method/embeddings"

        payload = {
            "text": text,
            "document_id": "mcp-search",
            "context": {
                "org_id": org_id,
                "user_id": user_id,
            },
        }

        try:
            async with httpx.AsyncClient(timeout=self._timeout) as client:
                response = await client.post(url, json=payload)
                response.raise_for_status()
                data = response.json()
                return data.get("embedding", [])
        except Exception as e:
            logger.error("Failed to generate embedding via MS-ATM-AI: %s", e)
            return []
