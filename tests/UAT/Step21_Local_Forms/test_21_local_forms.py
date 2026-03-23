#!/usr/bin/env python3
# Step21: Local Forms (FS21)
# Verify local form CRUD, org isolation, submission, release, PPTX templates, and sharing.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step21_Local_Forms"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})

    session._log(f"[INFO] Step21 — Local Forms Verification")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    # ---------------------------------------------------------------
    # 1. Load tokens, login as admin1 (CompanyAdmin)
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

    # Create sub-session for engine-reporting (forms, pptx templates)
    reporting = session.for_service(SERVICES["engine_reporting"])

    # ---------------------------------------------------------------
    # 2. Create local form (scope=LOCAL) — POST /api/forms
    # ---------------------------------------------------------------
    form_payload = {
        "name": "Internal IT Report",
        "scope": "LOCAL",
        "fields": [
            {"name": "hw_costs", "type": "number"},
            {"name": "sw_costs", "type": "number"}
        ]
    }
    status, body = reporting.call("POST", "/api/forms",
                                   body=form_payload,
                                   expected_status=201,
                                   tag="create-local-form")
    if status in (400, 404, 500):
        reporting.missing_feature("POST /api/forms", "Endpoint not implemented yet")

    form_id = None
    if status == 201 and isinstance(body, dict):
        form_id = body.get("id") or body.get("form_id")
        reporting.assert_true(form_id is not None, "Form ID returned on create")

    # ---------------------------------------------------------------
    # 3. Local form not visible to other org — login as admin2
    # ---------------------------------------------------------------
    admin2 = USERS["admin2"]
    admin2_token = tokens.get("admin2")
    if not admin2_token:
        admin2_token = session.login(admin2["email"], admin2["password"])
        if admin2_token:
            tokens["admin2"] = admin2_token

    if admin2_token:
        reporting.token = admin2_token
        status, body = reporting.call("GET", "/api/forms",
                                      expected_status=200,
                                      tag="admin2-list-forms")
        if status in (404, 500):
            reporting.missing_feature("GET /api/forms (admin2)", "Endpoint not implemented yet")
        if status == 200 and isinstance(body, (list, dict)):
            items = body if isinstance(body, list) else body.get("items", body.get("data", []))
            not_found = not any(
                d.get("name") == "Internal IT Report"
                for d in items
            )
            reporting.assert_true(not_found, "Local form not visible to other org (admin2)")

    # ---------------------------------------------------------------
    # 4. Fill local form — login as user1, POST /api/forms/{form_id}/submissions
    # ---------------------------------------------------------------
    user1 = USERS["user1"]
    user1_token = tokens.get("user1")
    if not user1_token:
        user1_token = session.login(user1["email"], user1["password"])
        if user1_token:
            tokens["user1"] = user1_token

    if user1_token and form_id:
        reporting.token = user1_token
        submission_payload = {
            "data": {
                "hw_costs": 15000,
                "sw_costs": 25000
            }
        }
        status, body = reporting.call("POST", f"/api/forms/{form_id}/submissions",
                                      body=submission_payload,
                                      expected_status=201,
                                      tag="fill-local-form")

    # ---------------------------------------------------------------
    # 5. Release data to holding — POST /api/forms/{form_id}/release
    # ---------------------------------------------------------------
    reporting.token = admin1_token
    if form_id:
        status, body = reporting.call("POST", f"/api/forms/{form_id}/release",
                                      expected_status=200,
                                      tag="release-to-holding")
        if status not in (200, 201):
            reporting.missing_feature(f"/api/forms/{form_id}/release",
                                      "Release local form data to holding")

    # ---------------------------------------------------------------
    # 6. Create local PPTX template — POST /api/templates/pptx
    # ---------------------------------------------------------------
    pptx_payload = {
        "scope": "LOCAL"
    }
    status, body = reporting.call("POST", "/api/templates/pptx",
                                  body=pptx_payload,
                                  expected_status=201,
                                  tag="create-local-pptx-template")
    if status in (404, 500):
        reporting.missing_feature("POST /api/templates/pptx", "Endpoint not implemented yet")
    elif status not in (200, 201):
        reporting.missing_feature("POST /api/templates/pptx",
                                  "Create local PPTX template")

    # ---------------------------------------------------------------
    # 7. Share form within holding — POST /api/forms/{form_id}/share
    # ---------------------------------------------------------------
    if form_id:
        share_payload = {
            "scope": "SHARED_WITHIN_HOLDING"
        }
        status, body = reporting.call("POST", f"/api/forms/{form_id}/share",
                                      body=share_payload,
                                      expected_status=200,
                                      tag="share-form-within-holding")
        if status not in (200, 201):
            reporting.missing_feature(f"/api/forms/{form_id}/share",
                                      "Share form within holding")

    # ---------------------------------------------------------------
    # Sync counters from sub-session
    # ---------------------------------------------------------------
    session.sync_counters_from(reporting)

    # ---------------------------------------------------------------
    # Save state
    # ---------------------------------------------------------------
    patch = {"tokens": tokens}
    if form_id:
        patch["local_form_id"] = form_id
    session.update_state(patch)

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
