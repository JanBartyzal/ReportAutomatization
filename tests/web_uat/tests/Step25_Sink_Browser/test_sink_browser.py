# Web UAT Test: Step25 - Sink Browser UI (FS25)
# Verifies: sink list, filters, data-source stats, corrections/selection controls

import os
import sys
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step25_Sink_Browser"


def _login_editor(session: WebTestSession) -> None:
    user = USERS["editor"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)


def test_sink_browser_accessible(session: WebTestSession) -> bool:
    session._log("[TEST] Sink browser page is accessible")
    _login_editor(session)
    session.navigate_to("/sinks")
    session.wait_for_load(timeout=10)

    selectors = [
        "h1:has-text('Sink')",
        "h2:has-text('Sink')",
        "table",
        "[role='grid']",
        "input[placeholder*='Sheet']",
        "input[placeholder*='file ID']",
    ]
    found = any(session.is_visible(sel, timeout=1) for sel in selectors)
    return session.assert_true(found and "sinks" in session.driver.current_url.lower(), "Sink browser route renders")


def test_sink_filters_present(session: WebTestSession) -> bool:
    session._log("[TEST] Sink browser filters")
    _login_editor(session)
    session.navigate_to("/sinks")
    session.wait_for_load(timeout=10)

    sheet_filter = any(session.is_element_present(sel, timeout=1) for sel in [
        "input[placeholder*='Sheet']",
        "input[placeholder*='sheet']",
        "input[name='source_sheet']",
    ])
    file_filter = any(session.is_element_present(sel, timeout=1) for sel in [
        "input[placeholder*='file ID']",
        "input[placeholder*='File ID']",
        "input[name='file_id']",
    ])
    clear_filter = any(session.is_visible(sel, timeout=1) for sel in [
        "button:has-text('Clear')",
        "button:has-text('Reset')",
    ])

    session.assert_true(sheet_filter, "Sink browser has sheet/search filter")
    session.assert_true(file_filter, "Sink browser has file ID filter")
    if clear_filter:
        session.assert_true(True, "Sink browser has clear/reset filters control")
    else:
        session.missing_feature("Clear filters", "Clear/reset filters control not visible")
    return True


def test_data_source_stats_visible(session: WebTestSession) -> bool:
    session._log("[TEST] Sink data-source stats")
    _login_editor(session)
    session.navigate_to("/sinks")
    session.wait_for_load(timeout=10)

    stats_found = any(session.is_visible(sel, timeout=1) for sel in [
        "span:has-text('POSTGRES')",
        "span:has-text('SPARK')",
        "span:has-text('BLOB')",
        "div:has-text('Data Source')",
        "[data-testid='data-source-stats']",
    ])
    if stats_found:
        session.assert_true(True, "Data-source stats or backend markers are visible")
    else:
        session.missing_feature("Data-source stats", "Storage backend stats/markers not visible")
    return True


def test_sink_detail_or_empty_state(session: WebTestSession) -> bool:
    session._log("[TEST] Sink detail navigation or empty state")
    _login_editor(session)
    session.navigate_to("/sinks")
    session.wait_for_load(timeout=10)

    row_selectors = ["tbody tr", "[role='row']", ".sink-row", "[data-testid='sink-row']"]
    clicked = False
    for selector in row_selectors:
        rows = session.driver.find_elements(By.CSS_SELECTOR, selector)
        if rows:
            try:
                rows[0].click()
                time.sleep(1)
                clicked = True
                break
            except Exception:
                pass

    if clicked:
        detail_found = any(session.is_visible(sel, timeout=3) for sel in [
            "button:has-text('Back')",
            "button:has-text('Select')",
            "input[placeholder*='Note']",
            "table",
        ])
        session.assert_true(detail_found, "Sink detail page opens from list")
    else:
        empty_state = any(session.is_visible(sel, timeout=1) for sel in [
            "div:has-text('No sinks')",
            "div:has-text('No data')",
            "span:has-text('No rows')",
        ])
        if empty_state:
            session.assert_true(True, "Sink browser shows an empty state when no sinks exist")
        else:
            session.missing_feature("Sink detail navigation", "No sink rows available and no explicit empty state found")
    return True


def test_sink_correction_controls(session: WebTestSession) -> bool:
    session._log("[TEST] Sink correction and selection controls")
    _login_editor(session)
    session.navigate_to("/sinks")
    session.wait_for_load(timeout=10)

    controls_found = any(session.is_element_present(sel, timeout=1) for sel in [
        "button:has-text('Select')",
        "button:has-text('Selected')",
        "button:has-text('Correct')",
        "button:has-text('Edit')",
        "input[placeholder*='Note']",
        "[data-testid='sink-selection']",
        "[data-testid='correction-editor']",
    ])
    if controls_found:
        session.assert_true(True, "Sink selection/correction controls are visible")
    else:
        session.missing_feature("Sink correction controls", "Selection/correction controls not visible on list; may require sink detail data")
    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)
    session._log("[INFO] Web UAT Tests - Step25: Sink Browser UI")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_sink_browser_accessible(session)
        test_sink_filters_present(session)
        test_data_source_stats_visible(session)
        test_sink_detail_or_empty_state(session)
        test_sink_correction_controls(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
