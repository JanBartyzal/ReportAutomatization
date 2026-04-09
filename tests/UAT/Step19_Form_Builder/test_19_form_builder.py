#!/usr/bin/env python3
# Step19: Form Builder (FS19)
# Verify form CRUD, publish, submissions, auto-save, validation, Excel export/import, and versioning.

import sys
import os
import io

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step19_Form_Builder"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    session._log("[INFO] Step19 — Form Builder")

    # 1. Load tokens from state, login as admin1
    state = session.load_state()
    tokens = state.get("tokens", {})
    session.assert_true(bool(tokens), "Tokens loaded from state")

    token = session.login(USERS["admin1"]["email"], USERS["admin1"]["password"])
    if not token:
        session._err("[FATAL] Cannot login as admin1 — aborting step19")
        session.save_log(STEP_NAME)
        return 1

    session.restore_auth_from_state()

    # Form endpoints live on engine-reporting
    reporting_session = session.for_service(SERVICES["engine_reporting"])

    # 2. Create form — POST /api/forms
    form_payload = {
        "name": "OPEX Q1 2026",
        "fields": [
            {"name": "it_costs", "type": "number", "required": True},
            {"name": "description", "type": "text", "required": False}
        ]
    }
    status, body = reporting_session.call("POST", "/api/forms",
                                body=form_payload,
                                expected_status=201, tag="create-form")
    if status in (400, 404, 500):
        reporting_session.missing_feature("POST /api/forms", "Endpoint not implemented yet")
    form_id = None
    if status == 201 and isinstance(body, dict):
        form_id = body.get("id") or body.get("form_id")

    if not form_id:
        session._log("[WARN] Cannot create form — skipping remaining form builder tests")
        session.sync_counters_from(reporting_session)
        session.update_state({"tokens": tokens})
        ok = session.save_log(STEP_NAME)
        return 0 if ok else 1

    # 3. List forms — GET /api/forms
    status, body = reporting_session.call("GET", "/api/forms",
                                expected_status=200, tag="list-forms")

    # 4. Get form — GET /api/forms/{form_id}, check fields
    status, body = reporting_session.call("GET", f"/api/forms/{form_id}",
                                expected_status=200, tag="get-form")
    if status == 200 and isinstance(body, dict):
        fields = body.get("fields", [])
        session.assert_true(len(fields) == 2, f"Form has 2 fields (got {len(fields)})")
        session.assert_field(body, "name", "OPEX Q1 2026", label="form-name")

    # 5. Publish form — POST /api/forms/{form_id}/publish
    status, body = reporting_session.call("POST", f"/api/forms/{form_id}/publish",
                                expected_status=200, tag="publish-form")

    # 6. Fill form as user1 — login as user1, POST /api/forms/{form_id}/submissions
    token_user1 = session.login(USERS["user1"]["email"], USERS["user1"]["password"])
    sub_id = None
    if token_user1:
        reporting_session.token = token_user1
        submission_payload = {
            "data": {
                "it_costs": 150000,
                "description": "IT OPEX Q1"
            }
        }
        status, body = reporting_session.call("POST", f"/api/forms/{form_id}/submissions",
                                    body=submission_payload,
                                    expected_status=201, tag="submit-form")
        if status == 201 and isinstance(body, dict):
            sub_id = body.get("id") or body.get("submission_id")
    else:
        session._err("[WARN] Cannot login as user1, skipping form submission")

    # 7. Auto-save — PUT /api/forms/{form_id}/submissions/{sub_id}/draft
    if sub_id:
        status, body = reporting_session.call("PUT", f"/api/forms/{form_id}/submissions/{sub_id}/draft",
                                    body={"data": {"it_costs": 160000}},
                                    expected_status=200, tag="auto-save-draft")
        if status not in (200, 201):
            session.missing_feature(f"PUT /api/forms/{form_id}/submissions/{sub_id}/draft",
                                    "Auto-save draft not yet implemented")
    else:
        session._log("[WARN] No submission_id, skipping auto-save test")

    # 8. Validate form — POST /api/forms/{form_id}/submissions/{sub_id}/validate
    if sub_id:
        status, body = reporting_session.call("POST", f"/api/forms/{form_id}/submissions/{sub_id}/validate",
                                    expected_status=200, tag="validate-submission")
        if status == 200 and isinstance(body, dict):
            session.assert_true("valid" in body or "errors" in body or "validation" in body,
                                "Validation response contains result field")
    else:
        session._log("[WARN] No submission_id, skipping validation test")

    # 9. Export Excel template — GET /api/forms/{form_id}/export/excel-template
    # Switch back to admin1 for admin operations (propagate full auth context)
    token = session.login(USERS["admin1"]["email"], USERS["admin1"]["password"])
    if token:
        session.restore_auth_from_state()
        reporting_session.token = token
        reporting_session.org_id = session.org_id
        reporting_session.user_id = session.user_id
        reporting_session.roles = session.roles

    status, body = reporting_session.call("GET", f"/api/forms/{form_id}/export/excel-template",
                                expected_status=200, tag="export-excel-template")
    if status == 200 and isinstance(body, bytes):
        session.assert_true(len(body) > 0, "Excel template export returned non-empty content")

    # 10. Import Excel — POST /api/forms/{form_id}/import/excel (multipart)
    # Minimal XLSX bytes (empty zip)
    minimal_xlsx = b"PK\x05\x06" + b"\x00" * 18
    files = {"file": ("import.xlsx", io.BytesIO(minimal_xlsx),
                       "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")}
    status, body = reporting_session.call("POST", f"/api/forms/{form_id}/import/excel",
                                files=files,
                                expected_status=200, tag="import-excel")
    if status not in (200, 201):
        session.missing_feature(f"POST /api/forms/{form_id}/import/excel",
                                "Excel import not yet implemented")

    # 11. Form versioning — edit form creates v2, check original submissions preserved
    updated_form = {
        "name": "OPEX Q1 2026",
        "fields": [
            {"name": "it_costs", "type": "number", "required": True},
            {"name": "description", "type": "text", "required": False},
            {"name": "notes", "type": "text", "required": False}
        ]
    }
    status, body = reporting_session.call("PUT", f"/api/forms/{form_id}",
                                body=updated_form,
                                expected_status=200, tag="edit-form-v2")
    if status == 200 and isinstance(body, dict):
        version = body.get("version") or body.get("v")
        if version:
            session.assert_true(int(version) >= 2, f"Form version incremented (got {version})")

    # Verify original submission still accessible
    if sub_id:
        token_user1 = session.login(USERS["user1"]["email"], USERS["user1"]["password"])
        if token_user1:
            reporting_session.token = token_user1
            status, body = reporting_session.call("GET", f"/api/forms/{form_id}/submissions/{sub_id}",
                                        expected_status=200, tag="get-original-submission")
            if status == 200 and isinstance(body, dict):
                data = body.get("data", {})
                session.assert_true(data.get("it_costs") in (150000, 160000),
                                    "Original submission data preserved after form edit")

    # 12. Save form_id and submission_id to state
    session.sync_counters_from(reporting_session)
    state_patch = {"form_ids": {"form_id": form_id}}
    if sub_id:
        state_patch["form_ids"]["submission_id"] = sub_id
    session.update_state(state_patch)
    session._log(f"[INFO] Saved form_ids to state: {state_patch}")

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
