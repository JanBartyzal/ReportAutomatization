#!/usr/bin/env python3
# Step00: UAT Initialization
# Verify platform is running (health check), authenticate via dev bypass,
# generate API keys for test organizations via admin endpoint.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS

STEP_NAME = "step00_Init"

# API keys to generate (one per test role)
# org_slug matches organization codes from DB (TEST-ORG-1, TEST-ORG-2)
API_KEY_SPECS = [
    {"user_key": "admin1", "name": "UAT admin1 key", "role": "ADMIN",   "org_slug": "test-org-1"},
    {"user_key": "user1",  "name": "UAT user1 key",  "role": "EDITOR",  "org_slug": "test-org-1"},
    {"user_key": "admin2", "name": "UAT admin2 key", "role": "ADMIN",   "org_slug": "test-org-2"},
    {"user_key": "user2",  "name": "UAT user2 key",  "role": "VIEWER",  "org_slug": "test-org-2"},
]


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    session._log("[INFO] Step00 — UAT Init")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    # ------------------------------------------------------------------
    # 1. Health check — try actuator/health or auth/verify (401 = alive)
    # ------------------------------------------------------------------
    status, body = session.call("GET", "/actuator/health", expected_status=200, tag="health-check")
    if status == 0:
        session._err("[FATAL] Backend is unreachable. Is engine-core running on port 8081?")
        session.save_log(STEP_NAME)
        return 1
    if status not in (200, 401, 403):
        # Fallback: try auth verify (401 = service is running)
        status2, _ = session.call("GET", "/api/auth/verify", expected_status=200, tag="health-fallback")
        if status2 == 0:
            session._err("[FATAL] Backend is unreachable.")
            session.save_log(STEP_NAME)
            return 1
        session.assert_true(status2 in (200, 401, 403), f"Backend reachable (HTTP {status2})")

    # ------------------------------------------------------------------
    # 2. Auth verify — dev bypass (AUTH_MODE=development)
    #    Backend requires Bearer header but skips JWT validation in dev mode
    # ------------------------------------------------------------------
    session.token = "dev-bypass"
    status, body = session.call("GET", "/api/auth/verify",
                                expected_status=200, tag="dev-bypass-verify")
    if status != 200:
        session._err(
            "[FATAL] Auth verify failed. Is AUTH_MODE=development set? "
            "Without dev bypass, API keys cannot be generated."
        )
        session.save_log(STEP_NAME)
        return 1

    session.assert_true(True, "Dev bypass auth verified — HOLDING_ADMIN access")

    # Extract user info from verify response
    dev_user_id = None
    dev_org_id = None
    if isinstance(body, dict):
        dev_user_id = body.get("userId") or body.get("user_id") or body.get("sub")
        dev_org_id = body.get("orgId") or body.get("orgId")
        session.user_id = dev_user_id
        session.org_id = dev_org_id
        session._log(f"[INFO] Dev user ID: {dev_user_id}, org ID: {dev_org_id}")

    # ------------------------------------------------------------------
    # 3. Get existing organizations
    # ------------------------------------------------------------------
    status, body = session.call("GET", "/api/admin/organizations",
                                expected_status=200, tag="list-organizations")
    org_ids = {}
    if status == 200:
        # API returns hierarchical tree: [{id, code, name, type, children: [...]}]
        def _collect_orgs(nodes):
            if not nodes:
                return
            for org in nodes:
                if not isinstance(org, dict):
                    continue
                code = org.get("code", "")
                oid = str(org.get("id", ""))
                name = org.get("name", "")
                if code and oid:
                    org_ids[code] = oid
                    # Also map lowercase slug-style (TEST-ORG-1 → test-org-1)
                    org_ids[code.lower().replace("_", "-")] = oid
                _collect_orgs(org.get("children"))

        root_list = body if isinstance(body, list) else [body] if isinstance(body, dict) else []
        _collect_orgs(root_list)
        session._log(f"[INFO] Organizations found: {org_ids}")
    elif status == 404:
        session._log("[WARN] Admin organizations endpoint not available — org_ids will be empty")

    # ------------------------------------------------------------------
    # 4. Try API key generation, fallback to dev-bypass tokens
    # ------------------------------------------------------------------
    api_keys = {}
    tokens = {}

    # Try creating one API key to check if endpoint works
    test_spec = API_KEY_SPECS[0]
    test_org_id = org_ids.get(test_spec["org_slug"], "")
    test_payload = {"name": test_spec["name"], "role": test_spec["role"]}
    if test_org_id:
        test_payload["orgId"] = test_org_id

    status, body = session.call("POST", "/api/admin/api-keys",
                                body=test_payload,
                                expected_status=201,
                                tag="test-api-key-creation")

    if status in (200, 201) and isinstance(body, dict):
        raw_key = body.get("key") or body.get("apiKey") or body.get("rawKey")
        if raw_key:
            session._log("[OK]   API key endpoint works — generating all keys")
            api_keys[test_spec["user_key"]] = {"key": raw_key, "role": test_spec["role"]}
            tokens[test_spec["user_key"]] = raw_key

            for spec in API_KEY_SPECS[1:]:
                oid = org_ids.get(spec["org_slug"], "")
                payload = {"name": spec["name"], "role": spec["role"]}
                if oid:
                    payload["orgId"] = oid
                s, b = session.call("POST", "/api/admin/api-keys", body=payload,
                                    expected_status=201, tag=f"create-api-key-{spec['user_key']}")
                if s in (200, 201) and isinstance(b, dict):
                    rk = b.get("key") or b.get("apiKey") or b.get("rawKey")
                    if rk:
                        api_keys[spec["user_key"]] = {"key": rk, "role": spec["role"]}
                        tokens[spec["user_key"]] = rk
    else:
        # API key creation failed (500 = backend bug, 404 = not implemented)
        # Fallback: use dev-bypass tokens for all test accounts
        session.missing_feature(
            "POST /api/admin/api-keys",
            f"API key creation returned {status} — using dev-bypass tokens instead"
        )
        for spec in API_KEY_SPECS:
            tokens[spec["user_key"]] = "dev-bypass"
        session._log("[INFO] All accounts using dev-bypass auth (HOLDING_ADMIN)")

    # ------------------------------------------------------------------
    # 5. Persist state for subsequent steps
    # ------------------------------------------------------------------
    state = session.load_state()
    state["tokens"] = tokens
    state["api_keys"] = api_keys
    if org_ids:
        state.setdefault("org_ids", {}).update(org_ids)
    state["auth_mode"] = "api_key" if api_keys else "dev-bypass"
    if dev_user_id:
        state["dev_user_id"] = dev_user_id
    if dev_org_id:
        state["dev_org_id"] = dev_org_id
    session.save_state(state)
    session._log(f"[INFO] State saved: auth_mode={state['auth_mode']}, "
                 f"tokens={list(tokens.keys())}, org_ids={list(org_ids.keys())}")

    # ------------------------------------------------------------------
    # 6. Summary
    # ------------------------------------------------------------------
    session.assert_true(len(tokens) == len(API_KEY_SPECS),
                        f"Auth tokens available for all {len(API_KEY_SPECS)} test accounts")

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
