#!/usr/bin/env python3
# Step23: ServiceNow Integration (FS23)
# Verify ServiceNow integration setup, connection test, sync scheduling, and report distribution.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step23_ServiceNow_Integration"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})

    session._log(f"[INFO] Step23 — ServiceNow Integration Verification")
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

    # Create sub-session for engine-integrations (ServiceNow)
    integrations = session.for_service(SERVICES["engine_integrations"])

    # ---------------------------------------------------------------
    # 2. List ServiceNow integrations — GET /api/admin/integrations/servicenow
    # ---------------------------------------------------------------
    status, body = integrations.call("GET", "/api/admin/integrations/servicenow",
                                      expected_status=200,
                                      tag="list-servicenow-integrations")
    if status in (403, 404, 500):
        integrations.missing_feature("GET /api/admin/integrations/servicenow", "Endpoint not implemented yet")

    # ---------------------------------------------------------------
    # 3. Configure ServiceNow connection — POST /api/admin/integrations/servicenow
    # ---------------------------------------------------------------
    snow_payload = {
        "instance_url": "https://test.service-now.com",
        "auth_type": "oauth2"
    }
    conn_id = None
    status, body = integrations.call("POST", "/api/admin/integrations/servicenow",
                                     body=snow_payload,
                                     expected_status=201,
                                     tag="configure-servicenow")
    if status in (200, 201) and isinstance(body, dict):
        conn_id = body.get("id") or body.get("connection_id") or body.get("connId")
        session._log(f"[INFO] ServiceNow connection ID: {conn_id}")
    elif status in (404, 500):
        integrations.missing_feature("POST /api/admin/integrations/servicenow", "Endpoint not implemented yet")
    elif status not in (200, 201):
        integrations.missing_feature("POST /api/admin/integrations/servicenow",
                                     "Configure ServiceNow connection")

    # ---------------------------------------------------------------
    # 4. Test connection — POST /api/admin/integrations/servicenow/test
    # ---------------------------------------------------------------
    status, body = integrations.call("POST", "/api/admin/integrations/servicenow/test",
                                     expected_status=200,
                                     tag="test-servicenow-connection")
    if status not in (200,):
        integrations.missing_feature("POST /api/admin/integrations/servicenow/test",
                                     "Test ServiceNow connection")

    # ---------------------------------------------------------------
    # 5. Trigger manual sync — POST /api/admin/integrations/servicenow/{id}/sync
    # ---------------------------------------------------------------
    if conn_id:
        status, body = integrations.call("POST", f"/api/admin/integrations/servicenow/{conn_id}/sync",
                                         expected_status=202,
                                         tag="trigger-servicenow-sync")
        if status not in (200, 202):
            integrations.missing_feature(f"POST /api/admin/integrations/servicenow/{conn_id}/sync",
                                         "Trigger manual ServiceNow sync")
    else:
        integrations.missing_feature("POST /api/admin/integrations/servicenow/{id}/sync",
                                     "No connection ID available to test sync")

    # ---------------------------------------------------------------
    # 6. Schedule sync — POST /api/admin/integrations/servicenow/{connId}/schedules
    # ---------------------------------------------------------------
    schedule_payload = {
        "interval": "daily"
    }
    if conn_id:
        status, body = integrations.call("POST", f"/api/admin/integrations/servicenow/{conn_id}/schedules",
                                         body=schedule_payload,
                                         expected_status=201,
                                         tag="schedule-servicenow-sync")
        if status not in (200, 201):
            integrations.missing_feature(f"POST /api/admin/integrations/servicenow/{conn_id}/schedules",
                                         "Schedule ServiceNow sync")
    else:
        integrations.missing_feature("POST /api/admin/integrations/servicenow/{connId}/schedules",
                                     "No connection ID available to test scheduling")

    # ---------------------------------------------------------------
    # 7. Report distribution — POST /api/admin/integrations/servicenow/{connId}/distributions
    # ---------------------------------------------------------------
    distribution_payload = {
        "recipients": ["test@example.com"],
        "format": "excel"
    }
    if conn_id:
        status, body = integrations.call("POST", f"/api/admin/integrations/servicenow/{conn_id}/distributions",
                                         body=distribution_payload,
                                         expected_status=201,
                                         tag="setup-report-distribution")
        if status not in (200, 201):
            integrations.missing_feature(f"POST /api/admin/integrations/servicenow/{conn_id}/distributions",
                                         "Report distribution via ServiceNow")
    else:
        integrations.missing_feature("POST /api/admin/integrations/servicenow/{connId}/distributions",
                                     "No connection ID available to test distribution")

    # ---------------------------------------------------------------
    # Sync counters from sub-session
    # ---------------------------------------------------------------
    session.sync_counters_from(integrations)

    # ---------------------------------------------------------------
    # Save state
    # ---------------------------------------------------------------
    session.update_state({"tokens": tokens})

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
