#!/usr/bin/env python3
# Step22: Period Comparison (FS22)
# Verify period-over-period comparison, KPI comparison, multi-org comparison, and PPTX export.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step22_Period_Comparison"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})

    session._log(f"[INFO] Step22 — Period Comparison Verification")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    # ---------------------------------------------------------------
    # 1. Load tokens and period_ids from state
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

    # Create sub-sessions for engine-reporting (periods) and engine-data (KPIs)
    reporting = session.for_service(SERVICES["engine_reporting"])
    data_session = session.for_service(SERVICES["engine_data"])

    period_ids = state.get("period_ids", [])
    period1 = period_ids[0] if len(period_ids) > 0 else "period-1"
    period2 = period_ids[1] if len(period_ids) > 1 else "period-2"
    session._log(f"[INFO] Using period IDs: {period1}, {period2}")

    # ---------------------------------------------------------------
    # 2. Basic period comparison — POST /api/periods/compare (engine-reporting 8105)
    # ---------------------------------------------------------------
    status, body = reporting.call("POST", "/api/periods/compare",
                                   body={"period1": period1, "period2": period2},
                                   expected_status=200,
                                   tag="basic-period-comparison")
    if status not in (200,):
        reporting.missing_feature("POST /api/periods/compare",
                                  "Basic period-over-period comparison")

    # ---------------------------------------------------------------
    # 3. KPI comparison — POST /api/comparisons/kpis (engine-data 8100)
    # ---------------------------------------------------------------
    status, body = data_session.call("POST", "/api/comparisons/kpis",
                                  body={"metric": "amount_czk", "period1": period1, "period2": period2},
                                  expected_status=200,
                                  tag="kpi-comparison-post")
    if status not in (200, 201):
        data_session.missing_feature("POST /api/comparisons/kpis",
                                  "KPI comparison between periods")

    # 3b. GET KPI comparisons
    status, body = data_session.call("GET", "/api/comparisons/kpis",
                                  expected_status=200,
                                  tag="kpi-comparison-get")
    if status not in (200,):
        data_session.missing_feature("GET /api/comparisons/kpis",
                                  "KPI comparison listing")

    # ---------------------------------------------------------------
    # 4. Multi-org comparison — POST /api/comparisons/multi-org (engine-data 8100)
    # ---------------------------------------------------------------
    status, body = data_session.call("POST", "/api/comparisons/multi-org",
                                  body={"period_id": period1, "metric": "total_costs"},
                                  expected_status=200,
                                  tag="multi-org-comparison")
    if status not in (200, 201):
        data_session.missing_feature("POST /api/comparisons/multi-org",
                                  "Multi-org comparison across organizations")

    # ---------------------------------------------------------------
    # 5. Export comparison as PPTX — POST /api/periods/compare/export (engine-reporting)
    # ---------------------------------------------------------------
    status, body = reporting.call("POST", "/api/periods/compare/export",
                                  query_params={"format": "pptx"},
                                  expected_status=200,
                                  tag="export-comparison-pptx")
    if status not in (200, 201):
        reporting.missing_feature("POST /api/periods/compare/export",
                                  "Export period comparison as PPTX")

    # ---------------------------------------------------------------
    # 6. Note: FS22 is largely placeholder — most tests use missing_feature()
    # ---------------------------------------------------------------
    session._log("[INFO] FS22 is largely placeholder; most endpoints expected to be unimplemented.")

    # ---------------------------------------------------------------
    # Sync counters from sub-sessions
    # ---------------------------------------------------------------
    session.sync_counters_from(reporting)
    session.sync_counters_from(data_session)

    # ---------------------------------------------------------------
    # Save state
    # ---------------------------------------------------------------
    session.update_state({"tokens": tokens})

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
