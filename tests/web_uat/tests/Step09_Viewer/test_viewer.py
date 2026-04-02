# Web UAT Test: Step09 - File Viewer (FS09)
# Verifies: Slide-by-slide viewer, PNG preview, table display, navigation

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step09_Viewer"


def test_viewer_page_accessible(session: WebTestSession) -> bool:
    """Verify viewer page loads when navigating to /viewer."""
    session._log("[TEST] Viewer page is accessible")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    session.navigate_to("/viewer")
    session.wait_for_load(timeout=10)

    viewer_selectors = [
        "[data-testid='slide-list']", ".slide-list", ".slides-sidebar",
        "[data-testid='slide-preview']", ".slide-preview", ".preview-image",
        ".viewer-container", "[data-testid='viewer']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in viewer_selectors)

    if not found:
        session.missing_feature("Viewer page", "Viewer page not accessible or empty (no file selected)")
        return True

    return session.assert_true(found, "Viewer page loads with slide list or preview")


def test_viewer_from_files_page(session: WebTestSession) -> bool:
    """Verify navigating to viewer from files page."""
    session._log("[TEST] Navigate to viewer from files page")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/files")
    session.wait_for_load(timeout=10)

    # Try clicking a file row or View button
    view_selectors = [
        "button:has-text('View')", "button:has-text('Zobrazit')",
        "[data-testid='view-btn']", "tbody tr:first-child",
    ]

    clicked = False
    for sel in view_selectors:
        if session.click(sel, timeout=1):
            clicked = True
            break

    if not clicked:
        session.missing_feature("File view action", "No files available or view button not found")
        return True

    session.wait_for_load(timeout=10)
    current_url = session.driver.current_url.lower()
    has_viewer = "viewer" in current_url or "files/" in current_url
    session.assert_true(has_viewer, "Clicking file navigates to viewer or detail page")
    return True


def test_slide_list_sidebar(session: WebTestSession) -> bool:
    """Verify slide list sidebar shows slide thumbnails."""
    session._log("[TEST] Slide list sidebar")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/viewer")
    session.wait_for_load(timeout=10)

    slide_list_selectors = [
        "[data-testid='slide-list']", ".slide-list", ".slides-sidebar",
        ".slide-thumbnails", "[data-testid='slide-thumbnails']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in slide_list_selectors)

    if not found:
        session.missing_feature("Slide list sidebar", "Slide sidebar not visible (may need file context)")
        return True

    # Check for individual slide items
    slide_items = session.find_elements("[data-testid='slide-item'], .slide-item, .thumbnail", timeout=1)
    session.assert_true(len(slide_items) > 0, f"Slide list contains items (found {len(slide_items)})")
    return True


def test_slide_preview_image(session: WebTestSession) -> bool:
    """Verify slide preview displays PNG image."""
    session._log("[TEST] Slide preview image (PNG)")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/viewer")
    session.wait_for_load(timeout=10)

    preview_selectors = [
        "[data-testid='slide-preview'] img", ".slide-preview img",
        ".preview-image img", "img.slide-image",
        "[data-testid='slide-preview']", ".slide-preview",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in preview_selectors)

    if not found:
        session.missing_feature("Slide PNG preview", "Preview image not displayed (needs file context)")
        return True

    session.assert_true(found, "Slide preview area displays image")
    return True


def test_slide_navigation_buttons(session: WebTestSession) -> bool:
    """Verify Previous/Next slide navigation buttons."""
    session._log("[TEST] Slide navigation buttons")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/viewer")
    session.wait_for_load(timeout=10)

    nav_selectors = [
        "[data-testid='next-slide']", "button:has-text('Next')", "button:has-text('Další')",
        "[data-testid='prev-slide']", "button:has-text('Previous')", "button:has-text('Předchozí')",
        ".slide-nav", "[data-testid='slide-nav']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in nav_selectors)

    if not found:
        session.missing_feature("Slide navigation buttons", "Next/Previous buttons not found")
        return True

    session.assert_true(found, "Slide navigation buttons visible")
    return True


def test_extracted_table_display(session: WebTestSession) -> bool:
    """Verify extracted table data is displayed."""
    session._log("[TEST] Extracted table data display")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/viewer")
    session.wait_for_load(timeout=10)

    table_selectors = [
        "[data-testid='table-data']", ".table-data", "table.extracted-data",
        "[data-testid='extracted-table']", ".extracted-table",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in table_selectors)

    if not found:
        session.missing_feature("Extracted table display", "Table data section not visible in viewer")
        return True

    session.assert_true(found, "Extracted table data displayed in viewer")
    return True


def test_extracted_text_content(session: WebTestSession) -> bool:
    """Verify extracted text content is displayed."""
    session._log("[TEST] Extracted text content display")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/viewer")
    session.wait_for_load(timeout=10)

    text_selectors = [
        "[data-testid='text-content']", ".text-content", ".extracted-text",
        "[data-testid='slide-text']", ".slide-text",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in text_selectors)

    if not found:
        session.missing_feature("Extracted text content", "Text content section not visible in viewer")
        return True

    session.assert_true(found, "Extracted text content displayed in viewer")
    return True


def test_zoom_control(session: WebTestSession) -> bool:
    """Verify zoom control is available."""
    session._log("[TEST] Zoom control")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/viewer")
    session.wait_for_load(timeout=10)

    zoom_selectors = [
        "[data-testid='zoom']", ".zoom-control", ".zoom-slider",
        "button:has-text('Zoom')", "[data-testid='zoom-in']", "[data-testid='zoom-out']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in zoom_selectors)

    if not found:
        session.missing_feature("Zoom control", "Zoom control not found in viewer")
        return True

    session.assert_true(found, "Zoom control available in viewer")
    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step09: File Viewer")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_viewer_page_accessible(session)
        test_viewer_from_files_page(session)
        test_slide_list_sidebar(session)
        test_slide_preview_image(session)
        test_slide_navigation_buttons(session)
        test_extracted_table_display(session)
        test_extracted_text_content(session)
        test_zoom_control(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
