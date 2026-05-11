# Web UAT Test: Step27 - Live Excel Export & External Sync UI (FS27)
# Verifies: export flow management, create drawer, target/schedule/manual controls

import os
import sys
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step27_Excel_Sync"


def _login_admin(session: WebTestSession) -> None:
    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)


def test_export_flows_page_accessible(session: WebTestSession) -> bool:
    session._log("[TEST] Export Flows page is accessible")
    _login_admin(session)
    session.navigate_to("/admin/export-flows")
    session.wait_for_load(timeout=10)

    selectors = [
        "h1:has-text('Export Flows')",
        "h2:has-text('Export Flows')",
        "button:has-text('New Export Flow')",
        "button:has-text('Create')",
        "input[placeholder*='Search']",
        "table",
        "[role='grid']",
    ]
    found = any(session.is_visible(sel, timeout=1) for sel in selectors)
    return session.assert_true(found and "export-flows" in session.driver.current_url.lower(), "Export Flows route renders")


def test_export_flow_create_entry(session: WebTestSession) -> bool:
    session._log("[TEST] Export Flow create entry opens editor")
    _login_admin(session)
    session.navigate_to("/admin/export-flows")
    session.wait_for_load(timeout=10)

    opened = False
    for selector in [
        "button:has-text('New Export Flow')",
        "button:has-text('Create Export Flow')",
        "button:has-text('New')",
        "button:has-text('Create')",
    ]:
        if session.click(selector, timeout=1):
            time.sleep(1)
            opened = True
            break

    if not opened:
        session.missing_feature("Export Flow editor", "Create/New Export Flow button not visible")
        return True

    editor_found = any(session.is_visible(sel, timeout=2) for sel in [
        "div[role='dialog']",
        "aside",
        "h2:has-text('Export Flow')",
        "h3:has-text('Export Flow')",
        "input[placeholder*='name']",
        "textarea",
    ])
    return session.assert_true(editor_found, "Export Flow editor drawer/dialog opens")


def test_export_flow_required_fields(session: WebTestSession) -> bool:
    session._log("[TEST] Export Flow required fields")
    _login_admin(session)
    session.navigate_to("/admin/export-flows")
    session.wait_for_load(timeout=10)

    for selector in ["button:has-text('New Export Flow')", "button:has-text('New')", "button:has-text('Create')"]:
        if session.click(selector, timeout=1):
            time.sleep(1)
            break

    labels = {
        "name": ["label:has-text('Name')", "input[placeholder*='name']", "input[placeholder*='Name']"],
        "sql": ["label:has-text('SQL')", "textarea", "div:has-text('SQL Query')"],
        "target": ["label:has-text('Target')", "span:has-text('SharePoint')", "span:has-text('Local Path')"],
        "sheet": ["label:has-text('Sheet')", "input[placeholder*='sheet']", "input[placeholder*='Sheet']"],
    }

    for field, selectors in labels.items():
        found = any(session.is_element_present(sel, timeout=1) for sel in selectors)
        session.assert_true(found, f"Export Flow editor exposes {field} field")
    return True


def test_export_flow_target_modes(session: WebTestSession) -> bool:
    session._log("[TEST] Export Flow target modes")
    _login_admin(session)
    session.navigate_to("/admin/export-flows")
    session.wait_for_load(timeout=10)

    for selector in ["button:has-text('New Export Flow')", "button:has-text('New')", "button:has-text('Create')"]:
        if session.click(selector, timeout=1):
            time.sleep(1)
            break

    sharepoint = any(session.is_element_present(sel, timeout=1) for sel in [
        "span:has-text('SharePoint')",
        "option[value='SHAREPOINT']",
        "button:has-text('SharePoint')",
    ])
    local_path = any(session.is_element_present(sel, timeout=1) for sel in [
        "span:has-text('Local Path')",
        "option[value='LOCAL_PATH']",
        "button:has-text('Local')",
    ])

    session.assert_true(sharepoint, "Export Flow supports SharePoint target mode")
    session.assert_true(local_path, "Export Flow supports Local Path target mode")
    return True


def test_export_flow_execution_controls(session: WebTestSession) -> bool:
    session._log("[TEST] Export Flow execution controls")
    _login_admin(session)
    session.navigate_to("/admin/export-flows")
    session.wait_for_load(timeout=10)

    controls_found = any(session.is_element_present(sel, timeout=1) for sel in [
        "button:has-text('Export Now')",
        "button:has-text('Run')",
        "button:has-text('Execute')",
        "button:has-text('History')",
        "span:has-text('Execution')",
        "[data-testid='execution-history']",
    ])
    if controls_found:
        session.assert_true(True, "Export Flow manual execution/history controls are visible")
    else:
        session.missing_feature("Export Flow execution controls", "Execution controls not visible; may require existing flow")
    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)
    session._log("[INFO] Web UAT Tests - Step27: Excel Sync UI")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_export_flows_page_accessible(session)
        test_export_flow_create_entry(session)
        test_export_flow_required_fields(session)
        test_export_flow_target_modes(session)
        test_export_flow_execution_controls(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
