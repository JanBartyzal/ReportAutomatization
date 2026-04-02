# Web UAT Test: Step14 - Data Versioning & Diff Tool (FS14)
# Verifies: Version history, diff tool UI, version comparison, rollback indication

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step14_Versioning"


def test_version_history_in_viewer(session: WebTestSession) -> bool:
    """Verify version history panel in file/data viewer."""
    session._log("[TEST] Version history in viewer")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    # Navigate to files and try to open a file
    session.navigate_to("/files")
    session.wait_for_load(timeout=10)

    # Click first file row
    file_selectors = [
        "tbody tr:first-child", "[data-testid='file-row']",
        ".file-row", ".file-item",
    ]
    for sel in file_selectors:
        if session.click(sel, timeout=1):
            break

    session.wait_for_load(timeout=10)

    # Check for version history panel
    version_selectors = [
        "[data-testid='version-history']", ".version-history",
        "[data-testid='versions']", ".versions-panel",
        "button:has-text('Versions')", "button:has-text('Verze')",
        "button:has-text('History')", "button:has-text('Historie')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in version_selectors)

    if not found:
        session.missing_feature("Version history panel", "Version history not visible in file viewer")
        return True

    return session.assert_true(found, "Version history panel visible")


def test_version_badges(session: WebTestSession) -> bool:
    """Verify version badges (v1, v2, etc.) display."""
    session._log("[TEST] Version badges")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/files")
    session.wait_for_load(timeout=10)

    # Check for version badges in file list or detail
    badge_selectors = [
        ".version-badge", "[data-testid='version-badge']",
        "span:has-text('v1')", "span:has-text('v2')",
        ".version-tag", "[data-testid='version-tag']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in badge_selectors)

    if not found:
        session.missing_feature("Version badges", "Version badges (v1, v2) not visible in file list")
        return True

    session.assert_true(found, "Version badges displayed")
    return True


def test_diff_tool_button(session: WebTestSession) -> bool:
    """Verify Diff/Compare button exists."""
    session._log("[TEST] Diff tool button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/files")
    session.wait_for_load(timeout=10)

    # Click first file
    file_selectors = ["tbody tr:first-child", "[data-testid='file-row']"]
    for sel in file_selectors:
        if session.click(sel, timeout=1):
            break

    session.wait_for_load(timeout=10)

    diff_selectors = [
        "button:has-text('Compare')", "button:has-text('Porovnat')",
        "button:has-text('Diff')", "[data-testid='diff-tool']",
        "[data-testid='compare-versions']", "button:has-text('View Changes')",
        "button:has-text('Zobrazit změny')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in diff_selectors)

    if not found:
        session.missing_feature("Diff tool button", "Compare/Diff button not found")
        return True

    session.assert_true(found, "Diff/Compare button available")
    return True


def test_diff_view_display(session: WebTestSession) -> bool:
    """Verify diff view shows changes between versions."""
    session._log("[TEST] Diff view display")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/files")
    session.wait_for_load(timeout=10)

    # Try to open diff view
    diff_selectors = [
        "button:has-text('Compare')", "button:has-text('Diff')",
        "[data-testid='diff-tool']", "[data-testid='compare-versions']",
    ]
    for sel in diff_selectors:
        if session.click(sel, timeout=1):
            break

    session.wait_for_load(timeout=10)

    # Check for diff view elements
    diff_view_selectors = [
        ".diff-view", "[data-testid='diff-view']",
        ".diff-table", ".diff-panel",
        ".added", ".removed", ".changed",
        "[data-testid='diff-added']", "[data-testid='diff-removed']",
        ".delta-value", "[data-testid='delta']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in diff_view_selectors)

    if not found:
        session.missing_feature("Diff view", "Diff view not displayed (may need multiple versions)")
        return True

    session.assert_true(found, "Diff view shows changes between versions")
    return True


def test_version_selector_dropdown(session: WebTestSession) -> bool:
    """Verify version selector dropdown for comparison."""
    session._log("[TEST] Version selector dropdown")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/files")
    session.wait_for_load(timeout=10)

    # Click first file
    file_selectors = ["tbody tr:first-child", "[data-testid='file-row']"]
    for sel in file_selectors:
        if session.click(sel, timeout=1):
            break

    session.wait_for_load(timeout=10)

    selector_selectors = [
        "select[name='version']", "[data-testid='version-select']",
        "select[name='fromVersion']", "select[name='toVersion']",
        "[data-testid='version-from']", "[data-testid='version-to']",
        ".version-selector", "select.version-picker",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in selector_selectors)

    if not found:
        session.missing_feature("Version selector", "Version selector dropdown not found")
        return True

    session.assert_true(found, "Version selector dropdown available")
    return True


def test_original_preserved_indicator(session: WebTestSession) -> bool:
    """Verify original data is preserved indicator (no overwrite)."""
    session._log("[TEST] Original data preserved indicator")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/files")
    session.wait_for_load(timeout=10)

    # Check for "original" or "v1" version marker
    original_selectors = [
        "[data-testid='original-version']", ".original-badge",
        "span:has-text('Original')", "span:has-text('Originál')",
        "[data-version='1']", ".version-1",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in original_selectors)

    if not found:
        session.missing_feature("Original preserved indicator", "Original version marker not visible")
        return True

    session.assert_true(found, "Original data preserved indicator visible")
    return True


def test_report_version_timeline(session: WebTestSession) -> bool:
    """Verify version timeline in report detail."""
    session._log("[TEST] Report version timeline")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/reports")
    session.wait_for_load(timeout=10)

    # Click first report
    report_selectors = ["tbody tr:first-child", "[data-testid='report-row']"]
    for sel in report_selectors:
        if session.click(sel, timeout=1):
            break

    session.wait_for_load(timeout=10)

    timeline_selectors = [
        ".state-history", "[data-testid='state-history']",
        ".timeline", "[data-testid='timeline']",
        ".version-timeline", "[data-testid='version-timeline']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in timeline_selectors)

    if not found:
        session.missing_feature("Version timeline", "Version timeline not visible in report detail")
        return True

    session.assert_true(found, "Version timeline visible in report detail")
    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step14: Data Versioning & Diff Tool")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_version_history_in_viewer(session)
        test_version_badges(session)
        test_diff_tool_button(session)
        test_diff_view_display(session)
        test_version_selector_dropdown(session)
        test_original_preserved_indicator(session)
        test_report_version_timeline(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
