#!/usr/bin/env python3
# Step17: Report Lifecycle (FS17)
# Verify report creation, state transitions (DRAFT→SUBMITTED→UNDER_REVIEW→APPROVED/REJECTED),
# checklist, data locking, rejection flow, and history tracking.

import sys
import os

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step17_Report_Lifecycle"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    session._log("[INFO] Step17 — Report Lifecycle")

    # 1. Load tokens from state, login as admin1
    state = session.load_state()
    tokens = state.get("tokens", {})
    org_ids = state.get("org_ids", {})
    session.assert_true(bool(tokens), "Tokens loaded from state")

    token = session.login(USERS["admin1"]["email"], USERS["admin1"]["password"])
    if not token:
        session._err("[FATAL] Cannot login as admin1 — aborting step17")
        session.save_log(STEP_NAME)
        return 1

    session.restore_auth_from_state()

    # Report endpoints live on engine-reporting
    reporting_session = session.for_service(SERVICES["engine_reporting"])

    org_id = session.org_id or org_ids.get(USERS["admin1"]["org_slug"])
    period_id = state.get("period_id") or state.get("period_ids", {}).get("default")

    # 2. Create report — POST /api/reports
    report_payload = {"org_id": org_id, "report_type": "OPEX"}
    if period_id:
        report_payload["period_id"] = period_id
    status, body = reporting_session.call("POST", "/api/reports",
                                body=report_payload,
                                expected_status=201, tag="create-report")
    if status in (400, 404, 500):
        reporting_session.missing_feature("POST /api/reports", "Endpoint not implemented yet")
    report_id = None
    if status == 201 and isinstance(body, dict):
        report_id = body.get("id") or body.get("report_id")

    if not report_id:
        session._log("[WARN] Cannot create report — skipping remaining lifecycle tests")
        session.sync_counters_from(reporting_session)
        session.update_state({"tokens": tokens})
        ok = session.save_log(STEP_NAME)
        return 0 if ok else 1

    # 3. Check initial state — GET /api/reports/{report_id} → status should be DRAFT
    status, body = reporting_session.call("GET", f"/api/reports/{report_id}",
                                expected_status=200, tag="get-report-initial")
    if status == 200 and isinstance(body, dict):
        report_status = body.get("status") or body.get("state")
        session.assert_true(report_status == "DRAFT",
                            f"Initial report status is DRAFT (got {report_status})")

    # 4. Submit report (as editor/user1) — login as user1
    token_user1 = session.login(USERS["user1"]["email"], USERS["user1"]["password"])
    if token_user1:
        reporting_session.token = token_user1
        status, body = reporting_session.call("POST", f"/api/reports/{report_id}/submit",
                                    expected_status=200, tag="submit-report")
        if status == 200 and isinstance(body, dict):
            report_status = body.get("status") or body.get("state")
            session.assert_true(report_status == "SUBMITTED",
                                f"Report status after submit is SUBMITTED (got {report_status})")
    else:
        session._err("[WARN] Cannot login as user1, skipping submit")

    # 5. Check checklist — GET /api/reports/{report_id}/checklist
    status, body = reporting_session.call("GET", f"/api/reports/{report_id}/checklist",
                                expected_status=200, tag="report-checklist")
    if status not in (200, 201):
        session.missing_feature(f"GET /api/reports/{report_id}/checklist",
                                "Report checklist not yet implemented")

    # 6. Review report (as admin1/HoldingAdmin)
    token = session.login(USERS["admin1"]["email"], USERS["admin1"]["password"])
    if token:
        reporting_session.token = token
        status, body = reporting_session.call("POST", f"/api/reports/{report_id}/review",
                                    expected_status=200, tag="review-report")
        if status == 200 and isinstance(body, dict):
            report_status = body.get("status") or body.get("state")
            session.assert_true(report_status == "UNDER_REVIEW",
                                f"Report status after review is UNDER_REVIEW (got {report_status})")

    # 7. Approve report — POST /api/reports/{report_id}/approve
    status, body = reporting_session.call("POST", f"/api/reports/{report_id}/approve",
                                expected_status=200, tag="approve-report")
    if status == 200 and isinstance(body, dict):
        report_status = body.get("status") or body.get("state")
        session.assert_true(report_status == "APPROVED",
                            f"Report status after approval is APPROVED (got {report_status})")

    # 8. Verify data locked after approval — attempt to edit → should fail or create new version
    status, body = reporting_session.call("PUT", f"/api/reports/{report_id}",
                                body={"report_type": "CAPEX"},
                                expected_status=409, tag="edit-after-approval")
    if status not in (409, 403, 422):
        session._log(f"[WARN] Edit after approval returned {status} — expected 409/403/422")

    # 9. Reject flow: create another report, submit, reject with comment
    report_payload2 = {"org_id": org_id, "report_type": "OPEX"}
    if period_id:
        report_payload2["period_id"] = period_id
    status, body = reporting_session.call("POST", "/api/reports",
                                body=report_payload2,
                                expected_status=201, tag="create-report-2")
    report_id2 = None
    if status == 201 and isinstance(body, dict):
        report_id2 = body.get("id") or body.get("report_id")

    if report_id2:
        # Submit as user1
        token_user1 = session.login(USERS["user1"]["email"], USERS["user1"]["password"])
        if token_user1:
            reporting_session.token = token_user1
            reporting_session.call("POST", f"/api/reports/{report_id2}/submit",
                         expected_status=200, tag="submit-report-2")

        # Reject as admin1
        token = session.login(USERS["admin1"]["email"], USERS["admin1"]["password"])
        if token:
            reporting_session.token = token
            status, body = reporting_session.call("POST", f"/api/reports/{report_id2}/reject",
                                        body={"comment": "Data incomplete"},
                                        expected_status=200, tag="reject-report")
            if status == 200 and isinstance(body, dict):
                report_status = body.get("status") or body.get("state")
                session.assert_true(report_status in ("REJECTED", "DRAFT"),
                                    f"Report status after rejection is REJECTED or DRAFT (got {report_status})")

    # 10. Check state transitions in audit/history — GET /api/reports/{report_id}/history
    status, body = reporting_session.call("GET", f"/api/reports/{report_id}/history",
                                expected_status=200, tag="report-history")

    session.sync_counters_from(reporting_session)

    # 11. Save report_ids to state
    report_ids_patch = {"report_ids": {"report_id": report_id}}
    if report_id2:
        report_ids_patch["report_ids"]["report_id2"] = report_id2
    session.update_state(report_ids_patch)
    session._log(f"[INFO] Saved report_ids to state: {report_ids_patch}")

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
