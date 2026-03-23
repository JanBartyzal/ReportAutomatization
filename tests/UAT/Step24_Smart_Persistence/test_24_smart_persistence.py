#!/usr/bin/env python3
# Step24: Smart Persistence (FS24)
# Verify promotion candidates listing, schema proposal, approval, and routing updates.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step24_Smart_Persistence"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})

    session._log(f"[INFO] Step24 — Smart Persistence Verification")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    # ---------------------------------------------------------------
    # 1. Load tokens, login as admin1
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

    # ---------------------------------------------------------------
    # 2. List promotion candidates — GET /api/admin/promotions/candidates
    # ---------------------------------------------------------------
    status, body = session.call("GET", "/api/admin/promotions/candidates",
                                expected_status=200,
                                tag="list-promotion-candidates")

    candidate_id = None
    if status == 200 and isinstance(body, (list, dict)):
        items = body if isinstance(body, list) else body.get("items", body.get("data", []))
        if items and len(items) > 0:
            candidate_id = items[0].get("id") or items[0].get("candidate_id")
            session._log(f"[INFO] Found candidate: {candidate_id}")
    if status not in (200,):
        session.missing_feature("/api/admin/promotions/candidates",
                                "List promotion candidates")

    # ---------------------------------------------------------------
    # 3. Get schema proposal — GET /api/admin/promotions/candidates/{id}/schema
    # ---------------------------------------------------------------
    cid = candidate_id or "candidate-1"
    status, body = session.call("GET", f"/api/admin/promotions/candidates/{cid}/schema",
                                expected_status=200,
                                tag="get-schema-proposal")
    if status not in (200,):
        session.missing_feature(f"/api/admin/promotions/candidates/{cid}/schema",
                                "Get schema proposal for promotion candidate")

    # ---------------------------------------------------------------
    # 4. Approve promotion — POST /api/admin/promotions/candidates/{id}/approve
    # ---------------------------------------------------------------
    approve_payload = {
        "modifications": {}
    }
    status, body = session.call("POST", f"/api/admin/promotions/candidates/{cid}/approve",
                                body=approve_payload,
                                expected_status=200,
                                tag="approve-promotion")
    if status not in (200, 201):
        session.missing_feature(f"/api/admin/promotions/candidates/{cid}/approve",
                                "Approve promotion of candidate to dedicated table")

    # ---------------------------------------------------------------
    # 5. Check routing updated — verify data routes to new table after promotion
    # ---------------------------------------------------------------
    session._log("[INFO] Checking routing update after promotion (advanced feature)")
    if candidate_id and status in (200, 201):
        # Re-fetch candidate to verify promotion status
        status2, body2 = session.call("GET", f"/api/admin/promotions/candidates/{cid}",
                                      expected_status=200,
                                      tag="check-routing-after-promotion")
        if status2 == 200 and isinstance(body2, dict):
            promoted = body2.get("status") in ("PROMOTED", "ACTIVE", "COMPLETED")
            session.assert_true(promoted, "Candidate status updated after promotion")
        elif status2 not in (200,):
            session.missing_feature(f"/api/admin/promotions/candidates/{cid}",
                                    "Check routing update after promotion")
    else:
        session.missing_feature("/api/admin/promotions/candidates/{id}",
                                "Verify routing update after promotion")

    # ---------------------------------------------------------------
    # 6. Note: FS24 is advanced — most endpoints expected to be unimplemented
    # ---------------------------------------------------------------
    session._log("[INFO] FS24 is advanced; most endpoints expected to use missing_feature().")

    # ---------------------------------------------------------------
    # Save state
    # ---------------------------------------------------------------
    session.update_state({"tokens": tokens})

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
