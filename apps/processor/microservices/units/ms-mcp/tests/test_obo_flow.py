"""Tests for OBO flow – token validation and exchange."""

from __future__ import annotations

import pytest

from src.auth.obo_flow import TokenClaims, validate_token


def test_dev_mode_accepts_any_token() -> None:
    """In dev mode (no AZURE_CLIENT_ID), any token is accepted."""
    # With empty AZURE_CLIENT_ID, dev mode bypass is active
    claims = validate_token("arbitrary-token-value")

    assert isinstance(claims, TokenClaims)
    assert claims.user_id == "dev-user"
    assert claims.org_id == "dev-org"


def test_dev_mode_returns_admin_role() -> None:
    """Dev mode grants admin role by default."""
    claims = validate_token("test")

    assert "admin" in claims.roles
