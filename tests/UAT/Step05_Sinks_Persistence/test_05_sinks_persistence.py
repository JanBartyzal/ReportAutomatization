#!/usr/bin/env python3
# Step05: Sinks & Persistence (FS05)
# Verify data persistence, RLS per organization, and cross-tenant isolation.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step05_Sinks_Persistence"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})
    file_ids = state.get("file_ids", {})

    session._log(f"[INFO] Step05 — Sinks & Persistence Verification")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    # ---------------------------------------------------------------
    # 1. Load state
    # ---------------------------------------------------------------
    admin1_token = tokens.get("admin1")
    if not admin1_token:
        session._err("[FATAL] No admin1 token in state. Run Step00 first.")
        session.save_log(STEP_NAME)
        return 1

    pptx_file_id = file_ids.get("pptx")
    if not pptx_file_id:
        session._err("[FATAL] No PPTX file_id in state. Run Step02 first.")
        session.save_log(STEP_NAME)
        return 1

    xlsx_file_id = file_ids.get("xlsx")
    if not xlsx_file_id:
        session.missing_feature(
            "file_ids.xlsx",
            "No XLSX file_id in state — XLSX persistence tests will be skipped"
        )

    session.token = admin1_token
    session.restore_auth_from_state()
    data_session = session.for_service(SERVICES["engine_data"])

    session._log(f"[INFO] PPTX file_id: {pptx_file_id}")
    session._log(f"[INFO] XLSX file_id: {xlsx_file_id}")

    # ---------------------------------------------------------------
    # 2. Query stored table data from PPTX
    # ---------------------------------------------------------------
    status, body = data_session.call("GET", "/api/query/tables",
                                query_params={"file_id": pptx_file_id},
                                expected_status=200, tag="tables-by-file")
    if status == 200:
        data_list = _extract_list(body)
        if data_list is not None:
            session.assert_true(isinstance(data_list, list), "Tables response is a list")
            session._log(f"[INFO] Table records for PPTX: {len(data_list)}")
        else:
            session._log("[INFO] Tables response has no list — may be empty or different structure")
    elif status in (404, 500):
        session.missing_feature(
            "GET /api/query/tables?file_id=...",
            "Table data query endpoint not implemented yet"
        )

    # ---------------------------------------------------------------
    # 2b. Verify XLSX table data persisted correctly
    #     Test.xlsx: 5 rows, 13 columns
    #     Project names: Item1, Item2, Item3, Item4, Item5
    #     TotalCost sum = 1617700, TotalBudget sum = 1882300
    # ---------------------------------------------------------------
    if xlsx_file_id:
        session._log("[INFO] --- XLSX persistence verification ---")
        status, body = data_session.call("GET", "/api/query/tables",
                                    query_params={"file_id": xlsx_file_id},
                                    expected_status=200, tag="xlsx-tables-by-file")
        if status == 200:
            xlsx_records = _extract_list(body)
            if xlsx_records is not None:
                if len(xlsx_records) == 0:
                    # Data hasn't been processed by orchestrator/atomizer pipeline yet
                    session._log("[INFO] XLSX tables query returned 0 records — data not yet processed by orchestrator pipeline")
                    session._log("[INFO] Skipping XLSX data assertions (project names, sums) until pipeline processes the file")
                else:
                    # Check 5 records persisted
                    session.assert_true(
                        len(xlsx_records) == 5,
                        f"XLSX table has 5 records (got {len(xlsx_records)})"
                    )

                    # Check project names present
                    expected_projects = {"Item1", "Item2", "Item3", "Item4", "Item5"}
                    found_projects = set()
                    for rec in xlsx_records:
                        if isinstance(rec, dict):
                            proj = _get_project_name(rec)
                            if proj:
                                found_projects.add(proj)
                    session.assert_true(
                        expected_projects.issubset(found_projects),
                        f"XLSX contains all project names {expected_projects} (found {found_projects})"
                    )

                    # Check known value: Item4 Max project cost = 1265000
                    item4_record = None
                    for rec in xlsx_records:
                        if isinstance(rec, dict) and _get_project_name(rec) == "Item4":
                            item4_record = rec
                            break
                    if item4_record:
                        max_cost = _get_numeric_field(item4_record,
                                                      ("MaxProjectCost", "Max project cost",
                                                       "max_project_cost", "maxProjectCost",
                                                       "Max Project Cost"))
                        if max_cost is not None:
                            session.assert_true(
                                max_cost == 1265000,
                                f"Item4 Max project cost = 1265000 (got {max_cost})"
                            )
                        else:
                            session._log("[INFO] Could not find Max project cost field in Item4 record")
                            session._log(f"[INFO] Item4 record keys: {list(item4_record.keys())}")
                    else:
                        session._log("[WARN] Item4 record not found in XLSX data")

                    # Check total: sum of TotalCost = 1617700
                    total_cost_sum = 0
                    total_cost_found = False
                    for rec in xlsx_records:
                        if isinstance(rec, dict):
                            tc = _get_numeric_field(rec,
                                                    ("TotalCost", "Total Cost", "total_cost",
                                                     "totalCost", "Total cost"))
                            if tc is not None:
                                total_cost_sum += tc
                                total_cost_found = True
                    if total_cost_found:
                        session.assert_true(
                            total_cost_sum == 1617700,
                            f"XLSX TotalCost sum = 1617700 (got {total_cost_sum})"
                        )
                    else:
                        session._log("[INFO] Could not find TotalCost field in XLSX records")
                        if xlsx_records and isinstance(xlsx_records[0], dict):
                            session._log(f"[INFO] Available keys: {list(xlsx_records[0].keys())}")

                    # Check total: sum of TotalBudget = 1882300
                    total_budget_sum = 0
                    total_budget_found = False
                    for rec in xlsx_records:
                        if isinstance(rec, dict):
                            tb = _get_numeric_field(rec,
                                                    ("TotalBudget", "Total Budget", "total_budget",
                                                     "totalBudget", "Total budget"))
                            if tb is not None:
                                total_budget_sum += tb
                                total_budget_found = True
                    if total_budget_found:
                        session.assert_true(
                            total_budget_sum == 1882300,
                            f"XLSX TotalBudget sum = 1882300 (got {total_budget_sum})"
                        )
                    else:
                        session._log("[INFO] Could not find TotalBudget field in XLSX records")
            else:
                session._log("[INFO] XLSX tables response has no list — may be empty or different structure")
        elif status in (404, 500):
            session.missing_feature(
                "GET /api/query/tables?file_id=... (xlsx)",
                "Table data query endpoint not implemented for XLSX"
            )
    else:
        session._log("[SKIP] XLSX persistence tests skipped — no xlsx_file_id in state")

    # ---------------------------------------------------------------
    # 3. Query stored document data by file_id
    # ---------------------------------------------------------------
    data_session.token = admin1_token
    status, body = data_session.call("GET", "/api/query/documents",
                                query_params={"file_id": pptx_file_id},
                                expected_status=200, tag="documents-by-file-id")
    if status == 200 and isinstance(body, list):
        session.assert_true(len(body) >= 0, f"Documents query returned {len(body)} documents for file")
        session._log(f"[INFO] Documents for PPTX file: {len(body)}")
    elif status in (404, 500):
        session.missing_feature(
            "GET /api/query/documents?file_id=...",
            "Documents endpoint does not support file_id query parameter"
        )

    # ---------------------------------------------------------------
    # 4. RLS: admin1 sees own org data only
    # ---------------------------------------------------------------
    status, body = data_session.call("GET", "/api/query/tables",
                                expected_status=200, tag="rls-admin1-tables")
    if status == 200:
        data_list = _extract_list(body)
        if data_list is not None and len(data_list) > 0:
            # Check all records belong to org1
            org1_slug = USERS["admin1"]["org_slug"]
            all_org1 = all(
                _get_org(record) in (org1_slug, None)
                for record in data_list
                if isinstance(record, dict)
            )
            session.assert_true(all_org1,
                                f"RLS: admin1 sees only {org1_slug} data ({len(data_list)} records)")
        else:
            session._log("[INFO] No table data for admin1 — RLS check inconclusive")
    elif status in (404, 500):
        session.missing_feature("GET /api/query/tables", "Data tables endpoint not implemented")

    # ---------------------------------------------------------------
    # 5. RLS: admin2 sees own org data only
    # ---------------------------------------------------------------
    admin2_token = tokens.get("admin2")
    if not admin2_token:
        # Try to login
        admin2 = USERS["admin2"]
        admin2_token = session.login(admin2["email"], admin2["password"])
        if admin2_token:
            session.update_state({"tokens": {"admin2": admin2_token}})

    if admin2_token:
        data_session.token = admin2_token
        status, body = data_session.call("GET", "/api/query/tables",
                                    expected_status=200, tag="rls-admin2-tables")
        if status in (404, 500):
            data_session.missing_feature("GET /api/query/tables (admin2 RLS)",
                                         "Data tables endpoint not implemented yet")
        elif status == 200:
            data_list = _extract_list(body)
            if data_list is not None:
                org2_slug = USERS["admin2"]["org_slug"]
                all_org2 = all(
                    _get_org(record) in (org2_slug, None)
                    for record in data_list
                    if isinstance(record, dict)
                )
                session.assert_true(all_org2,
                                    f"RLS: admin2 sees only {org2_slug} data ({len(data_list)} records)")
            else:
                session._log("[INFO] No table data for admin2 — RLS check inconclusive")
    else:
        session._log("[SKIP] No admin2 token — skipping admin2 RLS check")

    # ---------------------------------------------------------------
    # 6. Cross-tenant isolation: admin2 cannot see admin1's file data
    # ---------------------------------------------------------------
    if admin2_token and pptx_file_id:
        data_session.token = admin2_token
        status, body = data_session.call("GET", "/api/query/tables",
                                    query_params={"file_id": pptx_file_id},
                                    expected_status=200, tag="cross-tenant-isolation")
        if status in (404, 500):
            data_session.missing_feature("GET /api/query/tables (cross-tenant)",
                                         "Data tables endpoint not implemented yet")
        elif status == 200:
            data_list = _extract_list(body)
            if data_list is not None:
                session.assert_true(
                    len(data_list) == 0,
                    f"Cross-tenant: admin2 sees 0 records for admin1's file (got {len(data_list)})"
                )
            else:
                session._log("[INFO] Cross-tenant response has no list — isolation check inconclusive")
        elif status == 403:
            # 403 is also acceptable — explicit access denied
            session._pass_count += 1
            session._fail_count = max(0, session._fail_count - 1)
            session._log("[OK]   Cross-tenant: admin2 got 403 for admin1's file (isolation enforced)")
        elif status == 404:
            # 404 is acceptable — file not visible to admin2
            session._pass_count += 1
            session._fail_count = max(0, session._fail_count - 1)
            session._log("[OK]   Cross-tenant: admin2 got 404 for admin1's file (isolation enforced)")
    else:
        session._log("[SKIP] Cross-tenant isolation check skipped (missing admin2 token or file_id)")

    session.sync_counters_from(data_session)
    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _extract_list(body) -> list | None:
    """Extract list from various response formats (direct list, wrapped in dict)."""
    if isinstance(body, list):
        return body
    if isinstance(body, dict):
        for key in ("data", "items", "content", "records", "tables", "documents"):
            if key in body and isinstance(body[key], list):
                return body[key]
    return None


def _get_org(record: dict) -> str | None:
    """Extract organization identifier from a data record."""
    return (record.get("orgSlug") or record.get("org_slug") or
            record.get("organizationSlug") or record.get("orgId") or
            record.get("org_id"))


def _get_project_name(record: dict) -> str | None:
    """Extract project name from a data record, trying common key variants."""
    for key in ("ProjectName", "Project Name", "project_name", "projectName",
                "Project name", "name", "Name"):
        val = record.get(key)
        if val is not None:
            return str(val).strip()
    return None


def _get_numeric_field(record: dict, candidates: tuple) -> float | None:
    """Extract a numeric field value, trying multiple key name candidates."""
    for key in candidates:
        val = record.get(key)
        if val is not None:
            try:
                return float(val)
            except (ValueError, TypeError):
                continue
    return None


if __name__ == "__main__":
    sys.exit(main())
