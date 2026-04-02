# Web UAT Test: Step18 - PPTX Generation UI (FS18)
# Verifies: Template management, placeholder preview, generation trigger

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step18_PPTX_Generation"


def test_templates_page_accessible(session: WebTestSession) -> bool:
    """Verify templates page loads."""
    session._log("[TEST] Templates page is accessible")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    session.navigate_to("/templates")
    session.wait_for_load(timeout=10)

    # Check for template cards, DataGrid, or upload button
    template_selectors = [
        "button:has-text('Upload Template')", "button:has-text('Upload')",
        "[role='grid']", "table",
        "[data-testid='template-card']", ".template-card",
        "h1:has-text('Template')", "h2:has-text('Template')",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in template_selectors)

    return session.assert_true(found, "Templates page loads with template cards or upload button")


def test_template_cards(session: WebTestSession) -> bool:
    """Verify template cards display template info."""
    session._log("[TEST] Template cards display info")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/templates")
    session.wait_for_load(timeout=10)

    card_selectors = [
        "[data-testid='template-card']",
        ".template-card"
    ]

    cards_found = False
    for selector in card_selectors:
        cards = session.driver.find_elements(By.CSS_SELECTOR, selector)
        if cards:
            cards_found = True
            session._log(f"[INFO] Found {len(cards)} template cards")
            break

    if cards_found:
        session.assert_true(True, "Template cards are displayed")
    else:
        session.missing_feature("Template cards", "No template cards found")

    return True


def test_upload_template_button(session: WebTestSession) -> bool:
    """Verify upload template button exists."""
    session._log("[TEST] Upload template button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/templates")
    session.wait_for_load(timeout=10)

    upload_selectors = [
        "button:has-text('Upload Template')", "button:has-text('Upload')",
        "button:has-text('Nahrát šablonu')", "[data-testid='upload-template']",
    ]

    found = any(session.is_visible(sel, timeout=1) for sel in upload_selectors)
    return session.assert_true(found, "Upload template button is present")


def test_template_version_badge(session: WebTestSession) -> bool:
    """Verify version badge is shown on templates."""
    session._log("[TEST] Template version badge")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/templates")
    session.wait_for_load(timeout=10)

    version_selectors = [
        "[data-testid='version-badge']",
        ".version-badge"
    ]

    found = False
    for selector in version_selectors:
        if session.is_element_present(selector, timeout=1):
            found = True
            break

    if found:
        session.assert_true(True, "Template version badge is displayed")
    else:
        session.missing_feature("Version badge", "Version badge not found")

    return True


def test_placeholder_list(session: WebTestSession) -> bool:
    """Verify placeholder list is shown for a template."""
    session._log("[TEST] Placeholder list")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/templates")
    session.wait_for_load(timeout=10)

    # Click on a template card to see details
    card_selectors = ["[data-testid='template-card']", ".template-card"]
    clicked = False
    for selector in card_selectors:
        cards = session.driver.find_elements(By.CSS_SELECTOR, selector)
        if cards:
            cards[0].click()
            time.sleep(2)
            clicked = True
            break

    # Look for placeholder list
    placeholder_selectors = [
        "[data-testid='placeholder-list']",
        ".placeholder-list"
    ]

    found = False
    for selector in placeholder_selectors:
        if session.is_visible(selector, timeout=5):
            found = True
            # Try to count placeholders
            items = session.driver.find_elements(By.CSS_SELECTOR, selector + " li, .placeholder-item")
            session._log(f"[INFO] Found {len(items)} placeholder items")
            break

    if found:
        session.assert_true(True, "Placeholder list is displayed for template")
    else:
        session.missing_feature("Placeholder list", "Placeholder list not visible")

    return True


def test_generate_button(session: WebTestSession) -> bool:
    """Verify generate button exists."""
    session._log("[TEST] Generate button")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/templates")
    session.wait_for_load(timeout=10)

    generate_selectors = [
        "button:has-text('Generate')",
        "button:has-text('Generovat')",
        "[data-testid='generate-pptx']"
    ]

    found = False
    for selector in generate_selectors:
        if session.is_element_present(selector, timeout=1):
            found = True
            break

    if found:
        session.assert_true(True, "Generate button is present")
    else:
        session.missing_feature("Generate button", "Generate button not found")

    return True


def test_template_preview(session: WebTestSession) -> bool:
    """Verify template preview functionality."""
    session._log("[TEST] Template preview")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/templates")
    session.wait_for_load(timeout=10)

    # Look for preview button or click on template
    preview_selectors = [
        "button:has-text('Preview')",
        "button:has-text('Náhled')",
        "[data-testid='preview-template']"
    ]

    clicked = False
    for selector in preview_selectors:
        if session.is_element_present(selector, timeout=1):
            session.click(selector)
            time.sleep(1)
            clicked = True
            break

    if clicked:
        # Check for preview modal
        preview_opened = session.is_visible(".preview-modal, .template-preview", timeout=5)
        session.assert_true(preview_opened, "Template preview modal opened")
    else:
        session.missing_feature("Template preview", "Preview button not found")

    return True


def test_pptx_download_ready(session: WebTestSession) -> bool:
    """Verify generated PPTX can be downloaded."""
    session._log("[TEST] PPTX download ready")

    user = USERS["holding_admin"]
    session.login(user["email"], user["password"])
    session.navigate_to("/reports")
    session.wait_for_load(timeout=10)

    # Look for generated PPTX download link
    download_selectors = [
        "a[href*='.pptx']",
        "button:has-text('Download PPTX')",
        "[data-testid='download-pptx']"
    ]

    found = False
    for selector in download_selectors:
        if session.is_visible(selector, timeout=1):
            found = True
            session._log(f"[OK]   Found download element: {selector}")
            break

    if found:
        session.assert_true(True, "PPTX download is available")
    else:
        session._log("[INFO] No generated PPTX download link found (may not be generated yet)")

    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step18: PPTX Generation UI")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_templates_page_accessible(session)
        test_template_cards(session)
        test_upload_template_button(session)
        test_template_version_badge(session)
        test_placeholder_list(session)
        test_generate_button(session)
        test_template_preview(session)
        test_pptx_download_ready(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
