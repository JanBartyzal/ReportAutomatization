# Web UAT Test: Step23 - Integrations UI (FS23)
# Verifies: ServiceNow integration UI, schedules/history, project dashboard access

import os
import sys
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step23_Integrations_UI"


def _login_admin(session: WebTestSession) -> None:
    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)


def test_integrations_page_accessible(session: WebTestSession) -> bool:
    session._log("[TEST] Integrations page is accessible")
    _login_admin(session)
    session.navigate_to("/admin/integrations")
    session.wait_for_load(timeout=10)

    selectors = [
        "h1:has-text('Integration')",
        "h2:has-text('Integration')",
        "h1:has-text('Service')",
        "h2:has-text('Service')",
        "[role='tab']",
        "button:has-text('New Connection')",
    ]
    found = any(session.is_visible(sel, timeout=1) for sel in selectors)
    return session.assert_true(found, "ServiceNow integrations page renders")


def test_integrations_tabs(session: WebTestSession) -> bool:
    session._log("[TEST] Integrations tabs")
    _login_admin(session)
    session.navigate_to("/admin/integrations")
    session.wait_for_load(timeout=10)

    labels = ["Connections", "Schedules", "Sync History"]
    found = 0
    for label in labels:
        if any(session.is_visible(sel, timeout=1) for sel in [
            f"button:has-text('{label}')",
            f"span:has-text('{label}')",
            f"div:has-text('{label}')",
        ]):
            found += 1
    return session.assert_true(found >= 2, f"Integration tabs visible ({found}/3)")


def test_connection_dialog_fields(session: WebTestSession) -> bool:
    session._log("[TEST] ServiceNow connection dialog fields")
    _login_admin(session)
    session.navigate_to("/admin/integrations")
    session.wait_for_load(timeout=10)

    opened = False
    for selector in [
        "button:has-text('New Connection')",
        "button:has-text('Add Connection')",
        "button:has-text('Create Connection')",
        "button:has-text('New')",
    ]:
        if session.click(selector, timeout=1):
            opened = True
            time.sleep(1)
            break

    if not opened:
        session.missing_feature("Connection dialog", "New Connection button not found")
        return True

    has_instance = any(session.is_element_present(sel, timeout=1) for sel in [
        "input[placeholder*='service-now']",
        "input[placeholder*='ServiceNow']",
        "input[name='instanceUrl']",
    ])
    has_credentials = any(session.is_element_present(sel, timeout=1) for sel in [
        "input[placeholder*='credentials']",
        "input[name='credentialsRef']",
        "input[placeholder*='credential']",
    ])
    has_table = any(session.is_element_present(sel, timeout=1) for sel in [
        "input[placeholder*='table']",
        "input[name='tableName']",
        "label:has-text('Tables')",
    ])

    session.assert_true(has_instance, "Connection dialog has ServiceNow instance URL field")
    session.assert_true(has_credentials, "Connection dialog has credentials reference field")
    if has_table:
        session.assert_true(True, "Connection dialog exposes table mapping/sync configuration")
    else:
        session.missing_feature("ServiceNow table mapping", "Table sync mapping fields not visible")
    return True


def test_projects_page_accessible(session: WebTestSession) -> bool:
    session._log("[TEST] ServiceNow projects page is accessible")
    _login_admin(session)
    session.navigate_to("/projects")
    session.wait_for_load(timeout=10)

    selectors = [
        "h1:has-text('Projects')",
        "h2:has-text('Projects')",
        "table",
        "[role='grid']",
        "button:has-text('Refresh')",
    ]
    found = any(session.is_visible(sel, timeout=1) for sel in selectors)
    return session.assert_true(found, "Projects page renders")


def test_projects_kpi_columns(session: WebTestSession) -> bool:
    session._log("[TEST] Project KPI columns")
    _login_admin(session)
    session.navigate_to("/projects")
    session.wait_for_load(timeout=10)

    expected = ["Budget", "Schedule", "Milestone", "RAG"]
    found = 0
    for label in expected:
        if any(session.is_visible(sel, timeout=1) for sel in [
            f"th:has-text('{label}')",
            f"span:has-text('{label}')",
            f"div:has-text('{label}')",
        ]):
            found += 1
    return session.assert_true(found >= 2, f"Projects table exposes KPI columns ({found}/4)")


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)
    session._log("[INFO] Web UAT Tests - Step23: Integrations UI")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_integrations_page_accessible(session)
        test_integrations_tabs(session)
        test_connection_dialog_fields(session)
        test_projects_page_accessible(session)
        test_projects_kpi_columns(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
