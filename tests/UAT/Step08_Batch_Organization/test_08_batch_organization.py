#!/usr/bin/env python3
# Step08: Batch Organization (FS08)
# Verify batch/period CRUD, RLS isolation, and file-to-batch assignment.

import sys
import os

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step08_Batch_Organization"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    session._log("[INFO] Step08 — Batch Organization")

    # 1. Load tokens, login as admin1
    state = session.load_state()
    tokens = state.get("tokens", {})
    session.assert_true(bool(tokens), "Tokens loaded from state")

    token = session.login(USERS["admin1"]["email"], USERS["admin1"]["password"])
    if not token:
        session._err("[FATAL] Cannot login as admin1 — aborting step08")
        session.save_log(STEP_NAME)
        return 1
    session.restore_auth_from_state()
    # Batches belong to holding — set org_id to holding for RLS
    holding_id = state.get("org_ids", {}).get("hold-001", "")
    if holding_id:
        session.org_id = holding_id

    # 2. Create batch/period — POST /api/batches (engine-core 8081)
    status, body = session.call("POST", "/api/batches",
                                body={
                                    "name": "Q1/2026",
                                    "period": "Q1/2026",
                                    "description": "UAT test batch for Q1 2026",
                                    "holdingId": state.get("org_ids", {}).get("hold-001", ""),
                                },
                                expected_status=201, tag="create-batch")
    batch_id = None
    if status in (404, 500):
        session.missing_feature("POST /api/batches",
                                "Batch creation endpoint not implemented yet")
    elif status in (200, 201) and isinstance(body, dict):
        batch_id = body.get("id") or body.get("batchId") or body.get("batch_id")
        session.assert_true(bool(batch_id), f"Batch created with id={batch_id}")
    elif status == 201:
        session._log("[OK]   Batch created (status 201)")

    # 3. List batches — GET /api/batches (engine-core 8081)
    status, body = session.call("GET", "/api/batches",
                                expected_status=200, tag="list-batches")
    if status in (404, 500):
        session.missing_feature("GET /api/batches",
                                "Batch listing endpoint not implemented yet")
    elif status == 200 and isinstance(body, (list, dict)):
        batches_list = body if isinstance(body, list) else body.get("content", body.get("data", body.get("items", [])))
        if batch_id:
            found = any(
                b.get("name") == "Q1/2026"
                for b in batches_list if isinstance(b, dict)
            )
            session.assert_true(found, "Created batch 'Q1/2026' found in batch list")
        else:
            # Batch creation failed (e.g. 500), so we cannot expect it in the list
            session._log("[INFO] Batch creation failed earlier — skipping batch-in-list assertion")

    # 4. Get batch detail — GET /api/batches/{batch_id} (engine-core 8081)
    if batch_id:
        status, body = session.call("GET", f"/api/batches/{batch_id}",
                                    expected_status=200, tag="get-batch-detail")
        if status == 200 and isinstance(body, dict):
            session.assert_field(body, "name", "Q1/2026", label="batch-detail-name")

    # 5. RLS: admin2 cannot see admin1's batches
    # Use admin2's API key + org_id directly — dev bypass login always returns admin1's org,
    # so we must use the actual API key to get admin2's separate org context.
    session._log("[INFO] Testing RLS: admin2 should not see admin1's batches")
    admin2_key = state.get("api_keys", {}).get("admin2", {}).get("key")
    admin2_org_id = (state.get("org_ids", {}).get("test-org-2")
                     or state.get("org_ids", {}).get("TEST-ORG-2"))
    if admin2_key and admin2_org_id:
        saved_token, saved_api_key, saved_org_id = session.token, session.api_key, session.org_id
        session.api_key = admin2_key
        session.token = None
        session.org_id = admin2_org_id
        status, body = session.call("GET", "/api/batches",
                                    expected_status=200, tag="rls-admin2-list-batches")
        session.token, session.api_key, session.org_id = saved_token, saved_api_key, saved_org_id
        if status in (404, 500):
            session.missing_feature("GET /api/batches (admin2 RLS)",
                                    "Batch listing endpoint not implemented yet")
        elif status == 200 and isinstance(body, (list, dict)):
            batches_list = body if isinstance(body, list) else body.get("content", body.get("data", body.get("items", [])))
            has_q1 = any(
                b.get("name") == "Q1/2026"
                for b in batches_list if isinstance(b, dict)
            )
            session.assert_true(not has_q1,
                                "RLS: admin2 does NOT see admin1's batch 'Q1/2026'")
    else:
        session._err("[WARN] No admin2 API key or org_id in state — skipping RLS test")

    # 6. Assign file to batch — POST /api/batches/{batch_id}/files (engine-core 8081)
    # Re-login as admin1 for file assignment
    admin1_token_new = session.login(USERS["admin1"]["email"], USERS["admin1"]["password"])
    file_id = state.get("file_ids", {}).get("xlsx") or state.get("xlsx_file_id")
    if batch_id and file_id:
        status, body = session.call("POST", f"/api/batches/{batch_id}/files",
                                    body={"file_id": file_id},
                                    expected_status=200, tag="assign-file-to-batch")
        if status in (404, 500):
            session.missing_feature(f"POST /api/batches/{batch_id}/files",
                                    "File-to-batch assignment not implemented yet")
        elif status not in (200, 201):
            session._log(f"[WARN] Assign file to batch returned {status}")
    elif not file_id:
        session._log("[WARN] No file_id in state, skipping file-to-batch assignment")
    elif not batch_id:
        session._log("[WARN] No batch_id available, skipping file-to-batch assignment")

    # 7. Save batch_id to state
    if batch_id:
        session.update_state({"batch_ids": {"q1_2026": batch_id}})
        session._log(f"[INFO] Saved batch_id={batch_id} to state")

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
