"""Async HTTP client for the LiteLLM proxy (OpenAI-compatible API).

All LLM calls in the platform MUST go through this client, which targets
the LiteLLM gateway as mandated by project standards.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

import httpx

from src.config import LITELLM_API_KEY, LITELLM_BASE_URL

logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class ChatCompletionResult:
    """Parsed result from a chat completion call."""

    content: str
    prompt_tokens: int
    completion_tokens: int
    total_tokens: int


@dataclass(frozen=True, slots=True)
class EmbeddingResult:
    """Parsed result from an embedding call."""

    embedding: list[float]
    total_tokens: int


class LiteLLMClient:
    """Async wrapper around the LiteLLM OpenAI-compatible API."""

    def __init__(
        self,
        base_url: str = LITELLM_BASE_URL,
        api_key: str = LITELLM_API_KEY,
        timeout: float = 120.0,
    ) -> None:
        self._base_url = base_url.rstrip("/")
        self._headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }
        self._timeout = timeout

    async def chat_completion(
        self,
        model: str,
        messages: list[dict[str, str]],
        temperature: float = 0.1,
        max_tokens: int = 2048,
        response_format: dict[str, str] | None = None,
    ) -> ChatCompletionResult:
        """Send a chat completion request to LiteLLM.

        Args:
            model: Model name as configured in LiteLLM proxy.
            messages: List of message dicts with 'role' and 'content'.
            temperature: Sampling temperature.
            max_tokens: Maximum tokens in response.
            response_format: Optional response format (e.g., {"type": "json_object"}).

        Returns:
            Parsed ChatCompletionResult with content and token usage.
        """
        payload: dict[str, Any] = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        if response_format is not None:
            payload["response_format"] = response_format

        async with httpx.AsyncClient(timeout=self._timeout) as client:
            response = await client.post(
                f"{self._base_url}/v1/chat/completions",
                headers=self._headers,
                json=payload,
            )
            response.raise_for_status()

        data = response.json()
        usage = data.get("usage", {})

        return ChatCompletionResult(
            content=data["choices"][0]["message"]["content"],
            prompt_tokens=usage.get("prompt_tokens", 0),
            completion_tokens=usage.get("completion_tokens", 0),
            total_tokens=usage.get("total_tokens", 0),
        )

    async def create_embedding(
        self,
        model: str,
        text: str,
    ) -> EmbeddingResult:
        """Generate a vector embedding via LiteLLM.

        Args:
            model: Embedding model name as configured in LiteLLM proxy.
            text: Input text to embed.

        Returns:
            Parsed EmbeddingResult with the embedding vector and token count.
        """
        payload = {
            "model": model,
            "input": text,
        }

        async with httpx.AsyncClient(timeout=self._timeout) as client:
            response = await client.post(
                f"{self._base_url}/v1/embeddings",
                headers=self._headers,
                json=payload,
            )
            response.raise_for_status()

        data = response.json()
        usage = data.get("usage", {})

        return EmbeddingResult(
            embedding=data["data"][0]["embedding"],
            total_tokens=usage.get("total_tokens", 0),
        )
