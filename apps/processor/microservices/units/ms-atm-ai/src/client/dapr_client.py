"""Dapr state store client for quota persistence.

Uses the Dapr HTTP API to read/write token usage counters
in the configured state store (Redis-backed).
"""

from __future__ import annotations

import json
import logging
from typing import Any

import httpx

from src.config import DAPR_HOST, DAPR_HTTP_PORT, DAPR_STATESTORE_NAME

logger = logging.getLogger(__name__)


class DaprStateClient:
    """Async client for Dapr state store operations."""

    def __init__(
        self,
        host: str = DAPR_HOST,
        port: int = DAPR_HTTP_PORT,
        store_name: str = DAPR_STATESTORE_NAME,
    ) -> None:
        self._base_url = f"http://{host}:{port}/v1.0/state/{store_name}"
        self._timeout = 10.0

    async def get_state(self, key: str) -> Any | None:
        """Retrieve a value from the state store.

        Returns None if the key does not exist.
        """
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            response = await client.get(f"{self._base_url}/{key}")
            if response.status_code == 204 or not response.content:
                return None
            response.raise_for_status()
            return response.json()

    async def save_state(self, key: str, value: Any) -> None:
        """Save a value to the state store."""
        payload = [
            {
                "key": key,
                "value": value,
            }
        ]
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            response = await client.post(
                self._base_url,
                content=json.dumps(payload),
                headers={"Content-Type": "application/json"},
            )
            response.raise_for_status()
