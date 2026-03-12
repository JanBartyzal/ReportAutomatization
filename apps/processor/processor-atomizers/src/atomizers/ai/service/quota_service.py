"""Token quota management backed by Dapr state store."""

from __future__ import annotations

import logging
from datetime import datetime, timezone

from src.atomizers.ai.client.dapr_client import DaprStateClient
from src.atomizers.ai.models.quota import QuotaStatus

logger = logging.getLogger(__name__)


class QuotaService:
    """Manages per-org monthly token quotas."""

    def __init__(
        self,
        state_client: DaprStateClient | None = None,
        monthly_token_quota: int = 1_000_000,
    ) -> None:
        self._state = state_client or DaprStateClient()
        self._monthly_quota = monthly_token_quota

    @staticmethod
    def _quota_key(org_id: str) -> str:
        """Build the state store key for the current month."""
        now = datetime.now(tz=timezone.utc)
        return f"quota:{org_id}:{now.strftime('%Y-%m')}"

    async def get_quota_status(self, org_id: str) -> QuotaStatus:
        """Return the current quota status for an organization."""
        key = self._quota_key(org_id)
        state = await self._state.get_state(key)

        tokens_used = state.get("tokens_used", 0) if isinstance(state, dict) else 0
        quota_limit = self._monthly_quota
        tokens_remaining = max(0, quota_limit - tokens_used)

        return QuotaStatus(
            org_id=org_id,
            tokens_used_month=tokens_used,
            tokens_remaining=tokens_remaining,
            quota_limit=quota_limit,
            is_exceeded=tokens_used >= quota_limit,
        )

    async def is_exceeded(self, org_id: str) -> bool:
        """Check whether the organization has exceeded its monthly quota."""
        status = await self.get_quota_status(org_id)
        return status.is_exceeded

    async def record_usage(self, org_id: str, user_id: str, tokens_used: int, model: str) -> QuotaStatus:
        """Record token usage and return updated quota status."""
        key = self._quota_key(org_id)
        state = await self._state.get_state(key)

        current_used = state.get("tokens_used", 0) if isinstance(state, dict) else 0
        new_used = current_used + tokens_used

        await self._state.save_state(key, {
            "tokens_used": new_used,
            "last_user_id": user_id,
            "last_model": model,
        })

        logger.info(
            "Recorded %d tokens for org=%s user=%s model=%s (total=%d/%d)",
            tokens_used, org_id, user_id, model, new_used, self._monthly_quota,
        )

        quota_limit = self._monthly_quota
        return QuotaStatus(
            org_id=org_id,
            tokens_used_month=new_used,
            tokens_remaining=max(0, quota_limit - new_used),
            quota_limit=quota_limit,
            is_exceeded=new_used >= quota_limit,
        )
