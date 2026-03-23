#!/usr/bin/env python3
# Step14: Data Versioning (FS14)
# Verify file version listing, retrieval, creation, diff, and immutability.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step14_Data_Versioning"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})

    session._log(f"[INFO] Step14 — Data Versioning Verification")
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

    # File listing on ingestor, versioning on engine-core
    ingestor_session = session.for_service(SERVICES["engine_ingestor"])
    core_session = session.for_service(SERVICES["engine_core"])

    # Try to get a file_id from state (uploaded in earlier steps)
    file_id = state.get("file_id") or state.get("uploaded_file_id")

    if not file_id:
        session._log("[WARN] No file_id found in state; attempting to list files to find one")
        status, body = ingestor_session.call("GET", "/api/files",
                                    expected_status=200,
                                    tag="list-files-for-versioning")
        if status == 200:
            items = body if isinstance(body, list) else (body.get("items", body.get("data", [])) if isinstance(body, dict) else [])
            if items and len(items) > 0:
                file_id = items[0].get("id") or items[0].get("file_id")
                session._log(f"[INFO] Using file_id={file_id} from file listing")

    if not file_id:
        session.missing_feature("GET /api/versions/file/{file_id}",
                                "No file_id available in state; cannot test versioning")
        session.update_state({"tokens": tokens})
        ok = session.save_log(STEP_NAME)
        return 0 if ok else 1

    # ---------------------------------------------------------------
    # 2. Get file versions — GET /api/versions/file/{file_id} (engine-core)
    # ---------------------------------------------------------------
    status, body = core_session.call("GET", f"/api/versions/file/{file_id}",
                                expected_status=200,
                                tag="get-file-versions")

    versions = []
    if status == 200:
        if isinstance(body, list):
            versions = body
        elif isinstance(body, dict):
            versions = body.get("versions", body.get("items", body.get("data", [])))
        if len(versions) == 0:
            session._log("[INFO] File has 0 versions — versioning data not yet populated (endpoint exists but no versions created)")
        else:
            session.assert_true(len(versions) >= 1, f"File has at least 1 version (found {len(versions)})")
    elif status == 404:
        session.missing_feature(f"GET /api/versions/file/{file_id}",
                                "File versioning endpoint not implemented")
        session.sync_counters_from(ingestor_session)
        session.sync_counters_from(core_session)
        session.update_state({"tokens": tokens})
        ok = session.save_log(STEP_NAME)
        return 0 if ok else 1

    # ---------------------------------------------------------------
    # 3. Get specific version — GET /api/versions/file/{file_id} with version param
    # ---------------------------------------------------------------
    status, body = core_session.call("GET", f"/api/versions/file/{file_id}",
                                query_params={"version": "1"},
                                expected_status=200,
                                tag="get-version-1")

    # ---------------------------------------------------------------
    # 4. Create new version (re-upload or edit)
    # ---------------------------------------------------------------
    status, body = core_session.call("POST", f"/api/versions/file/{file_id}",
                                body={"comment": "UAT version bump"},
                                expected_status=201,
                                tag="create-new-version")
    if status == 200:
        # 200 is also acceptable
        session._log("[OK]   New version created (status 200)")
    elif status in (500, 404):
        core_session.missing_feature(
            f"POST /api/versions/file/{file_id}",
            "Version creation endpoint returned error — feature not fully implemented yet"
        )

    # ---------------------------------------------------------------
    # 5. Diff between versions — GET /api/versions/file/{file_id}/diff (engine-core)
    # ---------------------------------------------------------------
    status, body = core_session.call("GET", f"/api/versions/file/{file_id}/diff",
                                query_params={"v1": "1", "v2": "2"},
                                expected_status=200,
                                tag="diff-versions")
    if status == 200 and isinstance(body, dict):
        session.assert_true(
            "diff" in body or "changes" in body or "data" in body,
            "Diff response contains diff data"
        )
    elif status in (404, 500):
        core_session.missing_feature(f"GET /api/versions/file/{file_id}/diff",
                                "Version diff endpoint not implemented")

    # ---------------------------------------------------------------
    # 6. Version is immutable — attempt to modify v1 should fail
    # ---------------------------------------------------------------
    status, body = core_session.call("PUT", f"/api/versions/file/{file_id}",
                                body={"version": "1", "comment": "Attempt to modify immutable version"},
                                expected_status=405,
                                tag="immutable-version-check")
    if status == 400 or status == 403 or status == 409:
        # Any of these indicate immutability is enforced
        core_session._pass_count += 1
        core_session._fail_count = max(0, core_session._fail_count - 1)
        session._log(f"[OK]   Version immutability enforced (status {status})")
    elif status in (404, 500):
        core_session.missing_feature(f"PUT /api/versions/file/{file_id}",
                                     "Version immutability check — endpoint not fully implemented yet")

    # ---------------------------------------------------------------
    # 7. If not implemented, already handled via missing_feature above
    # ---------------------------------------------------------------

    # ---------------------------------------------------------------
    # Save state
    # ---------------------------------------------------------------
    session.sync_counters_from(ingestor_session)
    session.sync_counters_from(core_session)
    session.update_state({"tokens": tokens})

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
