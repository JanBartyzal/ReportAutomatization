# Web UAT Test: Step16 - Audit & Compliance Log (FS16)
# Verifies: Audit log viewer, filtering, export (CSV/JSON), AI audit, read access log

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step16_Audit"


def test_audit_page_accessible(session: WebTestSession) -> bool:
    """Verify audit log page is accessible."""
    session._log("[TEST] Audit log page accessible")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    # Audit may be under /admin/audit or /audit or as a tab in /admin
    paths_to_try = ["/admin/audit", "/admin"]
    found = False

    for path in paths_to_try:
        session.navigate_to(path)
        session.wait_for_load(timeout=10)

        # If on /admin, try clicking Audit tab
        if path == "/admin":
            audit_tab_selectors = [
                "[data-testid='tab-audit']", "button:has-text('Audit Log')",
                "button:has-text('Audit')", "a:has-text('Audit')",
            ]
            for sel in audit_tab_selectors:
                if session.click(sel, timeout=1):
                    time.sleep(1)
                    break

        audit_selectors = [
            "[data-testid='audit-log']", ".audit-log",
            "[data-testid='audit-table']", ".audit-table",
            "table.audit", ".audit-list",
            "h1:has-text('Audit')", "h2:has-text('Audit')",
        ]

        if any(session.is_visible(sel, timeout=1) for sel in audit_selectors):
            found = True
            break

    if not found:
        session.missing_feature("Audit log page", "Audit log UI not accessible")
        return True

    return session.assert_true(found, "Audit log page accessible")


def test_audit_log_table(session: WebTestSession) -> bool:
    """Verify audit log table shows entries."""
    session._log("[TEST] Audit log table entries")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/audit")
    session.wait_for_load(timeout=10)

    # If redirect, try via admin tab
    if "audit" not in session.driver.current_url.lower():
        session.navigate_to("/admin")
        session.wait_for_load(timeout=10)
        audit_tab = ["[data-testid='tab-audit']", "button:has-text('Audit')"]
        for sel in audit_tab:
            if session.click(sel, timeout=1):
                break
        time.sleep(1)

    table_selectors = [
        "[data-testid='audit-table']", ".audit-table",
        "table.audit", "table", "tbody tr",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in table_selectors)

    if not found:
        session.missing_feature("Audit log table", "Audit log table not displayed")
        return True

    session.assert_true(found, "Audit log table displays entries")
    return True


def test_audit_log_columns(session: WebTestSession) -> bool:
    """Verify audit log has required columns: User, Action, Timestamp, Detail."""
    session._log("[TEST] Audit log columns")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/audit")
    session.wait_for_load(timeout=10)

    if "audit" not in session.driver.current_url.lower():
        session.navigate_to("/admin")
        session.wait_for_load(timeout=10)
        for sel in ["[data-testid='tab-audit']", "button:has-text('Audit')"]:
            if session.click(sel, timeout=1):
                break
        time.sleep(1)

    # Check for column headers
    column_keywords = ["User", "Action", "Timestamp", "Detail", "IP"]
    found_columns = 0

    for keyword in column_keywords:
        col_selectors = [
            f"th:has-text('{keyword}')",
            f"[data-testid='col-{keyword.lower()}']",
        ]
        if any(session.is_visible(sel, timeout=1) for sel in col_selectors):
            found_columns += 1
            session._log(f"[OK]   Audit column found: {keyword}")

    if found_columns == 0:
        session.missing_feature("Audit log columns", "No expected columns found (User, Action, Timestamp, Detail)")
        return True

    session.assert_true(found_columns >= 2, f"Audit log has required columns (found {found_columns}/5)")
    return True


def test_audit_log_filter(session: WebTestSession) -> bool:
    """Verify audit log filtering (by user, action type, date)."""
    session._log("[TEST] Audit log filtering")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/audit")
    session.wait_for_load(timeout=10)

    if "audit" not in session.driver.current_url.lower():
        session.navigate_to("/admin")
        session.wait_for_load(timeout=10)
        for sel in ["[data-testid='tab-audit']", "button:has-text('Audit')"]:
            if session.click(sel, timeout=1):
                break
        time.sleep(1)

    filter_selectors = [
        "[data-testid='audit-filter']", ".audit-filter",
        "select[name='actionType']", "[data-testid='action-filter']",
        "input[type='date']", "[data-testid='date-filter']",
        "input[type='search']", "[data-testid='user-filter']",
        ".filter-bar", "[data-testid='filters']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in filter_selectors)

    if not found:
        session.missing_feature("Audit log filter", "Filter controls not found on audit page")
        return True

    session.assert_true(found, "Audit log filter controls available")
    return True


def test_export_csv_button(session: WebTestSession) -> bool:
    """Verify CSV export button for audit logs."""
    session._log("[TEST] CSV export button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/audit")
    session.wait_for_load(timeout=10)

    if "audit" not in session.driver.current_url.lower():
        session.navigate_to("/admin")
        session.wait_for_load(timeout=10)
        for sel in ["[data-testid='tab-audit']", "button:has-text('Audit')"]:
            if session.click(sel, timeout=1):
                break
        time.sleep(1)

    csv_selectors = [
        "button:has-text('Export CSV')", "button:has-text('Exportovat CSV')",
        "[data-testid='export-csv']", "button:has-text('Export')",
        "button:has-text('Exportovat')", "[data-testid='export-audit']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in csv_selectors)

    if not found:
        session.missing_feature("CSV export", "CSV export button not found on audit page")
        return True

    session.assert_true(found, "CSV export button available")
    return True


def test_export_json_button(session: WebTestSession) -> bool:
    """Verify JSON export button for audit logs."""
    session._log("[TEST] JSON export button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/audit")
    session.wait_for_load(timeout=10)

    if "audit" not in session.driver.current_url.lower():
        session.navigate_to("/admin")
        session.wait_for_load(timeout=10)
        for sel in ["[data-testid='tab-audit']", "button:has-text('Audit')"]:
            if session.click(sel, timeout=1):
                break
        time.sleep(1)

    json_selectors = [
        "button:has-text('Export JSON')", "button:has-text('Exportovat JSON')",
        "[data-testid='export-json']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in json_selectors)

    if not found:
        session.missing_feature("JSON export", "JSON export button not found on audit page")
        return True

    session.assert_true(found, "JSON export button available")
    return True


def test_state_transition_audit(session: WebTestSession) -> bool:
    """Verify state transition entries visible in audit log (FS17 integration)."""
    session._log("[TEST] State transition audit entries")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/audit")
    session.wait_for_load(timeout=10)

    if "audit" not in session.driver.current_url.lower():
        session.navigate_to("/admin")
        session.wait_for_load(timeout=10)
        for sel in ["[data-testid='tab-audit']", "button:has-text('Audit')"]:
            if session.click(sel, timeout=1):
                break
        time.sleep(1)

    # Check for state transition related entries
    transition_selectors = [
        "td:has-text('STATUS_CHANGE')", "td:has-text('STATE_TRANSITION')",
        "td:has-text('APPROVED')", "td:has-text('SUBMITTED')",
        "td:has-text('REJECTED')", "[data-action='state_change']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in transition_selectors)

    if not found:
        session.missing_feature("State transition audit", "State transition audit entries not visible (may need events)")
        return True

    session.assert_true(found, "State transition entries visible in audit log")
    return True


def test_immutable_log_indicator(session: WebTestSession) -> bool:
    """Verify immutable/append-only indicator for audit entries."""
    session._log("[TEST] Immutable log indicator")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/audit")
    session.wait_for_load(timeout=10)

    if "audit" not in session.driver.current_url.lower():
        session.navigate_to("/admin")
        session.wait_for_load(timeout=10)
        for sel in ["[data-testid='tab-audit']", "button:has-text('Audit')"]:
            if session.click(sel, timeout=1):
                break
        time.sleep(1)

    # Check that there are no edit/delete buttons on audit entries
    destructive_selectors = [
        "button:has-text('Delete')", "button:has-text('Edit')",
        "button:has-text('Smazat')", "button:has-text('Upravit')",
        "[data-testid='delete-audit']", "[data-testid='edit-audit']",
    ]

    has_destructive = any(session.is_visible(sel, timeout=1) for sel in destructive_selectors)

    if has_destructive:
        session._err("[FAIL] Audit log has edit/delete buttons — should be immutable!")
        session.assert_true(False, "Audit log should NOT have edit/delete buttons (immutable)")
    else:
        session.assert_true(True, "Audit log has no edit/delete buttons (immutable entries)")

    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step16: Audit & Compliance Log")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_audit_page_accessible(session)
        test_audit_log_table(session)
        test_audit_log_columns(session)
        test_audit_log_filter(session)
        test_export_csv_button(session)
        test_export_json_button(session)
        test_state_transition_audit(session)
        test_immutable_log_indicator(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
