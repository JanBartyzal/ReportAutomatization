# Web UAT Test: Step22 - Period Comparison UI (FS22)
# Verifies: comparison page, period/org filters, visualization surface

import os
import sys

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step22_Period_Comparison"


def _login_admin(session: WebTestSession) -> None:
    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)


def test_comparison_page_accessible(session: WebTestSession) -> bool:
    session._log("[TEST] Period comparison page is accessible")
    _login_admin(session)
    session.navigate_to("/comparison")
    session.wait_for_load(timeout=10)

    selectors = [
        "h1:has-text('Comparison')",
        "h2:has-text('Comparison')",
        "h1:has-text('Porovn')",
        "button:has-text('Compare')",
        "[data-testid='period-comparison']",
        ".period-comparison",
    ]
    found = any(session.is_visible(sel, timeout=1) for sel in selectors)
    return session.assert_true(found and "comparison" in session.driver.current_url.lower(), "Comparison page renders")


def test_comparison_filters_present(session: WebTestSession) -> bool:
    session._log("[TEST] Period comparison filters")
    _login_admin(session)
    session.navigate_to("/comparison")
    session.wait_for_load(timeout=10)

    period_controls = session.find_elements("select", timeout=1) + session.find_elements("input", timeout=1)
    has_period_text = any(
        session.is_visible(sel, timeout=1)
        for sel in ["span:has-text('Period')", "label:has-text('Period')", "span:has-text('Obdob')"]
    )
    has_org_text = any(
        session.is_visible(sel, timeout=1)
        for sel in ["span:has-text('Organization')", "label:has-text('Organization')", "span:has-text('Org')"]
    )

    session.assert_true(bool(period_controls) or has_period_text, "Comparison exposes period filter/control")
    if has_org_text or len(period_controls) >= 2:
        session.assert_true(True, "Comparison exposes organization or secondary dimension control")
    else:
        session.missing_feature("Comparison dimension filter", "Organization/secondary dimension filter not visible")
    return True


def test_comparison_visualization_surface(session: WebTestSession) -> bool:
    session._log("[TEST] Period comparison visualization surface")
    _login_admin(session)
    session.navigate_to("/comparison")
    session.wait_for_load(timeout=10)

    visual_found = any(session.is_element_present(sel, timeout=1) for sel in [
        ".recharts-wrapper",
        ".nivo-chart",
        ".chart-container",
        "svg",
        "canvas",
        "table",
        "[role='grid']",
    ])
    if visual_found:
        session.assert_true(True, "Comparison has chart/table visualization surface")
    else:
        session.missing_feature("Comparison visualization", "No chart or table surface visible; may require seeded data")
    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)
    session._log("[INFO] Web UAT Tests - Step22: Period Comparison UI")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_comparison_page_accessible(session)
        test_comparison_filters_present(session)
        test_comparison_visualization_surface(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
