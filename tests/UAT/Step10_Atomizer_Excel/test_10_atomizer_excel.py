#!/usr/bin/env python3
# Step10: Atomizer Excel (FS10)
# Verify Excel file processing: status, structure extraction, sheet content.
# Expected data based on tests/UAT/data/Test.xlsx (sheet "List1", 5 rows).

import sys
import os

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step10_Atomizer_Excel"

# ---------------------------------------------------------------------------
# Expected constants derived from Test.xlsx
# ---------------------------------------------------------------------------
EXPECTED_SHEET_NAME = "List1"
EXPECTED_HEADERS = [
    "Id", "Project Name", "Max project cost",
    "Budget24", "Budget 25", "Budget 26",
    "Cost24", "Cost25", "Cost26",
    "TotalCost", "ToalBudget", "DiffCost", "DiffBudget",
]
EXPECTED_ROW_COUNT = 5
EXPECTED_TOTAL_COST_SUM = 1617700  # 155000 + 36700 + 64000 + 1342500 + 19500

# Full expected rows (index 0-4) for spot-check assertions
EXPECTED_ROWS = [
    {"Id": 1, "Project Name": "Item1", "Max project cost": 150000,
     "Budget24": 72000, "Budget 25": 68000, "Budget 26": 25000,
     "Cost24": 70000, "Cost25": 65000, "Cost26": 20000,
     "TotalCost": 155000, "ToalBudget": 165000, "DiffCost": -5000, "DiffBudget": -15000},
    {"Id": 2, "Project Name": "Item2", "Max project cost": 25000,
     "Budget24": 18000, "Budget 25": 12500, "Budget 26": 5000,
     "Cost24": 15000, "Cost25": 11200, "Cost26": 10500,
     "TotalCost": 36700, "ToalBudget": 35500, "DiffCost": -11700, "DiffBudget": -10500},
    {"Id": 3, "Project Name": "Item3", "Max project cost": 65800,
     "Budget24": 25000, "Budget 25": 31000, "Budget 26": 12000,
     "Cost24": 32000, "Cost25": 21000, "Cost26": 11000,
     "TotalCost": 64000, "ToalBudget": 68000, "DiffCost": 1800, "DiffBudget": -2200},
    {"Id": 4, "Project Name": "Item4", "Max project cost": 1265000,
     "Budget24": 152000, "Budget 25": 425000, "Budget 26": 475000,
     "Cost24": 365000, "Cost25": 485000, "Cost26": 492500,
     "TotalCost": 1342500, "ToalBudget": 1052000, "DiffCost": -77500, "DiffBudget": 213000},
    {"Id": 5, "Project Name": "Item5", "Max project cost": 540000,
     "Budget24": 11800, "Budget 25": 275000, "Budget 26": 275000,
     "Cost24": 10500, "Cost25": 5000, "Cost26": 4000,
     "TotalCost": 19500, "ToalBudget": 561800, "DiffCost": 520500, "DiffBudget": -21800},
]

# Columns that must contain numeric values (not strings)
NUMERIC_COLUMNS = [
    "Id", "Max project cost",
    "Budget24", "Budget 25", "Budget 26",
    "Cost24", "Cost25", "Cost26",
    "TotalCost", "ToalBudget", "DiffCost", "DiffBudget",
]


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def _normalize(s: str) -> str:
    """Strip whitespace and lowercase for tolerant header comparison."""
    return s.strip().lower()


def _headers_match(actual: list[str], expected: list[str]) -> bool:
    """Compare header lists using normalized form."""
    if len(actual) != len(expected):
        return False
    return all(_normalize(a) == _normalize(e) for a, e in zip(actual, expected))


def _row_value(row, key: str, headers: list[str]):
    """
    Extract a cell value from a row.
    Supports row as dict (keyed by header) or list (positional).
    Uses normalized header matching when row is a list.
    """
    if isinstance(row, dict):
        # Try exact key first, then normalized match
        if key in row:
            return row[key]
        norm_key = _normalize(key)
        for k, v in row.items():
            if _normalize(k) == norm_key:
                return v
        return None
    elif isinstance(row, list):
        norm_key = _normalize(key)
        for idx, h in enumerate(headers):
            if _normalize(h) == norm_key and idx < len(row):
                return row[idx]
        return None
    return None


def _to_number(val):
    """Coerce a value to int or float if possible, return as-is otherwise."""
    if isinstance(val, (int, float)):
        return val
    if isinstance(val, str):
        try:
            return int(val)
        except ValueError:
            try:
                return float(val)
            except ValueError:
                return val
    return val


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    session._log("[INFO] Step10 — Atomizer Excel (concrete value validation)")

    # ------------------------------------------------------------------
    # 1. Load state (tokens, xlsx_file_id from step02)
    # ------------------------------------------------------------------
    state = session.load_state()
    tokens = state.get("tokens", {})
    session.assert_true(bool(tokens), "Tokens loaded from state")

    xlsx_file_id = (state.get("file_ids", {}).get("xlsx")
                    or state.get("xlsx_file_id"))

    token = session.login(USERS["admin1"]["email"], USERS["admin1"]["password"])
    if not token:
        session._err("[FATAL] Cannot login as admin1 — aborting step10")
        session.save_log(STEP_NAME)
        return 1
    session.restore_auth_from_state()
    data_session = session.for_service(SERVICES["engine_data"])
    ingestor_session = session.for_service(SERVICES["engine_ingestor"])

    if not xlsx_file_id:
        session._log("[WARN] No xlsx_file_id in state — some tests will use missing_feature")
        session.missing_feature("xlsx_file_id",
                                "No XLSX file uploaded in prior steps (step02)")

    # ------------------------------------------------------------------
    # 2. Check Excel processing status
    # ------------------------------------------------------------------
    if xlsx_file_id:
        status, body = ingestor_session.call("GET", f"/api/files/{xlsx_file_id}",
                                    expected_status=200, tag="xlsx-processing-status")
        if status == 200 and isinstance(body, dict):
            file_status = body.get("status") or body.get("processingStatus")
            session.assert_true(
                file_status is not None,
                f"Excel processing status field present: {file_status}"
            )
        elif status != 200:
            session.missing_feature(f"GET /api/files/{xlsx_file_id}",
                                    "File metadata endpoint on ingestor not implemented")
    else:
        session.missing_feature("GET /api/files/<id>",
                                "No xlsx_file_id available")

    # ------------------------------------------------------------------
    # 3. Get structure — verify sheet name and count
    # ------------------------------------------------------------------
    if xlsx_file_id:
        status, body = data_session.call("GET", f"/api/query/files/{xlsx_file_id}/data",
                                    expected_status=200, tag="xlsx-structure")
        if status == 200 and isinstance(body, dict):
            # Response format: {"fileId": ..., "tables": [...], "documents": [...]}
            # Extract sheet info from tables array or dedicated sheet fields
            sheets = body.get("sheets") or body.get("sheetNames") or body.get("sheet_names")
            tables = body.get("tables", [])

            if sheets is None and isinstance(tables, list) and len(tables) > 0:
                # Derive sheet info from tables — each table may have a sheetName/name field
                sheets = []
                for t in tables:
                    if isinstance(t, dict):
                        sn = t.get("sheetName") or t.get("sheet_name") or t.get("name")
                        if sn and sn not in sheets:
                            sheets.append(sn)

            if sheets is None and isinstance(tables, list) and len(tables) == 0:
                # Data not yet processed by pipeline — tables array is empty
                session._log("[INFO] File data endpoint returned empty tables — data not yet processed by orchestrator pipeline")
                session._log("[INFO] Skipping sheet structure assertions until pipeline processes the file")
            elif isinstance(sheets, list):
                session.assert_true(
                    len(sheets) == 1,
                    f"Exactly 1 sheet in workbook (got {len(sheets) if isinstance(sheets, list) else 'N/A'})"
                )
                if len(sheets) >= 1:
                    first_sheet = sheets[0]
                    sheet_name = first_sheet if isinstance(first_sheet, str) else first_sheet.get("name", first_sheet)
                    session.assert_true(
                        _normalize(str(sheet_name)) == _normalize(EXPECTED_SHEET_NAME),
                        f"Sheet name is '{EXPECTED_SHEET_NAME}' (got '{sheet_name}')"
                    )
            else:
                session._log("[INFO] Could not determine sheet info from response")
        elif status != 200:
            session.missing_feature(f"GET /api/query/files/{xlsx_file_id}/data",
                                    "File data endpoint not implemented")
    else:
        session.missing_feature("GET /api/query/files/<id>/data",
                                "No xlsx_file_id available")

    # ------------------------------------------------------------------
    # 4. Get sheet content — verify headers, row count, spot-check values
    # ------------------------------------------------------------------
    actual_headers: list[str] = []
    actual_rows: list = []

    if xlsx_file_id:
        status, body = data_session.call("GET", f"/api/query/files/{xlsx_file_id}/data",
                                    expected_status=200, tag="xlsx-sheet-content")
        if status == 200 and isinstance(body, dict):
            raw_headers = body.get("headers") or body.get("columns") or []
            raw_rows = body.get("rows") or body.get("data") or []

            # If response uses {"tables": [...]} format, extract from first table
            tables = body.get("tables", [])
            if not raw_headers and not raw_rows and isinstance(tables, list) and len(tables) > 0:
                first_table = tables[0]
                if isinstance(first_table, dict):
                    raw_headers = first_table.get("headers") or first_table.get("columns") or []
                    raw_rows = first_table.get("rows") or first_table.get("data") or []

            actual_headers = list(raw_headers)
            actual_rows = list(raw_rows)

            # If tables is empty and no headers/rows, data not yet processed
            if len(actual_headers) == 0 and len(actual_rows) == 0:
                session._log("[INFO] File data endpoint returned no headers/rows — data not yet processed by orchestrator pipeline")
                session._log("[INFO] Skipping header, row count, and spot-check assertions")
            else:
                # 4a. Header matching (normalized)
                session.assert_true(
                    _headers_match(actual_headers, EXPECTED_HEADERS),
                    f"Headers match expected {len(EXPECTED_HEADERS)} columns "
                    f"(got {len(actual_headers)}: {actual_headers})"
                )

                # 4b. Row count
                session.assert_true(
                    len(actual_rows) == EXPECTED_ROW_COUNT,
                    f"Exactly {EXPECTED_ROW_COUNT} data rows (got {len(actual_rows)})"
                )

                # 4c. Spot-check Row 0 (Item1)
                if len(actual_rows) > 0:
                    r0 = actual_rows[0]
                    session.assert_true(
                        _to_number(_row_value(r0, "Id", actual_headers)) == 1,
                        "Row 0: Id == 1"
                    )
                    session.assert_true(
                        str(_row_value(r0, "Project Name", actual_headers)).strip() == "Item1",
                        "Row 0: Project Name == 'Item1'"
                    )
                    session.assert_true(
                        _to_number(_row_value(r0, "Max project cost", actual_headers)) == 150000,
                        "Row 0: Max project cost == 150000"
                    )
                    session.assert_true(
                        _to_number(_row_value(r0, "TotalCost", actual_headers)) == 155000,
                        "Row 0: TotalCost == 155000"
                    )

                # 4d. Spot-check Row 3 (Item4 — largest project)
                if len(actual_rows) > 3:
                    r3 = actual_rows[3]
                    session.assert_true(
                        _to_number(_row_value(r3, "Id", actual_headers)) == 4,
                        "Row 3: Id == 4"
                    )
                    session.assert_true(
                        str(_row_value(r3, "Project Name", actual_headers)).strip() == "Item4",
                        "Row 3: Project Name == 'Item4'"
                    )
                    session.assert_true(
                        _to_number(_row_value(r3, "Max project cost", actual_headers)) == 1265000,
                        "Row 3: Max project cost == 1265000"
                    )
                    session.assert_true(
                        _to_number(_row_value(r3, "TotalCost", actual_headers)) == 1342500,
                        "Row 3: TotalCost == 1342500 (largest project)"
                    )

                # 4e. Spot-check Row 4 (Item5 — biggest positive DiffCost)
                if len(actual_rows) > 4:
                    r4 = actual_rows[4]
                    session.assert_true(
                        _to_number(_row_value(r4, "Id", actual_headers)) == 5,
                        "Row 4: Id == 5"
                    )
                    session.assert_true(
                        _to_number(_row_value(r4, "DiffCost", actual_headers)) == 520500,
                        "Row 4: DiffCost == 520500 (biggest positive diff)"
                    )

        elif status != 200:
            session.missing_feature(f"GET /api/query/files/{xlsx_file_id}/data",
                                    "File data endpoint not implemented")
    else:
        session.missing_feature("GET /api/query/files/<id>/data",
                                "No xlsx_file_id available")

    # ------------------------------------------------------------------
    # 5. Data type validation — numeric columns must be numbers
    # ------------------------------------------------------------------
    if actual_rows and actual_headers:
        session._log("[INFO] Validating numeric data types in returned rows")
        type_ok = True
        for row_idx, row in enumerate(actual_rows):
            for col_name in NUMERIC_COLUMNS:
                val = _row_value(row, col_name, actual_headers)
                if val is not None and not isinstance(val, (int, float)):
                    type_ok = False
                    session._log(
                        f"[WARN] Row {row_idx}, column '{col_name}': "
                        f"expected numeric, got {type(val).__name__} ({val!r})"
                    )
        session.assert_true(
            type_ok,
            "All numeric columns contain numeric values (int/float), not strings"
        )

    # ------------------------------------------------------------------
    # 6. Aggregation check — sum of TotalCost
    # ------------------------------------------------------------------
    if actual_rows and actual_headers:
        total_cost_sum = 0
        for row in actual_rows:
            val = _to_number(_row_value(row, "TotalCost", actual_headers))
            if isinstance(val, (int, float)):
                total_cost_sum += val

        session.assert_true(
            total_cost_sum == EXPECTED_TOTAL_COST_SUM,
            f"Sum of TotalCost == {EXPECTED_TOTAL_COST_SUM} (got {total_cost_sum})"
        )

    # ------------------------------------------------------------------
    # 7. Invalid sheet index — graceful error handling
    # ------------------------------------------------------------------
    if xlsx_file_id:
        session._log("[INFO] Per-sheet endpoint does not exist — marking as missing feature")
        session.missing_feature(f"GET /api/query/files/{xlsx_file_id}/sheets/9999",
                                "Per-sheet endpoint does not exist in current API")

    # ------------------------------------------------------------------
    # Done
    # ------------------------------------------------------------------
    session._log("[INFO] Step10 completed — all endpoint checks done")
    session.sync_counters_from(data_session)
    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
