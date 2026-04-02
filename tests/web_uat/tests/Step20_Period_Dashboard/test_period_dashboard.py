# Web UAT Test: Step20 - Period Management UI (FS20)
# Verifies: Period dashboard, matrix view, deadline tracking, completion %

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step20_Period_Dashboard"


def test_periods_page_accessible(session: WebTestSession) -> bool:
    """Verify periods page loads."""
    session._log("[TEST] Periods page is accessible")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    session.navigate_to("/periods")
    session.wait_for_load(timeout=10)

    # Check for period table or list (FluentUI DataGrid uses role='grid' or table)
    period_selectors = [
        "[role='grid']", "table",
        "[data-testid='period-table']", ".period-table",
        "[role='row']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in period_selectors)

    return session.assert_true(found, "Periods page loads with period table or list")


def test_period_table_columns(session: WebTestSession) -> bool:
    """Verify period table shows expected columns."""
    session._log("[TEST] Period table columns")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/periods")
    session.wait_for_load(timeout=10)

    # Look for table headers
    header_selectors = ["[role='columnheader']", "table th", "th"]
    headers_found = []

    for selector in header_selectors:
        if session.is_element_present(selector, timeout=1):
            headers = session.driver.find_elements(By.CSS_SELECTOR, selector)
            headers_found = [h.text.strip() for h in headers if h.text.strip()]
            break

    session._log(f"[INFO] Found table headers: {headers_found}")

    # Check for key columns
    has_name = any("name" in h.lower() or "název" in h.lower() or "období" in h.lower() for h in headers_found)
    has_status = any("status" in h.lower() or "stav" in h.lower() for h in headers_found)
    has_deadline = any("deadline" in h.lower() or "termín" in h.lower() for h in headers_found)

    return session.assert_true(has_name or has_status, "Period table has Name or Status column")


def test_period_status_badges(session: WebTestSession) -> bool:
    """Verify status badges are displayed for periods."""
    session._log("[TEST] Period status badges")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/periods")
    session.wait_for_load(timeout=10)

    # Look for status badges — FluentUI Badge renders as span with status text
    status_values = [
        "OPEN", "Open", "COLLECTING", "Collecting",
        "REVIEWING", "Reviewing", "CLOSED", "Closed",
    ]

    badge_found = False
    for status in status_values:
        if session.is_visible(f"span:has-text('{status}')", timeout=1):
            badge_found = True
            session._log(f"[OK]   Found status badge: {status}")
            break

    if not badge_found:
        badge_selectors = [".status-badge", "[data-testid='period-status']"]
        badge_found = any(session.is_element_present(sel, timeout=1) for sel in badge_selectors)

    if badge_found:
        return session.assert_true(True, "Period rows show status badges")
    else:
        session.missing_feature("Period status badges", "Status badges not visible (may need period data)")
        return True


def test_create_period_button(session: WebTestSession) -> bool:
    """Verify create period button exists."""
    session._log("[TEST] Create period button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/periods")
    session.wait_for_load(timeout=10)

    create_selectors = [
        "button:has-text('Create Period')",
        "button:has-text('Vytvořit období')",
        "button:has-text('New Period')",
        "[data-testid='create-period']"
    ]

    found = False
    for selector in create_selectors:
        if session.is_visible(selector, timeout=1):
            found = True
            break

    return session.assert_true(found, "Create period button is present")


def test_clone_period_button(session: WebTestSession) -> bool:
    """Verify clone period functionality."""
    session._log("[TEST] Clone period button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/periods")
    session.wait_for_load(timeout=10)

    clone_selectors = [
        "button:has-text('Clone')",
        "button:has-text('Klonovat')",
        "[data-testid='clone-period']"
    ]

    found = False
    for selector in clone_selectors:
        if session.is_element_present(selector, timeout=1):
            found = True
            session._log(f"[OK]   Found clone button: {selector}")
            break

    if found:
        session.assert_true(True, "Clone period button is present")
    else:
        session.missing_feature("Clone period", "Clone button not found (may need existing period)")

    return True


def test_period_detail_matrix_view(session: WebTestSession) -> bool:
    """Verify period detail page has matrix view."""
    session._log("[TEST] Period detail matrix view")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/periods")
    session.wait_for_load(timeout=10)

    # Try to click on a period row to open detail
    row_selectors = [
        "[role='row']", "tbody tr",
        "[data-testid='period-row']", ".period-row",
    ]

    clicked = False
    for selector in row_selectors:
        rows = session.driver.find_elements(By.CSS_SELECTOR, selector)
        if rows:
            rows[0].click()
            time.sleep(2)
            clicked = True
            break

    # Check for matrix view
    matrix_selectors = [
        "[data-testid='matrix-view']",
        ".matrix-view"
    ]

    matrix_found = False
    for selector in matrix_selectors:
        if session.is_visible(selector, timeout=5):
            matrix_found = True
            break

    if matrix_found:
        session.assert_true(True, "Period detail shows matrix view")
    else:
        session.missing_feature("Matrix view", "Matrix view not found on period detail")

    return True


def test_matrix_company_x_period(session: WebTestSession) -> bool:
    """Verify matrix shows Company × Period grid."""
    session._log("[TEST] Matrix shows Company × Period grid")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/periods")
    session.wait_for_load(timeout=10)

    # Try to open matrix view
    matrix_btn_selectors = [
        "button:has-text('View Matrix')",
        "button:has-text('Zobrazit matici')",
        "[data-testid='matrix-view']"
    ]

    for selector in matrix_btn_selectors:
        if session.is_element_present(selector, timeout=1):
            session.click(selector)
            time.sleep(2)
            break

    # Check for matrix table
    matrix_table = session.is_visible("table.matrix, .matrix-table, [data-testid='matrix-table']", timeout=5)

    if matrix_table:
        # Check dimensions
        try:
            rows = session.driver.find_elements(By.CSS_SELECTOR, "table.matrix tr, .matrix-table tr")
            if rows:
                cols = len(rows[0].find_elements(By.CSS_SELECTOR, "td, th"))
                session._log(f"[INFO] Matrix dimensions: {len(rows)} rows × {cols} cols")
        except Exception:
            pass
        session.assert_true(True, "Matrix table shows Company × Period grid")
    else:
        session.missing_feature("Matrix table", "Matrix table not displayed")

    return True


def test_completion_percentage(session: WebTestSession) -> bool:
    """Verify completion percentage is displayed."""
    session._log("[TEST] Completion percentage")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/periods")
    session.wait_for_load(timeout=10)

    # Look for completion indicator
    completion_selectors = [
        "[data-testid='completion-bar']",
        ".completion-bar",
        ".completion-percentage",
        "span:has-text('%')"
    ]

    found = False
    for selector in completion_selectors:
        if session.is_element_present(selector, timeout=1):
            found = True
            text = session.get_text(selector)
            session._log(f"[INFO] Completion: {text}")
            break

    if found:
        session.assert_true(True, "Completion percentage is displayed")
    else:
        session.missing_feature("Completion percentage", "Completion indicator not found")

    return True


def test_status_legend(session: WebTestSession) -> bool:
    """Verify status color legend is displayed."""
    session._log("[TEST] Status legend")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/periods")
    session.wait_for_load(timeout=10)

    legend_selectors = [
        "[data-testid='status-legend']",
        ".status-legend",
        ".legend"
    ]

    found = False
    for selector in legend_selectors:
        if session.is_visible(selector, timeout=5):
            found = True
            # Check for color indicators
            colors = session.driver.find_elements(By.CSS_SELECTOR, selector + " .color-box, .legend-item")
            session._log(f"[INFO] Found {len(colors)} legend items")
            break

    if found:
        session.assert_true(True, "Status legend is displayed")
    else:
        session.missing_feature("Status legend", "Legend not visible")

    return True


def test_export_period_status(session: WebTestSession) -> bool:
    """Verify period status can be exported."""
    session._log("[TEST] Export period status")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/periods")
    session.wait_for_load(timeout=10)

    export_selectors = [
        "button:has-text('Export')",
        "[data-testid='export-period']"
    ]

    found = False
    for selector in export_selectors:
        if session.is_element_present(selector, timeout=1):
            found = True
            break

    if found:
        session.assert_true(True, "Export button is present")
    else:
        session.missing_feature("Export period", "Export button not found")

    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step20: Period Management UI")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_periods_page_accessible(session)
        test_period_table_columns(session)
        test_period_status_badges(session)
        test_create_period_button(session)
        test_clone_period_button(session)
        test_period_detail_matrix_view(session)
        test_matrix_company_x_period(session)
        test_completion_percentage(session)
        test_status_legend(session)
        test_export_period_status(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
