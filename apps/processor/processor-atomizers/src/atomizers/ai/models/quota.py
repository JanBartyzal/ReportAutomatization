"""Pydantic models for AI token quota tracking."""

from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, Field


class TokenUsage(BaseModel):
    """Record of token consumption for a single request."""

    org_id: str
    user_id: str
    tokens_used: int
    model: str
    operation_type: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class QuotaStatus(BaseModel):
    """Current quota status for an organization."""

    org_id: str
    tokens_used_month: int = 0
    tokens_remaining: int = 0
    quota_limit: int = 0
    is_exceeded: bool = False
