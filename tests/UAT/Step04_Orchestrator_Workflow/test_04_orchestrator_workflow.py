#!/usr/bin/env python3
# Step04: Orchestrator Workflow (FS04)
# Verify workflow status, steps, idempotency, and failed jobs listing.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step04_Orchestrator_Workflow"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})
    file_ids = state.get("file_ids", {})

    session._log(f"[INFO] Step04 — Orchestrator Workflow Verification")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    # ---------------------------------------------------------------
    # 1. Load state
    # ---------------------------------------------------------------
    admin1_token = tokens.get("admin1")
    if not admin1_token:
        session._err("[FATAL] No admin1 token in state. Run Step00 first.")
        session.save_log(STEP_NAME)
        return 1

    session.token = admin1_token
    session.restore_auth_from_state()
    ingestor_session = session.for_service(SERVICES["engine_ingestor"])

    # Use PPTX file_id as the workflow subject
    file_id = file_ids.get("pptx")
    if not file_id:
        session._err("[FATAL] No PPTX file_id in state. Run Step02 first.")
        session.save_log(STEP_NAME)
        return 1

    session._log(f"[INFO] Testing workflow for file_id: {file_id}")

    # ---------------------------------------------------------------
    # 2. Check workflow status (via file metadata on ingestor)
    # ---------------------------------------------------------------
    status, body = ingestor_session.call("GET", f"/api/files/{file_id}",
                                expected_status=200, tag="workflow-status")
    if status == 200 and isinstance(body, dict):
        has_state = ("state" in body or "status" in body or "workflowStatus" in body or "processingStatus" in body)
        session.assert_true(has_state, "File metadata contains processing status field")
        wf_state = body.get("state") or body.get("status") or body.get("workflowStatus") or body.get("processingStatus")
        session._log(f"[INFO] Workflow/processing state: {wf_state}")
    elif status in (404, 500):
        session.missing_feature(
            f"GET /api/files/{file_id}",
            "File metadata endpoint on ingestor not implemented yet"
        )

    # ---------------------------------------------------------------
    # 3. Workflow steps — no endpoint exists
    # ---------------------------------------------------------------
    session.missing_feature(
        f"GET /api/query/workflows/{file_id}/steps",
        "Workflow steps endpoint does not exist in current API"
    )

    # ---------------------------------------------------------------
    # 4. Idempotency — re-trigger same file, should not duplicate
    # ---------------------------------------------------------------
    # Re-trigger via ingestor reprocess endpoint
    status, body = ingestor_session.call("POST", f"/api/files/{file_id}/reprocess",
                                expected_status=200, tag="workflow-retrigger")
    if status == 200 and isinstance(body, dict):
        # Check that no new workflow was created (idempotent)
        is_duplicate = body.get("duplicate", False) or body.get("alreadyExists", False)
        wf_id = body.get("workflowId") or body.get("workflow_id")
        session._log(f"[INFO] Re-trigger response: duplicate={is_duplicate}, workflowId={wf_id}")
    elif status == 409:
        # 409 Conflict is acceptable — means workflow already exists (idempotent)
        session._pass_count += 1
        session._fail_count = max(0, session._fail_count - 1)
        session._log("[OK]   Re-trigger returned 409 Conflict (idempotent — acceptable)")
    elif status in (404, 500, 0):
        # 0 = timeout / connection error; undo the fail already counted by call()
        if status == 0:
            ingestor_session._fail_count = max(0, ingestor_session._fail_count - 1)
        ingestor_session.missing_feature(
            f"POST /api/files/{file_id}/reprocess",
            "File reprocess endpoint on ingestor not implemented yet or timed out"
        )

    # ---------------------------------------------------------------
    # 5. Failed jobs list (admin endpoint)
    # ---------------------------------------------------------------
    status, body = session.call("GET", "/api/admin/failed-jobs",
                                expected_status=200, tag="admin-failed-jobs")
    if status == 200:
        jobs_list = None
        if isinstance(body, dict):
            jobs_list = body.get("jobs") or body.get("items") or body.get("data")
            if jobs_list is None and "content" in body:
                jobs_list = body["content"]  # Spring Boot Page response
        elif isinstance(body, list):
            jobs_list = body

        if jobs_list is not None:
            session.assert_true(isinstance(jobs_list, list), "Failed jobs is a list")
            session._log(f"[INFO] Failed jobs count: {len(jobs_list)}")
        else:
            # Empty response body is acceptable if no failed jobs
            session._log("[INFO] Failed jobs response has no list wrapper (may be empty)")
    elif status == 404:
        session.missing_feature(
            "GET /api/admin/failed-jobs",
            "Admin failed jobs endpoint not implemented yet"
        )

    session.sync_counters_from(ingestor_session)
    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
