"""Dapr state store client for quota persistence.

Uses the Dapr HTTP API to read/write token usage counters
in the configured state store (Redis-backed).
"""

from __future__ import annotations

import json
import logging
from typing import Any

import httpx

logger = logging.getLogger(__name__)


class DaprStateClient:
    """Async client for Dapr state store operations."""

    def __init__(
        self,
        host: str = "localhost",
        port: int = 3500,
        store_name: str = "reportplatform-statestore",
    ) -> None:
        self._base_url = f"http://{host}:{port}/v1.0/state/{store_name}"
        self._timeout = 10.0

    async def get_state(self, key: str) -> Any | None:
        """Retrieve a value from the state store."""
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
