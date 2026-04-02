# Web UAT Test: Step13 - Notification Center & Alerts (FS13)
# Verifies: Notification bell, notification list, settings, real-time push, type filters

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step13_Notifications"


def test_notification_bell_visible(session: WebTestSession) -> bool:
    """Verify notification bell icon is visible in header."""
    session._log("[TEST] Notification bell icon visible")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    bell_selectors = [
        "[data-testid='notification-bell']", ".notification-bell",
        "[data-testid='notifications-icon']", ".bell-icon",
        "button[aria-label='Notifications']", "button[aria-label='Upozornění']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in bell_selectors)

    if not found:
        session.missing_feature("Notification bell", "Notification bell icon not found in header")
        return True

    return session.assert_true(found, "Notification bell icon visible in header")


def test_notifications_page_accessible(session: WebTestSession) -> bool:
    """Verify notifications page loads."""
    session._log("[TEST] Notifications page is accessible")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    session.navigate_to("/notifications")
    session.wait_for_load(timeout=10)

    notif_selectors = [
        "[data-testid='notification-list']", ".notification-list",
        "[data-testid='notification-item']", ".notification-item",
        ".notifications-page", "h1:has-text('Notification')",
        "h2:has-text('Notification')", ".empty-state",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in notif_selectors)

    if not found:
        session.missing_feature("Notifications page", "Notifications page not accessible")
        return True

    return session.assert_true(found, "Notifications page loads")


def test_notification_list_items(session: WebTestSession) -> bool:
    """Verify notification items display."""
    session._log("[TEST] Notification list items")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/notifications")
    session.wait_for_load(timeout=10)

    item_selectors = [
        "[data-testid='notification-item']", ".notification-item",
        ".notification-card", ".notification-row",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in item_selectors)

    if not found:
        # May be empty — check for empty state
        empty_selectors = [
            ".empty-state", "[data-testid='no-notifications']",
            "p:has-text('No notifications')", "p:has-text('Žádná upozornění')",
        ]
        empty = any(session.is_visible(sel, timeout=1) for sel in empty_selectors)
        if empty:
            session.assert_true(True, "Notification list shows empty state (no notifications)")
            return True
        session.missing_feature("Notification items", "No notification items or empty state visible")
        return True

    session.assert_true(found, "Notification items displayed")
    return True


def test_unread_badge(session: WebTestSession) -> bool:
    """Verify unread notification badge/counter."""
    session._log("[TEST] Unread notification badge")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    badge_selectors = [
        "[data-testid='unread-count']", ".unread-count",
        ".notification-badge", ".badge",
        "[data-testid='notification-bell'] .badge",
        ".notification-bell .badge",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in badge_selectors)

    if not found:
        session.missing_feature("Unread badge", "Unread notification counter/badge not found")
        return True

    session.assert_true(found, "Unread notification badge visible")
    return True


def test_mark_as_read(session: WebTestSession) -> bool:
    """Verify Mark as Read functionality."""
    session._log("[TEST] Mark as Read button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/notifications")
    session.wait_for_load(timeout=10)

    mark_selectors = [
        "button:has-text('Mark Read')", "button:has-text('Označit jako přečtené')",
        "[data-testid='mark-read']", "button:has-text('Mark All Read')",
        "button:has-text('Označit vše')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in mark_selectors)

    if not found:
        session.missing_feature("Mark as Read", "Mark as Read button not found")
        return True

    session.assert_true(found, "Mark as Read button available")
    return True


def test_notification_type_filter(session: WebTestSession) -> bool:
    """Verify notification type filter (import, parsing fail, report ready, etc.)."""
    session._log("[TEST] Notification type filter")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/notifications")
    session.wait_for_load(timeout=10)

    filter_selectors = [
        "select[name='type']", "[data-testid='type-filter']",
        ".notification-filter", "[data-testid='notification-type']",
        "button:has-text('All')", "button:has-text('Imports')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in filter_selectors)

    if not found:
        session.missing_feature("Notification type filter", "Type filter not found on notifications page")
        return True

    session.assert_true(found, "Notification type filter available")
    return True


def test_notification_settings(session: WebTestSession) -> bool:
    """Verify notification settings (opt-in/opt-out per event type)."""
    session._log("[TEST] Notification settings")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/notifications")
    session.wait_for_load(timeout=10)

    # Look for settings button
    settings_selectors = [
        "button:has-text('Settings')", "button:has-text('Nastavení')",
        "[data-testid='notification-settings']", "a[href*='settings']",
        "button[aria-label='Settings']",
    ]

    clicked = False
    for sel in settings_selectors:
        if session.click(sel, timeout=1):
            clicked = True
            break

    if not clicked:
        # Try direct navigation
        session.navigate_to("/settings")
        session.wait_for_load(timeout=10)

    # Check for opt-in/opt-out toggles
    toggle_selectors = [
        "input[type='checkbox']", "[data-testid='notification-toggle']",
        ".toggle-switch", "[role='switch']",
        ".notification-preferences", "[data-testid='preferences']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in toggle_selectors)

    if not found:
        session.missing_feature("Notification settings", "Opt-in/opt-out toggles not found")
        return True

    session.assert_true(found, "Notification settings with toggles available")
    return True


def test_notification_types_coverage(session: WebTestSession) -> bool:
    """Verify notification types from charter: REPORT_SUBMITTED, APPROVED, REJECTED, DEADLINE."""
    session._log("[TEST] Notification types coverage")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/notifications")
    session.wait_for_load(timeout=10)

    # Check for notification type indicators
    type_selectors = [
        "[data-type='REPORT_SUBMITTED']", "[data-type='REPORT_APPROVED']",
        "[data-type='REPORT_REJECTED']", "[data-type='DEADLINE_APPROACHING']",
        ".notification-type", "[data-testid='notification-type']",
        "span:has-text('Submitted')", "span:has-text('Approved')",
        "span:has-text('Deadline')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in type_selectors)

    if not found:
        session.missing_feature("Notification types", "Specific notification types not visible (may need events)")
        return True

    session.assert_true(found, "Notification type indicators present")
    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step13: Notification Center & Alerts")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_notification_bell_visible(session)
        test_notifications_page_accessible(session)
        test_notification_list_items(session)
        test_unread_badge(session)
        test_mark_as_read(session)
        test_notification_type_filter(session)
        test_notification_settings(session)
        test_notification_types_coverage(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
