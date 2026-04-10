"""Core AI operations: semantic analysis, embeddings, and cleaning suggestions."""

from __future__ import annotations

import json
import logging
from dataclasses import dataclass

from src.atomizers.ai.client.litellm_client import LiteLLMClient
from src.atomizers.ai.service.prompt_service import PromptService
from src.atomizers.ai.service.quota_service import QuotaService
from src.atomizers.ai.service.rate_limiter import RateLimiter

logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class SemanticResult:
    """Result of a semantic analysis operation."""

    result: str
    entities: dict[str, str]
    classification: str
    tokens_used: int
    tokens_remaining: int


@dataclass(frozen=True, slots=True)
class EmbeddingServiceResult:
    """Result of an embedding generation operation."""

    document_id: str
    embedding: list[float]
    tokens_used: int


@dataclass(frozen=True, slots=True)
class ColumnSuggestion:
    """A single column cleaning suggestion."""

    col_index: int
    original_name: str
    suggested_name: str
    suggested_type: str
    confidence: float


@dataclass(frozen=True, slots=True)
class CleaningResult:
    """Result of a cleaning suggestion operation."""

    suggestions: list[ColumnSuggestion]
    tokens_used: int


class AiService:
    """Orchestrates AI operations with quota and rate limit enforcement."""

    def __init__(
        self,
        llm_client: LiteLLMClient | None = None,
        prompt_service: PromptService | None = None,
        quota_service: QuotaService | None = None,
        rate_limiter: RateLimiter | None = None,
        model_semantic: str = "gemma",
        model_embedding: str = "text-embedding-3-small",
    ) -> None:
        self._llm = llm_client or LiteLLMClient()
        self._prompts = prompt_service or PromptService()
        self._quota = quota_service or QuotaService()
        self._rate = rate_limiter or RateLimiter()
        self._model_semantic = model_semantic
        self._model_embedding = model_embedding

    async def analyze_semantic(
        self,
        org_id: str,
        user_id: str,
        text: str,
        analysis_type: str,
        parameters: dict[str, str] | None = None,
    ) -> SemanticResult:
        """Perform semantic analysis (classification, summarization, entity extraction)."""
        messages = self._prompts.get_messages(analysis_type, text=text, **(parameters or {}))

        async with self._rate.acquire(org_id):
            result = await self._llm.chat_completion(
                model=self._model_semantic,
                messages=messages,
                response_format={"type": "json_object"} if analysis_type != "SUMMARIZE" else None,
            )

        quota_status = await self._quota.record_usage(org_id, user_id, result.total_tokens, self._model_semantic)

        entities: dict[str, str] = {}
        classification = ""
        content = result.content

        if analysis_type != "SUMMARIZE":
            try:
                parsed = json.loads(content)
                entities = parsed.get("entities", {})
                classification = parsed.get("classification", "")
            except json.JSONDecodeError:
                logger.warning("Failed to parse JSON response for %s", analysis_type)

        return SemanticResult(
            result=content,
            entities=entities,
            classification=classification,
            tokens_used=result.total_tokens,
            tokens_remaining=quota_status.tokens_remaining,
        )

    async def generate_embeddings(
        self,
        org_id: str,
        user_id: str,
        document_id: str,
        text: str,
    ) -> EmbeddingServiceResult:
        """Generate a 1536-dimensional vector embedding for the given text."""
        async with self._rate.acquire(org_id):
            result = await self._llm.create_embedding(model=self._model_embedding, text=text)

        await self._quota.record_usage(org_id, user_id, result.total_tokens, self._model_embedding)

        return EmbeddingServiceResult(
            document_id=document_id,
            embedding=result.embedding,
            tokens_used=result.total_tokens,
        )

    async def suggest_cleaning(
        self,
        org_id: str,
        user_id: str,
        headers: list[str],
        sample_rows: list[list[str]],
    ) -> CleaningResult:
        """Suggest column name normalization and type detection."""
        headers_str = ", ".join(headers)
        rows_str = "\n".join([", ".join(row) for row in sample_rows[:10]])

        messages = self._prompts.get_messages("COLUMN_CLEANING", headers=headers_str, sample_rows=rows_str)

        async with self._rate.acquire(org_id):
            result = await self._llm.chat_completion(
                model=self._model_semantic,
                messages=messages,
                response_format={"type": "json_object"},
            )

        await self._quota.record_usage(org_id, user_id, result.total_tokens, self._model_semantic)

        suggestions: list[ColumnSuggestion] = []
        try:
            parsed = json.loads(result.content)
            for item in parsed.get("suggestions", []):
                suggestions.append(ColumnSuggestion(
                    col_index=item.get("col_index", 0),
                    original_name=item.get("original_name", ""),
                    suggested_name=item.get("suggested_name", ""),
                    suggested_type=item.get("suggested_type", "STRING"),
                    confidence=item.get("confidence", 0.0),
                ))
        except (json.JSONDecodeError, KeyError, TypeError) as exc:
            logger.warning("Failed to parse cleaning suggestions: %s", exc)

        return CleaningResult(
            suggestions=suggestions,
            tokens_used=result.total_tokens,
        )
