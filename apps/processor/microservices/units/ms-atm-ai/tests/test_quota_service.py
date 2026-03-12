"""Tests for QuotaService – token quota management."""

from __future__ import annotations

from unittest.mock import AsyncMock

import pytest

from src.service.quota_service import QuotaService


@pytest.fixture
def mock_state_client() -> AsyncMock:
    return AsyncMock()


@pytest.fixture
def quota_service(mock_state_client: AsyncMock) -> QuotaService:
    return QuotaService(state_client=mock_state_client)


async def test_quota_not_exceeded_when_empty(
    quota_service: QuotaService,
    mock_state_client: AsyncMock,
) -> None:
    """Fresh org with no usage should not be exceeded."""
    mock_state_client.get_state.return_value = None

    status = await quota_service.get_quota_status("org-new")

    assert status.tokens_used_month == 0
    assert status.is_exceeded is False
    assert status.tokens_remaining > 0


async def test_quota_exceeded_when_at_limit(
    quota_service: QuotaService,
    mock_state_client: AsyncMock,
) -> None:
    """Org that has consumed the full quota should be exceeded."""
    mock_state_client.get_state.return_value = {"tokens_used": 1_000_000}

    exceeded = await quota_service.is_exceeded("org-full")

    assert exceeded is True


async def test_record_usage_increments_counter(
    quota_service: QuotaService,
    mock_state_client: AsyncMock,
) -> None:
    """Recording usage should increment the stored token count."""
    mock_state_client.get_state.return_value = {"tokens_used": 500}

    status = await quota_service.record_usage(
        org_id="org-1",
        user_id="user-1",
        tokens_used=100,
        model="gpt-4o",
    )

    assert status.tokens_used_month == 600
    mock_state_client.save_state.assert_called_once()
    saved_value = mock_state_client.save_state.call_args[0][1]
    assert saved_value["tokens_used"] == 600


async def test_record_usage_from_zero(
    quota_service: QuotaService,
    mock_state_client: AsyncMock,
) -> None:
    """Recording usage when no prior state exists starts from zero."""
    mock_state_client.get_state.return_value = None

    status = await quota_service.record_usage(
        org_id="org-new",
        user_id="user-1",
        tokens_used=250,
        model="text-embedding-3-small",
    )

    assert status.tokens_used_month == 250
    saved_value = mock_state_client.save_state.call_args[0][1]
    assert saved_value["tokens_used"] == 250
