# Web UAT Test: Step99 - Cross-page UX Quality Smoke Checks (FS99 / UX)
# Verifies: responsive layout, navigation landmarks, basic accessible control names

import os
import sys
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step99_UX_Quality"

CORE_ROUTES = [
    "/dashboard",
    "/files",
    "/reports",
    "/forms",
    "/dashboards",
    "/sinks",
    "/admin/export-flows",
]


def _login_admin(session: WebTestSession) -> None:
    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)


def _visible_text_not_empty(session: WebTestSession) -> bool:
    body_text = session.get_text("body", timeout=2).strip()
    return len(body_text) >= 20


def test_core_routes_have_content(session: WebTestSession) -> bool:
    session._log("[TEST] Core routes render meaningful content")
    _login_admin(session)

    all_ok = True
    for route in CORE_ROUTES:
        session.navigate_to(route)
        session.wait_for_load(timeout=10)
        has_content = _visible_text_not_empty(session)
        no_404 = "404" not in session.get_text("body", timeout=1).lower()
        all_ok = session.assert_true(has_content and no_404, f"Route {route} renders content") and all_ok
    return all_ok


def test_global_navigation_landmarks(session: WebTestSession) -> bool:
    session._log("[TEST] Global navigation landmarks")
    _login_admin(session)
    session.navigate_to("/dashboard")
    session.wait_for_load(timeout=10)

    has_nav = any(session.is_element_present(sel, timeout=1) for sel in [
        "nav",
        "[role='navigation']",
        "aside",
        "a[href='/dashboard']",
        "a[href='/files']",
    ])
    has_main = any(session.is_element_present(sel, timeout=1) for sel in [
        "main",
        "[role='main']",
    ])
    session.assert_true(has_nav, "App exposes navigation landmark/sidebar")
    session.assert_true(has_main, "App exposes main content landmark")
    return True


def test_global_search_accessible(session: WebTestSession) -> bool:
    session._log("[TEST] Global search accessible")
    _login_admin(session)
    session.navigate_to("/dashboard")
    session.wait_for_load(timeout=10)

    search_found = any(session.is_element_present(sel, timeout=1) for sel in [
        "input[type='search']",
        "input[placeholder*='Search']",
        "input[aria-label*='Search']",
        "[role='searchbox']",
    ])
    if search_found:
        session.assert_true(True, "Global search input is accessible from app shell")
    else:
        session.missing_feature("Global search", "Global search input not visible on dashboard shell")
    return True


def test_mobile_viewport_has_no_horizontal_overflow(session: WebTestSession) -> bool:
    session._log("[TEST] Mobile viewport has no horizontal overflow on core pages")
    _login_admin(session)

    try:
        session.driver.set_window_size(390, 844)
    except Exception as exc:
        session._log(f"[WARN] Could not set mobile viewport: {exc}")

    overflow_routes = []
    for route in ["/dashboard", "/forms", "/reports", "/sinks"]:
        session.navigate_to(route)
        session.wait_for_load(timeout=10)
        time.sleep(0.5)
        metrics = session.execute_script(
            "return {sw: document.documentElement.scrollWidth, cw: document.documentElement.clientWidth};"
        ) or {}
        scroll_width = int(metrics.get("sw") or 0)
        client_width = int(metrics.get("cw") or 0)
        if scroll_width and client_width and scroll_width > client_width + 8:
            overflow_routes.append(f"{route} ({scroll_width}>{client_width})")

    return session.assert_true(not overflow_routes, "Mobile core routes avoid horizontal overflow: " + ", ".join(overflow_routes))


def test_buttons_and_inputs_have_accessible_names(session: WebTestSession) -> bool:
    session._log("[TEST] Visible controls have accessible names")
    _login_admin(session)
    session.navigate_to("/dashboard")
    session.wait_for_load(timeout=10)

    unnamed = []
    controls = session.driver.find_elements(By.CSS_SELECTOR, "button, input, select, textarea")
    for control in controls[:60]:
        try:
            if not control.is_displayed():
                continue
            label = (
                (control.text or "").strip()
                or (control.get_attribute("aria-label") or "").strip()
                or (control.get_attribute("title") or "").strip()
                or (control.get_attribute("placeholder") or "").strip()
                or (control.get_attribute("name") or "").strip()
            )
            if not label:
                unnamed.append(control.tag_name)
        except Exception:
            continue

    return session.assert_true(len(unnamed) <= 2, f"Visible controls mostly have accessible names (unnamed={len(unnamed)})")


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)
    session._log("[INFO] Web UAT Tests - Step99: UX Quality")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_core_routes_have_content(session)
        test_global_navigation_landmarks(session)
        test_global_search_accessible(session)
        test_mobile_viewport_has_no_horizontal_overflow(session)
        test_buttons_and_inputs_have_accessible_names(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
