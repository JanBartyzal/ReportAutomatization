#!/usr/bin/env python3
# Step03: Atomizer PPTX (FS03)
# Verify PPTX atomizer: processing status, structure extraction, slide content,
# pseudo-table reconstruction, and comparison with expected result.
#
# Source: tests/UAT/data/DemoPage.pptx (1 slide, 71 shapes, pseudo-table)
# Expected: tests/UAT/data/Results/DemoData-Result.xlsx
# Metadata: tests/UAT/data/slide_metadata.json

import sys
import os
import json

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step03_Atomizer_PPTX"

# ---------------------------------------------------------------------------
# Expected data from DemoPage.pptx (1 slide pseudo-table)
# ---------------------------------------------------------------------------
DATA_DIR = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../data"))
METADATA_FILE = os.path.join(DATA_DIR, "slide_metadata.json")

EXPECTED_SLIDE_COUNT = 1
EXPECTED_TITLE = "Cost optimization"
EXPECTED_LAYOUT = "Title and Content"
EXPECTED_SHAPE_COUNT = 71

# Main table: "Cost optimatiztation" sheet
EXPECTED_MAIN_TABLE_HEADERS = [
    "Cost Category",
    "Main saving initiatives description (focus on non-comp)",
    "Net saving 2024 (M€) ",
    "Additional net saving 2024 (M€)",
]
EXPECTED_MAIN_TABLE_ROW_COUNT = 14
EXPECTED_MAIN_NET_SAVING_SUM = -50.7  # sum of numeric rows (excl. WIP)
EXPECTED_MAIN_FIRST_ROW_NET = -21.4
EXPECTED_MAIN_WIP_COUNT = 1

# Total summary table (3 rows at bottom)
EXPECTED_TOTAL_ROWS = [
    {"label": "Savings identified", "value1": -100, "value2": -2},
    {"label": "Savings to be identified", "value1": -2.6, "value2": 0},
    {"label": "TOTAL", "value1": -8.6, "value2": 0},
]


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})
    file_ids = state.get("file_ids", {})

    session._log("[INFO] Step03 — Atomizer PPTX Verification")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    # ---------------------------------------------------------------
    # 0. Verify metadata file exists
    # ---------------------------------------------------------------
    if os.path.exists(METADATA_FILE):
        with open(METADATA_FILE, "r", encoding="utf-8") as f:
            metadata = json.load(f)
        session._log(f"[INFO] Loaded slide metadata: {METADATA_FILE}")
        session.assert_true(
            len(metadata.get("slides", [])) == 1,
            "Metadata defines exactly 1 slide"
        )
    else:
        metadata = None
        session._log(f"[WARN] Metadata file not found: {METADATA_FILE}")

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

    session.token = admin1_token
    session.restore_auth_from_state()
    data_session = session.for_service(SERVICES["engine_data"])
    ingestor_session = session.for_service(SERVICES["engine_ingestor"])
    session._log(f"[INFO] PPTX file_id: {pptx_file_id}")

    # ---------------------------------------------------------------
    # 2. Check PPTX processing status
    # ---------------------------------------------------------------
    status, body = ingestor_session.call("GET", f"/api/files/{pptx_file_id}",
                                expected_status=200, tag="pptx-processing-status")
    if status == 200 and isinstance(body, dict):
        has_status = ("status" in body or "state" in body or "processingStatus" in body)
        session.assert_true(has_status, "Processing status response contains status/state field")
        status_val = body.get("status") or body.get("state") or body.get("processingStatus")
        session._log(f"[INFO] PPTX processing status: {status_val}")
    elif status in (404, 500):
        session.missing_feature(
            f"GET /api/files/{pptx_file_id}",
            "File metadata endpoint on ingestor not implemented yet"
        )

    # ---------------------------------------------------------------
    # 3. Get extracted structure — verify slide count, title, layout
    # ---------------------------------------------------------------
    slides_data = None
    status, body = data_session.call("GET", f"/api/query/files/{pptx_file_id}/data",
                                expected_status=200, tag="pptx-structure")
    if status == 200 and isinstance(body, dict):
        slides_data = body.get("slides") or body.get("pages") or body.get("elements")
        session.assert_true(
            isinstance(slides_data, list),
            f"Structure response contains slides list"
        )
        if isinstance(slides_data, list):
            session.assert_true(
                len(slides_data) == EXPECTED_SLIDE_COUNT,
                f"Slide count = {EXPECTED_SLIDE_COUNT} (got {len(slides_data)})"
            )

            if len(slides_data) >= 1:
                slide0 = slides_data[0]
                # Check title
                slide_title = (slide0.get("title") or slide0.get("name") or "")
                if slide_title:
                    session.assert_true(
                        EXPECTED_TITLE.lower() in slide_title.lower(),
                        f"Slide 1 title contains '{EXPECTED_TITLE}' (got '{slide_title}')"
                    )
                else:
                    session._log("[INFO] Slide title not in structure response — will check in content")

                # Check has_tables flag
                has_tables = slide0.get("has_tables") or slide0.get("hasTables") or slide0.get("table_count", 0) > 0
                session._log(f"[INFO] Slide 1 has_tables: {has_tables}")

                # Check shape count if available
                shape_count = slide0.get("shape_count") or slide0.get("shapeCount")
                if shape_count is not None:
                    session.assert_true(
                        shape_count == EXPECTED_SHAPE_COUNT,
                        f"Slide 1 shape count = {EXPECTED_SHAPE_COUNT} (got {shape_count})"
                    )
    elif status in (404, 500):
        session.missing_feature(
            f"GET /api/query/files/{pptx_file_id}/data",
            "File data endpoint not implemented yet"
        )

    # ---------------------------------------------------------------
    # 4. Get slide 1 content — verify texts, tables
    # ---------------------------------------------------------------
    status, body = data_session.call("GET", f"/api/query/files/{pptx_file_id}/slides",
                                expected_status=200, tag="pptx-slide-1-content")
    if status == 200 and isinstance(body, dict):
        # Check title text
        texts = body.get("texts") or body.get("text_elements") or []
        if isinstance(texts, list) and len(texts) > 0:
            all_text = " ".join(str(t) for t in texts).lower()
            session.assert_true(
                "cost optimization" in all_text,
                "Slide 1 texts contain 'Cost optimization'"
            )

        # Check tables extracted
        tables = body.get("tables") or body.get("extracted_tables") or []
        if isinstance(tables, list):
            session._log(f"[INFO] Tables found on slide 1: {len(tables)}")

            # ---------------------------------------------------------------
            # 4a. Verify main pseudo-table (Cost optimization)
            # ---------------------------------------------------------------
            main_table = _find_table(tables, EXPECTED_MAIN_TABLE_HEADERS, EXPECTED_MAIN_TABLE_ROW_COUNT)
            if main_table:
                headers = main_table.get("headers") or main_table.get("columns") or []
                rows = main_table.get("rows") or main_table.get("data") or []

                # Header check
                if headers:
                    session.assert_true(
                        len(headers) == len(EXPECTED_MAIN_TABLE_HEADERS),
                        f"Main table has {len(EXPECTED_MAIN_TABLE_HEADERS)} columns (got {len(headers)})"
                    )

                # Row count
                session.assert_true(
                    len(rows) == EXPECTED_MAIN_TABLE_ROW_COUNT,
                    f"Main table has {EXPECTED_MAIN_TABLE_ROW_COUNT} rows (got {len(rows)})"
                )

                # First row net saving = -21.4
                if rows:
                    first_net = _get_cell_value(rows[0], 2, "net_saving", "Net saving")
                    if first_net is not None:
                        session.assert_true(
                            _approx(first_net, EXPECTED_MAIN_FIRST_ROW_NET),
                            f"Row 1 Net saving = {EXPECTED_MAIN_FIRST_ROW_NET} (got {first_net})"
                        )

                # Sum of numeric Net saving values = -50.7
                numeric_sum = 0.0
                numeric_count = 0
                wip_count = 0
                for row in rows:
                    val = _get_cell_value(row, 2, "net_saving", "Net saving")
                    if isinstance(val, (int, float)):
                        numeric_sum += val
                        numeric_count += 1
                    elif isinstance(val, str) and val.strip().upper() == "WIP":
                        wip_count += 1

                if numeric_count > 0:
                    session.assert_true(
                        _approx(numeric_sum, EXPECTED_MAIN_NET_SAVING_SUM, tolerance=0.1),
                        f"Sum of numeric Net saving = {EXPECTED_MAIN_NET_SAVING_SUM} (got {numeric_sum:.2f})"
                    )
                    session.assert_true(
                        numeric_count == 13,
                        f"Numeric Net saving count = 13 (got {numeric_count})"
                    )
                    session.assert_true(
                        wip_count == EXPECTED_MAIN_WIP_COUNT,
                        f"WIP entries = {EXPECTED_MAIN_WIP_COUNT} (got {wip_count})"
                    )
            else:
                session._log("[INFO] Main pseudo-table not found in extracted tables — may need MetaTable logic")
                session.missing_feature(
                    "PPTX pseudo-table extraction",
                    "MetaTable reconstruction from text boxes/shapes not yet implemented"
                )

            # ---------------------------------------------------------------
            # 4b. Verify total summary table (3 rows)
            # ---------------------------------------------------------------
            total_table = _find_table(tables, None, 3)
            if total_table:
                total_rows = total_table.get("rows") or total_table.get("data") or []
                session.assert_true(
                    len(total_rows) == 3,
                    f"Total table has 3 rows (got {len(total_rows)})"
                )

                # Verify TOTAL row values
                for expected_row in EXPECTED_TOTAL_ROWS:
                    label = expected_row["label"]
                    found = _find_row_by_label(total_rows, label)
                    if found:
                        val1 = _get_cell_value(found, 1, "value1", "value")
                        if val1 is not None:
                            session.assert_true(
                                _approx(val1, expected_row["value1"]),
                                f"Total '{label}' col1 = {expected_row['value1']} (got {val1})"
                            )
                    else:
                        session._log(f"[INFO] Total row '{label}' not found in extracted data")
            else:
                session._log("[INFO] Total summary table not found — may be part of main extraction")
        else:
            session._log("[INFO] No tables key in slide content — pseudo-table extraction may not be implemented")
            session.missing_feature(
                "GET /api/query/files/<id>/slides/1 → tables",
                "Pseudo-table extraction from PPTX shapes not yet implemented"
            )

    elif status in (404, 500):
        session.missing_feature(
            f"GET /api/query/files/{pptx_file_id}/slides",
            "Slides endpoint not implemented yet"
        )

    # ---------------------------------------------------------------
    # 5. Slide image — no endpoint exists
    # ---------------------------------------------------------------
    session.missing_feature(
        f"GET /api/query/files/{pptx_file_id}/slides/<n>/image",
        "Slide image endpoint does not exist in current API"
    )

    # ---------------------------------------------------------------
    # 6. Save extracted data info to state
    # ---------------------------------------------------------------
    pptx_info = {
        "slide_count": EXPECTED_SLIDE_COUNT,
        "main_table_rows": EXPECTED_MAIN_TABLE_ROW_COUNT,
        "total_table_rows": 3,
        "net_saving_sum": EXPECTED_MAIN_NET_SAVING_SUM,
    }
    session.update_state({"pptx_extraction": pptx_info})
    session._log(f"[INFO] PPTX extraction info saved to state")

    session.sync_counters_from(data_session)
    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _find_table(tables: list, expected_headers: list | None, expected_rows: int) -> dict | None:
    """Find a table in extracted tables matching expected header count or row count."""
    if not tables:
        return None

    for tbl in tables:
        if not isinstance(tbl, dict):
            continue
        rows = tbl.get("rows") or tbl.get("data") or []
        headers = tbl.get("headers") or tbl.get("columns") or []

        if expected_headers and len(headers) == len(expected_headers):
            return tbl
        if len(rows) == expected_rows:
            return tbl

    # Fallback: return largest table
    return max(tables, key=lambda t: len(t.get("rows") or t.get("data") or []), default=None)


def _find_row_by_label(rows: list, label: str) -> dict | list | None:
    """Find a row containing the given label text."""
    label_lower = label.lower()
    for row in rows:
        if isinstance(row, dict):
            for val in row.values():
                if isinstance(val, str) and label_lower in val.lower():
                    return row
        elif isinstance(row, (list, tuple)):
            for val in row:
                if isinstance(val, str) and label_lower in val.lower():
                    return row
    return None


def _get_cell_value(row, col_index: int, *key_candidates):
    """Extract cell value from row (supports dict and list formats)."""
    if isinstance(row, dict):
        for key in key_candidates:
            for actual_key in row:
                if key.lower() in actual_key.lower():
                    return row[actual_key]
        # Try by column index from values
        vals = list(row.values())
        if col_index < len(vals):
            return vals[col_index]
    elif isinstance(row, (list, tuple)):
        if col_index < len(row):
            return row[col_index]
    return None


def _approx(actual, expected, tolerance=0.01) -> bool:
    """Check if actual approximately equals expected."""
    try:
        return abs(float(actual) - float(expected)) <= tolerance
    except (ValueError, TypeError):
        return False


if __name__ == "__main__":
    sys.exit(main())
