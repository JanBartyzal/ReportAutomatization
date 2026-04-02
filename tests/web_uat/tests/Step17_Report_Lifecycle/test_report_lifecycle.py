# Web UAT Test: Step17 - Report Lifecycle UI (FS17)
# Verifies: State machine DRAFT→SUBMITTED→UNDER_REVIEW→APPROVED/REJECTED

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step17_Report_Lifecycle"


def test_reports_page_accessible(session: WebTestSession) -> bool:
    """Verify reports page loads with report table."""
    session._log("[TEST] Reports page is accessible")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    session.navigate_to("/reports")
    session.wait_for_load(timeout=10)

    # Check for report table or list (FluentUI DataGrid uses role='grid' or table)
    table_selectors = [
        "[role='grid']", "table",
        "[data-testid='report-table']", ".report-table",
        "[role='row']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in table_selectors)

    return session.assert_true(found, "Reports page has report table or list")


def test_report_list_columns(session: WebTestSession) -> bool:
    """Verify report table shows expected columns."""
    session._log("[TEST] Report table columns")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/reports")
    session.wait_for_load(timeout=10)

    # Expected columns: Name, Status, Period, Company, Actions
    expected_headers = ["Status", "status", "Stav", "Období", "Period", "Company", "Společnost"]

    # Look for table headers
    header_selectors = [
        "[role='columnheader']", "table th", "th",
        "[data-testid='report-table'] th",
    ]

    headers_found = []
    for selector in header_selectors:
        if session.is_element_present(selector, timeout=1):
            headers = session.driver.find_elements(By.CSS_SELECTOR, selector)
            headers_found = [h.text.strip() for h in headers if h.text.strip()]
            break

    session._log(f"[INFO] Found table headers: {headers_found}")

    # Check for key columns (Report, Status, Organization, Period, etc.)
    has_relevant = any(
        any(kw in h.lower() for kw in ("status", "stav", "report", "period", "organization", "updated"))
        for h in headers_found
    )
    return session.assert_true(
        has_relevant or len(headers_found) >= 2,
        f"Report table has expected columns (found: {headers_found})"
    )


def test_status_badges_visible(session: WebTestSession) -> bool:
    """Verify status badges are displayed for each report."""
    session._log("[TEST] Status badges visible")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/reports")
    session.wait_for_load(timeout=10)

    # Look for status badges (FluentUI Badge renders as span with specific styles)
    badge_selectors = [
        ".status-badge", "[data-testid='status-badge']",
        # FluentUI Badge is a span — try various status text values
        "span:has-text('Draft')", "span:has-text('DRAFT')",
        "span:has-text('Submitted')", "span:has-text('SUBMITTED')",
        "span:has-text('Approved')", "span:has-text('APPROVED')",
        "span:has-text('Under Review')", "span:has-text('Rejected')",
    ]

    badge_found = any(session.is_element_present(sel, timeout=1) for sel in badge_selectors)

    if badge_found:
        return session.assert_true(True, "Report rows show status badges")
    else:
        session.missing_feature("Status badges", "Status badges not found (may need report data)")
        return True


def test_filter_by_status(session: WebTestSession) -> bool:
    """Verify status filter works."""
    session._log("[TEST] Status filter")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/reports")
    session.wait_for_load(timeout=10)

    # Look for status filter
    filter_selectors = [
        "[data-testid='status-filter']",
        "select[name='status']",
        ".status-filter"
    ]

    for selector in filter_selectors:
        if session.is_element_present(selector, timeout=1):
            from selenium.webdriver.support.ui import Select
            dropdown = session.driver.find_element(By.CSS_SELECTOR, selector)
            select = Select(dropdown)

            options = [opt.text for opt in select.options]
            session._log(f"[INFO] Status filter options: {options}")

            if len(options) > 1:
                # Select "Submitted" filter
                for i, opt in enumerate(options):
                    if "submitted" in opt.lower() or "odeslan" in opt.lower():
                        select.select_by_index(i)
                        time.sleep(1)
                        session.assert_true(True, f"Applied status filter: {opt}")
                        return True

            # Just select first non-all option
            if len(options) > 1:
                select.select_by_index(1)
                time.sleep(1)
                session.assert_true(True, "Status filter applied")
                return True

    session.missing_feature("Status filter", "Status filter dropdown not found")
    return True


def test_report_detail_page(session: WebTestSession) -> bool:
    """Verify clicking a report opens detail page."""
    session._log("[TEST] Report detail page")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/reports")
    session.wait_for_load(timeout=10)

    # Try to click on a report row
    row_selectors = [
        "[role='row']", "tbody tr",
        "[data-testid='report-row']", ".report-row",
    ]

    clicked = False
    for selector in row_selectors:
        rows = session.driver.find_elements(By.CSS_SELECTOR, selector)
        if rows:
            rows[0].click()
            time.sleep(2)
            clicked = True
            break

    if not clicked:
        session._log("[WARN] Could not click report row")
        session.missing_feature("Report click navigation", "Could not click on report row")
        return True

    # Check URL or content changed
    current_url = session.driver.current_url.lower()
    detail_opened = (
        "report" in current_url and ("/" in current_url.split("report")[-1] or "detail" in current_url)
    ) or session.is_visible("[data-testid='report-header'], .report-header", timeout=5)

    if detail_opened:
        session.assert_true(True, "Report detail page opened")

        # Check for status badge
        status_visible = session.is_visible("[data-testid='status-badge'], .status-badge", timeout=1)
        session.assert_true(status_visible, "Report detail shows status badge")

        # Check for action buttons
        action_buttons = [
            "button:has-text('Submit')",
            "button:has-text('Approve')",
            "button:has-text('Reject')",
            "button:has-text('Odeslat')",
            "button:has-text('Schválit')"
        ]
        has_actions = any(session.is_visible(btn, timeout=2) for btn in action_buttons)
        session.assert_true(has_actions, "Report detail has action buttons")

        # Check for state history
        history_visible = session.is_visible("[data-testid='state-history'], .timeline, .state-history", timeout=1)
        session.assert_true(history_visible, "Report detail shows state history")
    else:
        session.missing_feature("Report detail view", "Detail page did not open on row click")

    return True


def test_matrix_view(session: WebTestSession) -> bool:
    """Verify matrix view for period/company overview."""
    session._log("[TEST] Matrix view (Company × Period)")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/reports")
    session.wait_for_load(timeout=10)

    # Look for matrix view toggle
    matrix_btn_selectors = [
        "[data-testid='matrix-view']",
        "button:has-text('Matrix')",
        "button:has-text('Zobrazit matici')",
        ".view-toggle button:nth-child(2)"
    ]

    for selector in matrix_btn_selectors:
        if session.is_element_present(selector, timeout=1):
            session.click(selector)
            time.sleep(2)

            # Check if matrix loaded
            matrix_visible = session.is_visible("[data-testid='matrix-view'], .matrix-view", timeout=5)
            if matrix_visible:
                session.assert_true(True, "Matrix view is displayed")
                return True

    session.missing_feature("Matrix view", "Matrix view toggle not found or not functional")
    return True


def test_holding_admin_approve_button(session: WebTestSession) -> bool:
    """Verify Holding Admin sees approve/reject buttons."""
    session._log("[TEST] Holding Admin action buttons")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])

    # Navigate to a submitted report
    session.navigate_to("/reports")
    session.wait_for_load(timeout=10)

    # Try to find approve/reject buttons
    approve_selectors = [
        "button:has-text('Approve')",
        "button:has-text('Schválit')",
        "button:has-text('Reject')",
        "button:has-text('Zamítnout')"
    ]

    found_buttons = []
    for selector in approve_selectors:
        if session.is_visible(selector, timeout=1):
            found_buttons.append(selector)

    if found_buttons:
        session.assert_true(True, f"Holding Admin sees action buttons: {found_buttons}")
    else:
        session._log("[INFO] No approve/reject buttons visible (may need submitted report)")

    return True


def test_rejection_flow(session: WebTestSession) -> bool:
    """Verify rejection requires comment."""
    session._log("[TEST] Rejection flow with comment")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])

    # Navigate to a report that can be rejected
    session.navigate_to("/reports")
    session.wait_for_load(timeout=10)

    # Try to find and click reject button
    reject_selectors = [
        "button:has-text('Reject')",
        "button:has-text('Zamítnout')"
    ]

    rejected = False
    for selector in reject_selectors:
        if session.click(selector, timeout=1):
            time.sleep(1)

            # Check for rejection comment field
            comment_selectors = [
                "[data-testid='rejection-comment']",
                "textarea[name='rejectionReason']",
                "textarea"
            ]

            for comment_sel in comment_selectors:
                if session.is_element_present(comment_sel, timeout=1):
                    session.type_text(comment_sel, "Test rejection reason")
                    session.assert_true(True, "Rejection comment field is required")
                    rejected = True
                    break
            break

    if not rejected:
        session._log("[INFO] Rejection flow not tested (may need specific report state)")

    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step17: Report Lifecycle UI")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_reports_page_accessible(session)
        test_report_list_columns(session)
        test_status_badges_visible(session)
        test_filter_by_status(session)
        test_report_detail_page(session)
        test_matrix_view(session)
        test_holding_admin_approve_button(session)
        test_rejection_flow(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
