#!/usr/bin/env python3
# Step11: Dashboards & SQL (FS11)
# Verify dashboard CRUD, widget creation with concrete Test.xlsx data,
# public/private visibility rules, dashboard data endpoint, and SQL queries.
#
# Test.xlsx reference data (5 projects: Item1..Item5):
#   Project Name, TotalCost, ToalBudget, DiffCost,
#   Cost24, Cost25, Cost26, Budget24, Budget25, Budget26
#
# Known values:
#   Item4 TotalCost=1342500 (highest)
#   Item4 Cost24=365000, Cost25=485000, Cost26=492500
#   SUM(TotalCost) = 1617700

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step11_Dashboards_SQL"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})

    session._log(f"[INFO] Step11 — Dashboards & SQL Verification")
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

    # All dashboard and query endpoints live on engine-data
    data_session = session.for_service(SERVICES["engine_data"])

    # ---------------------------------------------------------------
    # 2. Create dashboard — POST /api/dashboards
    # ---------------------------------------------------------------
    dashboard_payload = {
        "name": "UAT Dashboard",
        "is_public": False
    }
    status, body = data_session.call("POST", "/api/dashboards",
                                body=dashboard_payload,
                                expected_status=201,
                                tag="create-dashboard")
    if status in (403, 404, 500):
        data_session.missing_feature("POST /api/dashboards",
                                     "Endpoint not implemented yet or auth headers not forwarded to engine-data")

    dashboard_id = None
    if status == 201 and isinstance(body, dict):
        dashboard_id = body.get("id") or body.get("dashboard_id")
        session.assert_true(dashboard_id is not None, "Dashboard ID returned on create")

    # ---------------------------------------------------------------
    # 3. List dashboards — GET /api/dashboards
    # ---------------------------------------------------------------
    status, body = data_session.call("GET", "/api/dashboards",
                                expected_status=200,
                                tag="list-dashboards")
    if status in (403, 404, 500):
        data_session.missing_feature("GET /api/dashboards",
                                     "Endpoint not implemented yet or auth headers not forwarded to engine-data")

    if status == 200 and isinstance(body, (list, dict)):
        items = body if isinstance(body, list) else body.get("items", body.get("data", []))
        if dashboard_id:
            found = any(
                (d.get("id") == dashboard_id or d.get("dashboard_id") == dashboard_id)
                for d in items
            )
            session.assert_true(found, f"Created dashboard {dashboard_id} found in list")

    # ---------------------------------------------------------------
    # 4. Get dashboard — GET /api/dashboards/{id}
    # ---------------------------------------------------------------
    if dashboard_id:
        status, body = data_session.call("GET", f"/api/dashboards/{dashboard_id}",
                                    expected_status=200,
                                    tag="get-dashboard")
        if status == 200 and isinstance(body, dict):
            session.assert_field(body, "name", "UAT Dashboard", label="dashboard name")

    # ---------------------------------------------------------------
    # 5. Add widgets to dashboard (3 concrete widgets based on Test.xlsx)
    # ---------------------------------------------------------------
    widget_ids = []

    if dashboard_id:
        # 5a. Bar chart — Cost vs Budget per project
        bar_chart_payload = {
            "type": "bar_chart",
            "title": "Cost vs Budget by Project",
            "query": 'SELECT "Project Name", "TotalCost", "ToalBudget" FROM xlsx_data ORDER BY "TotalCost" DESC',
            "config": {"group_by": "Project Name", "metrics": ["TotalCost", "ToalBudget"]}
        }
        status, body = data_session.call("POST", f"/api/dashboards/{dashboard_id}/widgets",
                                    body=bar_chart_payload,
                                    expected_status=201,
                                    tag="add-widget-bar-chart")
        if status == 201 and isinstance(body, dict):
            wid = body.get("id") or body.get("widget_id")
            if wid:
                widget_ids.append(wid)
            session.assert_true(
                body.get("type") == "bar_chart" or body.get("title") == "Cost vs Budget by Project" or wid is not None,
                "Bar chart widget created successfully"
            )

        # 5b. Table widget — full data view
        table_payload = {
            "type": "table",
            "title": "Project Overview",
            "query": 'SELECT "Project Name", "Max project cost", "TotalCost", "ToalBudget", "DiffCost" FROM xlsx_data',
            "config": {"columns": ["Project Name", "Max project cost", "TotalCost", "ToalBudget", "DiffCost"]}
        }
        status, body = data_session.call("POST", f"/api/dashboards/{dashboard_id}/widgets",
                                    body=table_payload,
                                    expected_status=201,
                                    tag="add-widget-table")
        if status == 201 and isinstance(body, dict):
            wid = body.get("id") or body.get("widget_id")
            if wid:
                widget_ids.append(wid)
            session.assert_true(
                body.get("type") == "table" or body.get("title") == "Project Overview" or wid is not None,
                "Table widget created successfully"
            )

        # 5c. Line chart — Cost trend per year
        line_chart_payload = {
            "type": "line_chart",
            "title": "Cost Trend 2024-2026",
            "query": 'SELECT "Project Name", "Cost24", "Cost25", "Cost26" FROM xlsx_data',
            "config": {"x_axis": "Project Name", "series": ["Cost24", "Cost25", "Cost26"]}
        }
        status, body = data_session.call("POST", f"/api/dashboards/{dashboard_id}/widgets",
                                    body=line_chart_payload,
                                    expected_status=201,
                                    tag="add-widget-line-chart")
        if status == 201 and isinstance(body, dict):
            wid = body.get("id") or body.get("widget_id")
            if wid:
                widget_ids.append(wid)
            session.assert_true(
                body.get("type") == "line_chart" or body.get("title") == "Cost Trend 2024-2026" or wid is not None,
                "Line chart widget created successfully"
            )

        session._log(f"[INFO] Created {len(widget_ids)} widgets: {widget_ids}")

    # ---------------------------------------------------------------
    # 6. Public vs private: user2 (viewer) should NOT see non-public dashboard
    # ---------------------------------------------------------------
    user2 = USERS["user2"]
    user2_token = tokens.get("user2")
    if not user2_token:
        user2_token = session.login(user2["email"], user2["password"])
        if user2_token:
            tokens["user2"] = user2_token

    if user2_token and dashboard_id:
        session.token = user2_token
        data_session.token = user2_token
        data_session.roles = session.roles
        status, body = data_session.call("GET", "/api/dashboards",
                                    expected_status=200,
                                    tag="viewer-list-dashboards-private")
        if status in (403, 404, 500):
            data_session.missing_feature("GET /api/dashboards (viewer/private)",
                                         "Auth headers not propagated for user2 token on engine-data")
        if status == 200 and isinstance(body, (list, dict)):
            items = body if isinstance(body, list) else body.get("items", body.get("data", []))
            not_found = not any(
                (d.get("id") == dashboard_id or d.get("dashboard_id") == dashboard_id)
                for d in items
            )
            session.assert_true(not_found, "Viewer cannot see non-public dashboard")

    # ---------------------------------------------------------------
    # 7. Make dashboard public — PUT /api/dashboards/{id}
    # ---------------------------------------------------------------
    # Restore admin1 auth fully (token + roles + org context)
    session.token = admin1_token
    session.restore_auth_from_state()
    data_session.token = admin1_token
    data_session.org_id = session.org_id
    data_session.user_id = session.user_id
    data_session.roles = session.roles
    if dashboard_id:
        status, body = data_session.call("PUT", f"/api/dashboards/{dashboard_id}",
                                    body={"is_public": True},
                                    expected_status=200,
                                    tag="make-dashboard-public")

    # ---------------------------------------------------------------
    # 8. Viewer can now see public dashboard
    # ---------------------------------------------------------------
    if user2_token and dashboard_id:
        session.token = user2_token
        data_session.token = user2_token
        data_session.roles = session.roles
        status, body = data_session.call("GET", "/api/dashboards",
                                    expected_status=200,
                                    tag="viewer-list-dashboards-public")
        if status in (403, 404, 500):
            data_session.missing_feature("GET /api/dashboards (viewer/public)",
                                         "Auth headers not propagated for user2 token on engine-data")
        if status == 200 and isinstance(body, (list, dict)):
            items = body if isinstance(body, list) else body.get("items", body.get("data", []))
            found = any(
                (d.get("id") == dashboard_id or d.get("dashboard_id") == dashboard_id)
                for d in items
            )
            session.assert_true(found, "Viewer can see public dashboard")

    # ---------------------------------------------------------------
    # 9. Dashboard data endpoint — GET /api/dashboards/{id}/data
    #    Verify it returns data based on the widgets' queries (5 project rows).
    # ---------------------------------------------------------------
    session.token = admin1_token
    session.restore_auth_from_state()
    data_session.token = admin1_token
    data_session.org_id = session.org_id
    data_session.user_id = session.user_id
    data_session.roles = session.roles
    dashboard_data = None

    if dashboard_id:
        status, body = data_session.call("GET", f"/api/dashboards/{dashboard_id}/data",
                                    expected_status=200,
                                    tag="dashboard-data")

        if status == 200 and isinstance(body, dict):
            dashboard_data = body
            # The response may contain widget data keyed by widget id or as a list
            widgets_data = body.get("widgets", body.get("data", body.get("items", [])))
            if isinstance(widgets_data, list):
                session.assert_true(len(widgets_data) > 0, "Dashboard data contains widget results")
                # Check that at least one widget result has 5 rows (one per project)
                has_five_rows = False
                for wd in widgets_data:
                    rows = wd.get("rows", wd.get("data", wd.get("results", [])))
                    if isinstance(rows, list) and len(rows) == 5:
                        has_five_rows = True
                        break
                session.assert_true(has_five_rows, "Dashboard data includes 5 project rows")
            elif isinstance(widgets_data, dict):
                # Data keyed by widget id
                session.assert_true(len(widgets_data) > 0, "Dashboard data contains widget results (dict)")
                for wkey, wval in widgets_data.items():
                    rows = wval.get("rows", wval.get("data", wval.get("results", []))) if isinstance(wval, dict) else wval
                    if isinstance(rows, list) and len(rows) == 5:
                        session._log(f"[INFO] Widget {wkey} returned 5 rows as expected")
                        break
        elif status in (404, 501):
            session._log("[WARN] Dashboard data endpoint not implemented (missing_feature)")
        else:
            session._log(f"[WARN] Dashboard data endpoint returned status={status}")

    # ---------------------------------------------------------------
    # 10. Chart data validation — verify concrete values from Test.xlsx
    #     - Bar chart: Item4 TotalCost=1342500 (highest)
    #     - Table: 5 rows
    #     - Line chart: Item4 Cost24=365000, Cost25=485000, Cost26=492500
    # ---------------------------------------------------------------
    if dashboard_data and isinstance(dashboard_data, dict):
        widgets_data = dashboard_data.get("widgets", dashboard_data.get("data", dashboard_data.get("items", [])))

        def _find_rows_in_widget_data(wd_entry):
            """Extract rows from a widget data entry."""
            if isinstance(wd_entry, dict):
                return wd_entry.get("rows", wd_entry.get("data", wd_entry.get("results", [])))
            return []

        def _find_project_row(rows, project_name):
            """Find a row matching project_name in a list of row dicts."""
            if not isinstance(rows, list):
                return None
            for r in rows:
                if isinstance(r, dict):
                    name = r.get("Project Name", r.get("project_name", r.get("name", "")))
                    if name == project_name:
                        return r
            return None

        # Iterate over widget data entries to validate
        all_widget_entries = []
        if isinstance(widgets_data, list):
            all_widget_entries = widgets_data
        elif isinstance(widgets_data, dict):
            all_widget_entries = list(widgets_data.values())

        bar_validated = False
        table_validated = False
        line_validated = False

        for wd in all_widget_entries:
            if not isinstance(wd, dict):
                continue
            wtype = wd.get("type", wd.get("widget_type", ""))
            wtitle = wd.get("title", wd.get("widget_title", ""))
            rows = _find_rows_in_widget_data(wd)

            # Bar chart validation — Item4 has TotalCost=1342500
            if wtype == "bar_chart" or "Cost vs Budget" in str(wtitle):
                item4 = _find_project_row(rows, "Item4")
                if item4:
                    total_cost = item4.get("TotalCost", item4.get("totalcost", item4.get("total_cost")))
                    if total_cost is not None:
                        session.assert_true(
                            float(total_cost) == 1342500.0,
                            f"Bar chart: Item4 TotalCost={total_cost} == 1342500"
                        )
                        bar_validated = True

            # Table validation — should have 5 rows
            if wtype == "table" or "Project Overview" in str(wtitle):
                if isinstance(rows, list):
                    session.assert_true(len(rows) == 5, f"Table widget has 5 rows (got {len(rows)})")
                    table_validated = True

            # Line chart validation — Item4 Cost24=365000, Cost25=485000, Cost26=492500
            if wtype == "line_chart" or "Cost Trend" in str(wtitle):
                item4 = _find_project_row(rows, "Item4")
                if item4:
                    cost24 = item4.get("Cost24", item4.get("cost24"))
                    cost25 = item4.get("Cost25", item4.get("cost25"))
                    cost26 = item4.get("Cost26", item4.get("cost26"))
                    if cost24 is not None:
                        session.assert_true(
                            float(cost24) == 365000.0,
                            f"Line chart: Item4 Cost24={cost24} == 365000"
                        )
                    if cost25 is not None:
                        session.assert_true(
                            float(cost25) == 485000.0,
                            f"Line chart: Item4 Cost25={cost25} == 485000"
                        )
                    if cost26 is not None:
                        session.assert_true(
                            float(cost26) == 492500.0,
                            f"Line chart: Item4 Cost26={cost26} == 492500"
                        )
                    line_validated = True

        if not bar_validated:
            session._log("[WARN] Bar chart data validation skipped — data not available or format unrecognized")
        if not table_validated:
            session._log("[WARN] Table data validation skipped — data not available or format unrecognized")
        if not line_validated:
            session._log("[WARN] Line chart data validation skipped — data not available or format unrecognized")
    else:
        session._log("[INFO] Skipping chart data validation — dashboard data not available")

    # ---------------------------------------------------------------
    # 11. Dashboard data endpoint — POST /api/dashboards/{id}/data
    #     No direct SQL query endpoint exists; use dashboard data instead.
    #     SUM(TotalCost) should equal 1617700
    # ---------------------------------------------------------------
    session.token = admin1_token
    data_session.token = admin1_token

    if dashboard_id:
        # DashboardDataRequest requires groupBy, aggregation, valueField
        sql_payload = {
            "groupBy": ["Project Name"],
            "aggregation": "SUM",
            "valueField": "TotalCost",
            "sourceType": "FILE"
        }
        status, body = data_session.call("POST", f"/api/dashboards/{dashboard_id}/data",
                                    body=sql_payload,
                                    expected_status=200,
                                    tag="dashboard-data-query")
        if status in (404, 500):
            data_session.missing_feature("POST /api/dashboards/{id}/data", "Dashboard data query endpoint not implemented yet")

        if status == 200 and isinstance(body, dict):
            # Response may be: {"rows": [{"total": 1617700}]} or {"data": [...]} or {"result": {...}}
            rows = body.get("rows", body.get("data", body.get("results", [])))
            result_value = None

            if isinstance(rows, list) and len(rows) > 0:
                first_row = rows[0]
                if isinstance(first_row, dict):
                    result_value = first_row.get("total", first_row.get("TOTAL", first_row.get("sum")))
            elif isinstance(body.get("result"), dict):
                result_value = body["result"].get("total", body["result"].get("TOTAL"))
            elif "total" in body:
                result_value = body["total"]

            if result_value is not None:
                session.assert_true(
                    float(result_value) == 1617700.0,
                    f"SQL SUM(TotalCost) = {result_value} == 1617700"
                )
            else:
                session._log("[WARN] Dashboard data query returned 200 but could not extract total value from response")
        elif status in (404, 501):
            session._log("[WARN] Dashboard data query endpoint not implemented (missing_feature)")
        else:
            session._log(f"[WARN] Dashboard data query endpoint returned status={status}")
    else:
        data_session.missing_feature("POST /api/dashboards/{id}/data", "No dashboard_id available to test data query")

    # ---------------------------------------------------------------
    # 12. Save dashboard_id and widget_ids to state
    # ---------------------------------------------------------------
    session.sync_counters_from(data_session)
    patch = {"tokens": tokens}
    if dashboard_id:
        patch["dashboard_id"] = dashboard_id
    if widget_ids:
        patch["widget_ids"] = widget_ids
    session.update_state(patch)

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
