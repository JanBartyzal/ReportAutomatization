#!/usr/bin/env python3
# Step12: API Key Auth, AI Semantic Analysis & MCP (FS12)
# Verify API key authentication, AI analysis endpoints, cost control, and MCP health.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step12_API_AI_MCP"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})

    session._log(f"[INFO] Step12 — API Key Auth, AI & MCP Verification")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    # ---------------------------------------------------------------
    # 1. Load tokens
    # ---------------------------------------------------------------
    admin1 = USERS["admin1"]
    admin1_token = tokens.get("admin1")
    if not admin1_token:
        admin1_token = session.login(admin1["email"], admin1["password"])
        if admin1_token:
            tokens["admin1"] = admin1_token
    else:
        session.token = admin1_token
        session._log("[INFO] Reusing admin1 token from state")

    session.assert_true(admin1_token is not None, "admin1 token available")
    if not admin1_token:
        session._err("[FATAL] Cannot proceed without admin1 token.")
        session.save_log(STEP_NAME)
        return 1

    session.token = admin1_token
    session.restore_auth_from_state()

    # AI and MCP endpoints live on engine-data
    data_session = session.for_service(SERVICES["engine_data"])

    # ---------------------------------------------------------------
    # 2. API key auth — create key, then call /auth/me with it
    # ---------------------------------------------------------------
    # Always create a fresh API key (stale keys from previous runs may be invalid after DB reset)
    api_key = None
    status, body = session.call("POST", "/api/admin/api-keys",
                                body={"name": "UAT API Key"},
                                expected_status=201,
                                tag="create-api-key")
    if status in (404, 500):
        session.missing_feature("POST /api/admin/api-keys", "Endpoint not implemented yet")
    elif status in (200, 201) and isinstance(body, dict):
        api_key = body.get("key") or body.get("api_key") or body.get("token")
        session.assert_true(api_key is not None, "API key returned on create")

    if api_key:
        # Call /auth/me using API key instead of bearer token
        saved_token = session.token
        session.token = api_key
        status, body = session.call("GET", "/api/auth/me",
                                    expected_status=200,
                                    tag="auth-me-api-key")
        if status == 200 and isinstance(body, dict):
            session.assert_field(body, "email", label="API key auth returns email")
        session.token = saved_token

    # ---------------------------------------------------------------
    # 3. AI semantic analysis — POST /api/query/ai/analyze (engine-data)
    # ---------------------------------------------------------------
    status, body = data_session.call("POST", "/api/query/ai/analyze",
                                     body={"query": "analyze costs", "text": "Total cost is 1M"},
                                     expected_status=200, tag="ai-analyze")
    if status in (403, 404, 500):
        data_session.missing_feature("POST /api/query/ai/analyze",
                                     "AI semantic analysis endpoint not available on engine-data")

    # ---------------------------------------------------------------
    # 4. AI cost control — GET /api/query/ai/quota (engine-data)
    # ---------------------------------------------------------------
    status, body = data_session.call("GET", "/api/query/ai/quota",
                                     expected_status=200, tag="ai-quota")
    if status in (403, 404, 500):
        data_session.missing_feature("GET /api/query/ai/quota",
                                     "AI cost control / quota endpoint not available on engine-data")

    # ---------------------------------------------------------------
    # 5. MCP server health — GET /api/query/mcp/health (engine-data)
    # ---------------------------------------------------------------
    status, body = data_session.call("GET", "/api/query/mcp/health",
                                     expected_status=200, tag="mcp-health")
    if status in (403, 404, 500):
        data_session.missing_feature("GET /api/query/mcp/health",
                                     "MCP server health endpoint not available on engine-data")

    # ---------------------------------------------------------------
    # Save state
    # ---------------------------------------------------------------
    session.sync_counters_from(data_session)
    patch = {"tokens": tokens}
    if api_key:
        patch["api_key"] = api_key
    session.update_state(patch)

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
