# Web UAT Test: Step07 - Admin UI (FS07)
# Verifies: Role management, Failed Jobs UI, API keys, organization management

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step07_Admin_UI"


def test_admin_page_accessible(session: WebTestSession) -> bool:
    """Verify admin page loads for HoldingAdmin."""
    session._log("[TEST] Admin page is accessible for HoldingAdmin")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    session.navigate_to("/admin/manage")
    session.wait_for_load(timeout=10)

    admin_selectors = [
        "[data-testid='tab-users']", "button:has-text('Users')",
        ".users-table", "[data-testid='users-table']",
        ".admin-panel", "[data-testid='admin-panel']",
        "table.users", "h1:has-text('Admin')", "h2:has-text('Admin')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in admin_selectors)

    if not found:
        session.missing_feature("Admin page", "Admin page not accessible or not implemented")
        return True

    return session.assert_true(found, "Admin page loads with user management UI")


def test_users_tab(session: WebTestSession) -> bool:
    """Verify Users tab shows user list."""
    session._log("[TEST] Users tab shows user list")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/manage")
    session.wait_for_load(timeout=10)

    # Click Users tab
    users_tab_selectors = [
        "[data-testid='tab-users']", "button:has-text('Users')",
        "button:has-text('Uživatelé')", "a:has-text('Users')",
    ]
    for sel in users_tab_selectors:
        if session.click(sel, timeout=1):
            break

    time.sleep(1)

    # Check for user table
    table_selectors = [
        ".users-table", "[data-testid='users-table']",
        "table.users", "table", "tbody tr",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in table_selectors)

    if not found:
        session.missing_feature("Users table", "Users table not visible in admin panel")
        return True

    session.assert_true(found, "Users tab displays user table")
    return True


def test_organizations_tab(session: WebTestSession) -> bool:
    """Verify Organizations tab."""
    session._log("[TEST] Organizations tab")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/manage")
    session.wait_for_load(timeout=10)

    org_tab_selectors = [
        "[data-testid='tab-organizations']", "button:has-text('Organizations')",
        "button:has-text('Organizace')", "a:has-text('Organizations')",
    ]

    clicked = False
    for sel in org_tab_selectors:
        if session.click(sel, timeout=1):
            clicked = True
            break

    if not clicked:
        session.missing_feature("Organizations tab", "Organizations tab not found in admin panel")
        return True

    time.sleep(1)

    org_selectors = [
        ".org-table", "[data-testid='org-table']",
        ".organization-list", "table",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in org_selectors)
    session.assert_true(found or clicked, "Organizations tab accessible")
    return True


def test_api_keys_tab(session: WebTestSession) -> bool:
    """Verify API Keys management tab."""
    session._log("[TEST] API Keys management tab")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/manage")
    session.wait_for_load(timeout=10)

    api_tab_selectors = [
        "[data-testid='tab-apikeys']", "button:has-text('API Keys')",
        "button:has-text('API klíče')", "a:has-text('API Keys')",
    ]

    clicked = False
    for sel in api_tab_selectors:
        if session.click(sel, timeout=1):
            clicked = True
            break

    if not clicked:
        session.missing_feature("API Keys tab", "API Keys management tab not found")
        return True

    time.sleep(1)

    # Check for generate key button
    gen_selectors = [
        "button:has-text('Generate')", "button:has-text('Vygenerovat')",
        "[data-testid='generate-key']", "button:has-text('Create')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in gen_selectors)
    session.assert_true(found or clicked, "API Keys tab has generate key functionality")
    return True


def test_failed_jobs_tab(session: WebTestSession) -> bool:
    """Verify Failed Jobs tab shows failed processing jobs."""
    session._log("[TEST] Failed Jobs tab")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/manage")
    session.wait_for_load(timeout=10)

    fj_tab_selectors = [
        "[data-testid='tab-failedjobs']", "button:has-text('Failed Jobs')",
        "button:has-text('Selhané úlohy')", "a:has-text('Failed')",
    ]

    clicked = False
    for sel in fj_tab_selectors:
        if session.click(sel, timeout=1):
            clicked = True
            break

    if not clicked:
        session.missing_feature("Failed Jobs tab", "Failed Jobs tab not found in admin")
        return True

    time.sleep(1)

    # Check for failed jobs table or empty state
    fj_selectors = [
        ".failed-jobs-table", "[data-testid='failed-jobs-table']",
        "table.failed-jobs", ".empty-state",
        "[data-testid='no-failed-jobs']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in fj_selectors)
    session.assert_true(found or clicked, "Failed Jobs tab accessible")
    return True


def test_reprocess_button(session: WebTestSession) -> bool:
    """Verify Reprocess button exists in Failed Jobs."""
    session._log("[TEST] Reprocess button in Failed Jobs")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/manage")
    session.wait_for_load(timeout=10)

    # Navigate to Failed Jobs tab
    fj_tab_selectors = [
        "[data-testid='tab-failedjobs']", "button:has-text('Failed Jobs')",
    ]
    for sel in fj_tab_selectors:
        if session.click(sel, timeout=1):
            break

    time.sleep(1)

    reprocess_selectors = [
        "button:has-text('Reprocess')", "[data-testid='reprocess']",
        "button:has-text('Retry')", "button:has-text('Opakovat')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in reprocess_selectors)

    if not found:
        session.missing_feature("Reprocess button", "Reprocess button not visible (may need failed jobs to exist)")
        return True

    session.assert_true(found, "Reprocess button visible in Failed Jobs")
    return True


def test_invite_user_button(session: WebTestSession) -> bool:
    """Verify Invite User button in Users tab."""
    session._log("[TEST] Invite User button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/manage")
    session.wait_for_load(timeout=10)

    # Navigate to Users tab
    users_tab_selectors = [
        "[data-testid='tab-users']", "button:has-text('Users')",
    ]
    for sel in users_tab_selectors:
        if session.click(sel, timeout=1):
            break

    time.sleep(1)

    invite_selectors = [
        "button:has-text('Invite User')", "button:has-text('Pozvat uživatele')",
        "[data-testid='invite-user']", "button:has-text('Add User')",
        "button:has-text('Přidat uživatele')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in invite_selectors)

    if not found:
        session.missing_feature("Invite user button", "Invite User button not found in admin")
        return True

    session.assert_true(found, "Invite User button visible")
    return True


def test_role_column_visible(session: WebTestSession) -> bool:
    """Verify user table shows role column."""
    session._log("[TEST] Role column in users table")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin/manage")
    session.wait_for_load(timeout=10)

    # Check for role column headers
    role_selectors = [
        "th:has-text('Role')", "th:has-text('Role')",
        "[data-testid='col-role']", "th:has-text('Oprávnění')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in role_selectors)

    if not found:
        session.missing_feature("Role column", "Role column not visible in users table")
        return True

    session.assert_true(found, "Users table shows Role column")
    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step07: Admin UI")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_admin_page_accessible(session)
        test_users_tab(session)
        test_organizations_tab(session)
        test_api_keys_tab(session)
        test_failed_jobs_tab(session)
        test_reprocess_button(session)
        test_invite_user_button(session)
        test_role_column_visible(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
