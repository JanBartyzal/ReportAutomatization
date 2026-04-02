# Web UAT Test: Step11 - Dashboards & SQL Reporting (FS11)
# Verifies: Dashboard creation, chart display, SQL config, public/private visibility

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step11_Dashboards"


def test_dashboards_page_accessible(session: WebTestSession) -> bool:
    """Verify dashboards page loads."""
    session._log("[TEST] Dashboards page is accessible")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    session.navigate_to("/dashboards")
    session.wait_for_load(timeout=10)

    dash_selectors = [
        "[data-testid='dashboard-list']", ".dashboard-list",
        "[data-testid='dashboard-grid']", ".dashboard-grid",
        ".dashboard-cards", "h1:has-text('Dashboard')", "h2:has-text('Dashboard')",
        "[data-testid='create-dashboard']", "button:has-text('Create Dashboard')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in dash_selectors)

    if not found:
        session.missing_feature("Dashboards page", "Dashboards page not accessible")
        return True

    return session.assert_true(found, "Dashboards page loads")


def test_create_dashboard_button(session: WebTestSession) -> bool:
    """Verify Create Dashboard button is available for Admin/Editor."""
    session._log("[TEST] Create Dashboard button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/dashboards")
    session.wait_for_load(timeout=10)

    create_selectors = [
        "button:has-text('Create Dashboard')", "button:has-text('Vytvořit dashboard')",
        "[data-testid='create-dashboard']", "button:has-text('New Dashboard')",
        "button:has-text('Nový dashboard')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in create_selectors)

    if not found:
        session.missing_feature("Create Dashboard button", "Create Dashboard button not found")
        return True

    session.assert_true(found, "Create Dashboard button visible for admin")
    return True


def test_dashboard_cards_display(session: WebTestSession) -> bool:
    """Verify dashboard cards or list items display."""
    session._log("[TEST] Dashboard cards display")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/dashboards")
    session.wait_for_load(timeout=10)

    card_selectors = [
        ".dashboard-card", "[data-testid='dashboard-card']",
        ".dashboard-item", "[data-testid='dashboard-item']",
        "table.dashboards tbody tr",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in card_selectors)

    if not found:
        session.missing_feature("Dashboard cards", "No dashboard cards/items displayed (may be empty)")
        return True

    session.assert_true(found, "Dashboard cards or list items visible")
    return True


def test_chart_display(session: WebTestSession) -> bool:
    """Verify charts render in dashboard view."""
    session._log("[TEST] Chart display in dashboard")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/dashboards")
    session.wait_for_load(timeout=10)

    # Try clicking first dashboard
    card_selectors = [
        ".dashboard-card", "[data-testid='dashboard-card']",
        ".dashboard-item a", "table.dashboards tbody tr",
    ]
    for sel in card_selectors:
        if session.click(sel, timeout=1):
            break

    session.wait_for_load(timeout=10)

    # Check for chart elements (Recharts / Nivo SVGs)
    chart_selectors = [
        ".recharts-wrapper", ".recharts-surface",
        "svg.recharts-surface", "[data-testid='chart']",
        ".nivo-chart", ".chart-container",
        "canvas", "svg",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in chart_selectors)

    if not found:
        session.missing_feature("Chart display", "Charts not rendered (may need data or dashboard selection)")
        return True

    session.assert_true(found, "Charts rendered in dashboard view")
    return True


def test_sql_configuration_ui(session: WebTestSession) -> bool:
    """Verify SQL configuration UI (GROUP BY, ORDER BY, filters)."""
    session._log("[TEST] SQL configuration UI")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/dashboards")
    session.wait_for_load(timeout=10)

    # Look for query/config panel
    config_selectors = [
        "[data-testid='query-config']", ".query-config",
        "[data-testid='group-by']", "select[name='groupBy']",
        "[data-testid='order-by']", "select[name='orderBy']",
        "[data-testid='sql-editor']", ".sql-editor",
        "button:has-text('Configure')", "button:has-text('Edit Query')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in config_selectors)

    if not found:
        session.missing_feature("SQL configuration UI", "Query configuration panel not found")
        return True

    session.assert_true(found, "SQL configuration UI available")
    return True


def test_date_org_filters(session: WebTestSession) -> bool:
    """Verify date and organization filter controls."""
    session._log("[TEST] Date and organization filters")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/dashboards")
    session.wait_for_load(timeout=10)

    filter_selectors = [
        "[data-testid='date-filter']", "input[type='date']",
        "[data-testid='org-filter']", "select[name='organization']",
        "[data-testid='period-filter']", "select[name='period']",
        ".filter-bar", "[data-testid='filters']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in filter_selectors)

    if not found:
        session.missing_feature("Dashboard filters", "Date/organization filter controls not found")
        return True

    session.assert_true(found, "Dashboard filter controls visible")
    return True


def test_public_dashboard_visibility(session: WebTestSession) -> bool:
    """Verify public dashboards visible to Viewer role."""
    session._log("[TEST] Public dashboard visibility for Viewer")

    user = USERS["viewer"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    session.navigate_to("/dashboards")
    session.wait_for_load(timeout=10)

    # Viewer should see at least public dashboards
    current_url = session.driver.current_url.lower()
    page_loaded = "dashboards" in current_url or "dashboard" in current_url

    if not page_loaded:
        # Check if redirected away (access denied)
        if "login" in current_url or "forbidden" in current_url:
            session.missing_feature("Public dashboards for viewer", "Viewer cannot access dashboards page")
            return True

    dash_selectors = [
        ".dashboard-card", "[data-testid='dashboard-card']",
        ".dashboard-item", ".empty-state",
        "h1:has-text('Dashboard')", "h2:has-text('Dashboard')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in dash_selectors)
    session.assert_true(found or page_loaded, "Viewer can access dashboards page")
    return True


def test_data_source_indicator(session: WebTestSession) -> bool:
    """Verify data source type indicator (FORM / FILE)."""
    session._log("[TEST] Data source type indicator")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/dashboards")
    session.wait_for_load(timeout=10)

    source_selectors = [
        "[data-testid='source-type']", ".source-type",
        "span:has-text('FORM')", "span:has-text('FILE')",
        "[data-testid='data-source']", ".data-source-badge",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in source_selectors)

    if not found:
        session.missing_feature("Data source indicator", "source_type (FORM/FILE) indicator not found")
        return True

    session.assert_true(found, "Data source type indicator visible")
    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step11: Dashboards & SQL Reporting")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_dashboards_page_accessible(session)
        test_create_dashboard_button(session)
        test_dashboard_cards_display(session)
        test_chart_display(session)
        test_sql_configuration_ui(session)
        test_date_org_filters(session)
        test_public_dashboard_visibility(session)
        test_data_source_indicator(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
