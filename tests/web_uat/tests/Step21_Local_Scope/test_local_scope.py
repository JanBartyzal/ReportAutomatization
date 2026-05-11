# Web UAT Test: Step21 - Local Scope UI (FS21)
# Verifies: local dashboard, local forms/templates entry points, scope indicators

import os
import sys
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step21_Local_Scope"


def _login_admin(session: WebTestSession) -> None:
    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)


def test_local_dashboard_accessible(session: WebTestSession) -> bool:
    session._log("[TEST] Local dashboard is accessible")
    _login_admin(session)
    session.navigate_to("/local")
    session.wait_for_load(timeout=10)

    selectors = [
        "h1:has-text('Local')",
        "h2:has-text('Local')",
        "h3:has-text('Local')",
        "button:has-text('Local Form')",
        "button:has-text('Local Template')",
        "[role='tab']",
    ]
    found = any(session.is_visible(sel, timeout=1) for sel in selectors)
    return session.assert_true(found and "local" in session.driver.current_url.lower(), "Local dashboard route renders")


def test_local_creation_entry_points(session: WebTestSession) -> bool:
    session._log("[TEST] Local creation entry points")
    _login_admin(session)
    session.navigate_to("/local")
    session.wait_for_load(timeout=10)

    form_entry = any(session.is_visible(sel, timeout=1) for sel in [
        "button:has-text('Create Local Form')",
        "button:has-text('New Local Form')",
        "button:has-text('Local Form')",
        "a[href*='/forms/new?scope=LOCAL']",
    ])
    template_entry = any(session.is_visible(sel, timeout=1) for sel in [
        "button:has-text('Create Local Template')",
        "button:has-text('New Local Template')",
        "button:has-text('Local Template')",
        "a[href*='/templates/new?scope=LOCAL']",
    ])

    session.assert_true(form_entry, "Local form creation entry is present")
    session.assert_true(template_entry, "Local template creation entry is present")
    return True


def test_local_tabs_and_scope_badges(session: WebTestSession) -> bool:
    session._log("[TEST] Local tabs and scope badges")
    _login_admin(session)
    session.navigate_to("/local")
    session.wait_for_load(timeout=10)

    tabs_found = any(session.is_visible(sel, timeout=1) for sel in [
        "[role='tab']",
        "button:has-text('Forms')",
        "button:has-text('Templates')",
    ])
    session.assert_true(tabs_found, "Local dashboard exposes Forms/Templates tabs")

    local_marker_found = any(session.is_element_present(sel, timeout=1) for sel in [
        "span:has-text('LOCAL')",
        "span:has-text('Local')",
        "[data-testid='scope-badge']",
        ".scope-badge",
    ])
    if local_marker_found:
        session.assert_true(True, "Local scope marker is visible")
    else:
        session.missing_feature("Local scope marker", "No local scope badge visible; page may be empty")
    return True


def test_holding_admin_overview_accessible(session: WebTestSession) -> bool:
    session._log("[TEST] Holding admin overview shows local/shared items")
    _login_admin(session)
    session.navigate_to("/admin/holding")
    session.wait_for_load(timeout=10)

    selectors = [
        "h1:has-text('Holding')",
        "h2:has-text('Holding')",
        "span:has-text('Forms')",
        "span:has-text('Templates')",
        "[role='tab']",
    ]
    found = any(session.is_visible(sel, timeout=1) for sel in selectors)
    return session.assert_true(found, "Holding admin overview is accessible")


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)
    session._log("[INFO] Web UAT Tests - Step21: Local Scope UI")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_local_dashboard_accessible(session)
        test_local_creation_entry_points(session)
        test_local_tabs_and_scope_badges(session)
        test_holding_admin_overview_accessible(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
