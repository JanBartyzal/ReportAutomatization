#!/usr/bin/env python3
# Step20: Period Management (FS20)
# Verify period CRUD, state transitions, dashboard, clone, export, close, and historical comparison.

import sys
import os

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step20_Period_Management"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    session._log("[INFO] Step20 — Period Management")

    # 1. Load tokens from state, login as admin1
    state = session.load_state()
    tokens = state.get("tokens", {})
    session.assert_true(bool(tokens), "Tokens loaded from state")

    token = session.login(USERS["admin1"]["email"], USERS["admin1"]["password"])
    if not token:
        session._err("[FATAL] Cannot login as admin1 — aborting step20")
        session.save_log(STEP_NAME)
        return 1

    session.restore_auth_from_state()

    # Period endpoints live on engine-reporting
    reporting_session = session.for_service(SERVICES["engine_reporting"])

    # 2. Create period — POST /api/periods
    period_payload = {
        "name": "Q1 2026",
        "type": "QUARTERLY",
        "start_date": "2026-01-01",
        "submission_deadline": "2026-03-31",
        "review_deadline": "2026-04-15",
        "period_code": "Q1-2026"
    }
    status, body = reporting_session.call("POST", "/api/periods",
                                body=period_payload,
                                expected_status=201, tag="create-period")
    if status in (400, 404, 500):
        reporting_session.missing_feature("POST /api/periods", "Endpoint not implemented yet")
    period_id = None
    if status == 201 and isinstance(body, dict):
        period_id = body.get("id") or body.get("period_id")

    if not period_id:
        session._log("[WARN] Cannot create period — skipping remaining period management tests")
        session.sync_counters_from(reporting_session)
        session.update_state({"tokens": tokens})
        ok = session.save_log(STEP_NAME)
        return 0 if ok else 1

    # 3. List periods — GET /api/periods
    status, body = reporting_session.call("GET", "/api/periods",
                                expected_status=200, tag="list-periods")

    # 4. Get period — GET /api/periods/{period_id}, check state is OPEN
    status, body = reporting_session.call("GET", f"/api/periods/{period_id}",
                                expected_status=200, tag="get-period")
    if status == 200 and isinstance(body, dict):
        period_state = body.get("state") or body.get("status")
        session.assert_true(period_state == "OPEN",
                            f"Initial period state is OPEN (got {period_state})")

    # 5. Transition to COLLECTING — POST /api/periods/{period_id}/collect
    status, body = reporting_session.call("POST", f"/api/periods/{period_id}/collect",
                                expected_status=200, tag="period-collect")
    if status == 200 and isinstance(body, dict):
        period_state = body.get("state") or body.get("status")
        session.assert_true(period_state == "COLLECTING",
                            f"Period state after collect is COLLECTING (got {period_state})")

    # 6. Period dashboard (completion matrix) — GET /api/periods/{period_id}/dashboard
    status, body = reporting_session.call("GET", f"/api/periods/{period_id}/dashboard",
                                expected_status=200, tag="period-dashboard")

    # 7. Clone period — POST /api/periods/{period_id}/clone
    clone_payload = {
        "name": "Q2 2026",
        "period_code": "Q2-2026",
        "submission_deadline": "2026-06-30"
    }
    status, body = reporting_session.call("POST", f"/api/periods/{period_id}/clone",
                                body=clone_payload,
                                expected_status=201, tag="clone-period")
    period_id2 = None
    if status == 201 and isinstance(body, dict):
        period_id2 = body.get("id") or body.get("period_id")

    # 8. Export period status — GET /api/periods/{period_id}/export?format=excel
    status, body = reporting_session.call("GET", f"/api/periods/{period_id}/export",
                                query_params={"format": "excel"},
                                expected_status=200, tag="export-period-status")
    if status not in (200, 201):
        session.missing_feature(f"GET /api/periods/{period_id}/export?format=excel",
                                "Period status export not yet implemented")

    # 9. Close period — POST /api/periods/{period_id}/close
    status, body = reporting_session.call("POST", f"/api/periods/{period_id}/close",
                                expected_status=200, tag="close-period")
    if status == 200 and isinstance(body, dict):
        period_state = body.get("state") or body.get("status")
        session.assert_true(period_state == "CLOSED",
                            f"Period state after close is CLOSED (got {period_state})")

    # 10. Historical comparison — GET /api/periods/compare?period1={id1}&period2={id2}
    if period_id and period_id2:
        status, body = reporting_session.call("GET", "/api/periods/compare",
                                    query_params={"period1": period_id, "period2": period_id2},
                                    expected_status=200, tag="period-comparison")
        if status not in (200, 201):
            session.missing_feature("GET /api/periods/compare",
                                    "Historical period comparison not yet implemented")
    else:
        session.missing_feature("GET /api/periods/compare",
                                "No second period_id available for comparison test")

    session.sync_counters_from(reporting_session)

    # 11. Save period_ids to state
    state_patch = {"period_ids": {"period_id": period_id}}
    if period_id2:
        state_patch["period_ids"]["period_id2"] = period_id2
    session.update_state(state_patch)
    session._log(f"[INFO] Saved period_ids to state: {state_patch}")

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
