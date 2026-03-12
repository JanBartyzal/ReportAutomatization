"""Tests for AiService – core AI operations."""

from __future__ import annotations

import json
from unittest.mock import AsyncMock, MagicMock

import pytest

from src.client.litellm_client import ChatCompletionResult, EmbeddingResult
from src.models.quota import QuotaStatus
from src.service.ai_service import AiService


@pytest.fixture
def mock_llm_client() -> AsyncMock:
    return AsyncMock()


@pytest.fixture
def mock_prompt_service() -> MagicMock:
    svc = MagicMock()
    svc.get_messages.return_value = [
        {"role": "system", "content": "You are a classifier."},
        {"role": "user", "content": "Classify: test text"},
    ]
    return svc


@pytest.fixture
def mock_quota_service() -> AsyncMock:
    svc = AsyncMock()
    svc.is_exceeded.return_value = False
    svc.record_usage.return_value = QuotaStatus(
        org_id="org-1",
        tokens_used_month=100,
        tokens_remaining=999900,
        quota_limit=1000000,
        is_exceeded=False,
    )
    return svc


@pytest.fixture
def mock_rate_limiter() -> MagicMock:
    limiter = MagicMock()
    limiter.acquire.return_value.__aenter__ = AsyncMock()
    limiter.acquire.return_value.__aexit__ = AsyncMock()
    return limiter


@pytest.fixture
def ai_service(
    mock_llm_client: AsyncMock,
    mock_prompt_service: MagicMock,
    mock_quota_service: AsyncMock,
    mock_rate_limiter: MagicMock,
) -> AiService:
    return AiService(
        llm_client=mock_llm_client,
        prompt_service=mock_prompt_service,
        quota_service=mock_quota_service,
        rate_limiter=mock_rate_limiter,
    )


async def test_analyze_semantic_classify(
    ai_service: AiService,
    mock_llm_client: AsyncMock,
) -> None:
    """AnalyzeSemantic with CLASSIFY returns classification and entities."""
    mock_llm_client.chat_completion.return_value = ChatCompletionResult(
        content=json.dumps({
            "classification": "IT_COSTS",
            "entities": {"amount": "50000 CZK"},
        }),
        prompt_tokens=50,
        completion_tokens=30,
        total_tokens=80,
    )

    result = await ai_service.analyze_semantic(
        org_id="org-1",
        user_id="user-1",
        text="IT infrastructure costs 50000 CZK",
        analysis_type="CLASSIFY",
    )

    assert result.classification == "IT_COSTS"
    assert result.entities["amount"] == "50000 CZK"
    assert result.tokens_used == 80


async def test_analyze_semantic_summarize(
    ai_service: AiService,
    mock_llm_client: AsyncMock,
) -> None:
    """AnalyzeSemantic with SUMMARIZE returns plain text summary."""
    mock_llm_client.chat_completion.return_value = ChatCompletionResult(
        content="The report covers Q1 IT expenses totaling 150k CZK.",
        prompt_tokens=100,
        completion_tokens=20,
        total_tokens=120,
    )

    result = await ai_service.analyze_semantic(
        org_id="org-1",
        user_id="user-1",
        text="Long financial report text...",
        analysis_type="SUMMARIZE",
    )

    assert "Q1 IT expenses" in result.result
    assert result.tokens_used == 120


async def test_generate_embeddings_dimension(
    ai_service: AiService,
    mock_llm_client: AsyncMock,
) -> None:
    """GenerateEmbeddings produces a 1536-dimensional vector."""
    embedding_vector = [0.1] * 1536
    mock_llm_client.create_embedding.return_value = EmbeddingResult(
        embedding=embedding_vector,
        total_tokens=15,
    )

    result = await ai_service.generate_embeddings(
        org_id="org-1",
        user_id="user-1",
        document_id="doc-abc",
        text="Some document text for embedding",
    )

    assert len(result.embedding) == 1536
    assert result.document_id == "doc-abc"
    assert result.tokens_used == 15


async def test_suggest_cleaning(
    ai_service: AiService,
    mock_llm_client: AsyncMock,
) -> None:
    """SuggestCleaning returns normalized column suggestions."""
    mock_llm_client.chat_completion.return_value = ChatCompletionResult(
        content=json.dumps({
            "suggestions": [
                {
                    "col_index": 0,
                    "original_name": "Naklady",
                    "suggested_name": "costs",
                    "suggested_type": "CURRENCY",
                    "confidence": 0.92,
                },
                {
                    "col_index": 1,
                    "original_name": "Datum",
                    "suggested_name": "date",
                    "suggested_type": "DATE",
                    "confidence": 0.95,
                },
            ]
        }),
        prompt_tokens=80,
        completion_tokens=60,
        total_tokens=140,
    )

    result = await ai_service.suggest_cleaning(
        org_id="org-1",
        user_id="user-1",
        headers=["Naklady", "Datum"],
        sample_rows=[["50000", "2024-01-15"], ["30000", "2024-02-01"]],
    )

    assert len(result.suggestions) == 2
    assert result.suggestions[0].original_name == "Naklady"
    assert result.suggestions[0].suggested_name == "costs"
    assert result.suggestions[1].suggested_type == "DATE"
    assert result.tokens_used == 140
