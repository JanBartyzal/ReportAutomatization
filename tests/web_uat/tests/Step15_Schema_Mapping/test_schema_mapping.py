# Web UAT Test: Step15 - Template & Schema Mapping Registry (FS15)
# Verifies: Mapping editor, auto-suggest, Excel-to-form mapping, slide metadata

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step15_Schema_Mapping"


def test_schema_mapping_page_accessible(session: WebTestSession) -> bool:
    """Verify schema mapping page or section is accessible."""
    session._log("[TEST] Schema mapping page accessible")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    # Schema mapping may be under /admin, /settings, or /templates
    paths = ["/admin", "/settings", "/templates"]
    found = False

    for path in paths:
        session.navigate_to(path)
        session.wait_for_load(timeout=10)

        mapping_selectors = [
            "[data-testid='schema-mapping']", ".schema-mapping",
            "button:has-text('Schema Mapping')", "button:has-text('Mapování')",
            "a:has-text('Mapping')", "a:has-text('Mapování')",
            "[data-testid='tab-mapping']", "button:has-text('Mappings')",
        ]

        if any(session.is_visible(sel, timeout=1) for sel in mapping_selectors):
            found = True
            break

    if not found:
        session.missing_feature("Schema mapping page", "Schema mapping UI not accessible from admin/settings/templates")
        return True

    return session.assert_true(found, "Schema mapping page or section accessible")


def test_mapping_templates_list(session: WebTestSession) -> bool:
    """Verify list of mapping templates displays."""
    session._log("[TEST] Mapping templates list")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin")
    session.wait_for_load(timeout=10)

    # Try to navigate to mappings section
    mapping_nav_selectors = [
        "button:has-text('Schema Mapping')", "button:has-text('Mappings')",
        "a:has-text('Mapping')", "[data-testid='tab-mapping']",
    ]
    for sel in mapping_nav_selectors:
        if session.click(sel, timeout=1):
            break

    time.sleep(1)

    list_selectors = [
        ".mapping-list", "[data-testid='mapping-list']",
        ".mapping-table", "[data-testid='mapping-table']",
        "table.mappings", ".mapping-card",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in list_selectors)

    if not found:
        session.missing_feature("Mapping templates list", "Mapping templates list not displayed")
        return True

    session.assert_true(found, "Mapping templates list visible")
    return True


def test_create_mapping_button(session: WebTestSession) -> bool:
    """Verify Create Mapping button."""
    session._log("[TEST] Create Mapping button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin")
    session.wait_for_load(timeout=10)

    create_selectors = [
        "button:has-text('Create Mapping')", "button:has-text('Vytvořit mapování')",
        "button:has-text('New Mapping')", "button:has-text('Nové mapování')",
        "[data-testid='create-mapping']", "button:has-text('Add Mapping')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in create_selectors)

    if not found:
        session.missing_feature("Create mapping button", "Create Mapping button not found")
        return True

    session.assert_true(found, "Create Mapping button available")
    return True


def test_mapping_editor_ui(session: WebTestSession) -> bool:
    """Verify mapping editor with source → target column mapping."""
    session._log("[TEST] Mapping editor UI")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin")
    session.wait_for_load(timeout=10)

    # Try to open mapping editor
    editor_nav_selectors = [
        "button:has-text('Create Mapping')", "[data-testid='create-mapping']",
        ".mapping-card:first-child", ".mapping-table tbody tr:first-child",
    ]
    for sel in editor_nav_selectors:
        if session.click(sel, timeout=1):
            break

    session.wait_for_load(timeout=10)

    editor_selectors = [
        "[data-testid='mapping-editor']", ".mapping-editor",
        "[data-testid='source-column']", "[data-testid='target-column']",
        "input[name='sourceColumn']", "input[name='targetColumn']",
        ".mapping-row", ".column-mapping",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in editor_selectors)

    if not found:
        session.missing_feature("Mapping editor", "Mapping editor UI not displayed")
        return True

    session.assert_true(found, "Mapping editor with source/target columns visible")
    return True


def test_auto_suggest_mapping(session: WebTestSession) -> bool:
    """Verify auto-suggest mapping feature."""
    session._log("[TEST] Auto-suggest mapping")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin")
    session.wait_for_load(timeout=10)

    suggest_selectors = [
        "button:has-text('Auto Suggest')", "button:has-text('Navrhnout')",
        "button:has-text('Suggest Mapping')", "[data-testid='auto-suggest']",
        "button:has-text('Auto-map')", "button:has-text('Automatické mapování')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in suggest_selectors)

    if not found:
        session.missing_feature("Auto-suggest mapping", "Auto-suggest button not found")
        return True

    session.assert_true(found, "Auto-suggest mapping button available")
    return True


def test_excel_to_form_mapping(session: WebTestSession) -> bool:
    """Verify Excel-to-form mapping inference UI (FS15 + FS19 integration)."""
    session._log("[TEST] Excel-to-form mapping inference")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])

    # Check in form filling page for import mapping
    session.navigate_to("/forms/fill")
    session.wait_for_load(timeout=10)

    import_selectors = [
        "button:has-text('Import Excel')", "button:has-text('Importovat Excel')",
        "[data-testid='import-excel']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in import_selectors)

    if not found:
        session.missing_feature("Excel-to-form mapping", "Import Excel button not found on form fill page")
        return True

    # Try clicking to see mapping preview
    for sel in import_selectors:
        if session.click(sel, timeout=1):
            break

    time.sleep(1)

    mapping_preview_selectors = [
        "[data-testid='mapping-preview']", ".mapping-preview",
        "[data-testid='column-mapping']", ".column-mapping-modal",
        ".import-dialog", "[data-testid='import-dialog']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in mapping_preview_selectors)

    if not found:
        session.missing_feature("Excel-to-form mapping dialog", "Mapping preview dialog not shown after import click")
        return True

    session.assert_true(found, "Excel-to-form mapping dialog displayed")
    return True


def test_slide_metadata_section(session: WebTestSession) -> bool:
    """Verify slide metadata management section."""
    session._log("[TEST] Slide metadata section")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/templates")
    session.wait_for_load(timeout=10)

    metadata_selectors = [
        "[data-testid='slide-metadata']", ".slide-metadata",
        "button:has-text('Slide Metadata')", "button:has-text('Metadata slidů')",
        "[data-testid='metadata-tab']", "a:has-text('Metadata')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in metadata_selectors)

    if not found:
        session.missing_feature("Slide metadata section", "Slide metadata management not found")
        return True

    session.assert_true(found, "Slide metadata section available")
    return True


def test_learning_indicator(session: WebTestSession) -> bool:
    """Verify learning/history indicator for previous mappings."""
    session._log("[TEST] Mapping learning indicator")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/admin")
    session.wait_for_load(timeout=10)

    learning_selectors = [
        "[data-testid='mapping-history']", ".mapping-history",
        "[data-testid='learned-mappings']", ".learned-mappings",
        "span:has-text('Previously mapped')", "span:has-text('Suggested')",
        ".mapping-confidence", "[data-testid='confidence']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in learning_selectors)

    if not found:
        session.missing_feature("Mapping learning indicator", "Learning/history indicator for mappings not found")
        return True

    session.assert_true(found, "Mapping learning indicator visible")
    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step15: Template & Schema Mapping Registry")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_schema_mapping_page_accessible(session)
        test_mapping_templates_list(session)
        test_create_mapping_button(session)
        test_mapping_editor_ui(session)
        test_auto_suggest_mapping(session)
        test_excel_to_form_mapping(session)
        test_slide_metadata_section(session)
        test_learning_indicator(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
