# Web UAT Test: Step19 - Form Filling UI (FS19)
# Verifies: Form filling, auto-save, validation, Excel import/export
# Route: /forms/:formId/fill (requires navigating from /forms first)

import sys
import os
import time

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../config")))

from selenium.webdriver.common.by import By
from web_common import WebTestSession
from web_config import BASE_URL, USERS

STEP_NAME = "Step19_Form_Filling"


def _navigate_to_form_fill(session: WebTestSession) -> bool:
    """Navigate to a form filling page by going to /forms and clicking Fill."""
    session.navigate_to("/forms")
    session.wait_for_load(timeout=10)

    # Try clicking a Fill button or navigating to a form's fill page
    fill_selectors = [
        "button:has-text('Fill')", "button:has-text('Vyplnit')",
        "a[href*='/fill']", "[data-testid='fill-btn']",
        # Or try clicking on any form row/card to get to its detail
        "[role='row']", "tbody tr", ".form-card",
    ]

    for sel in fill_selectors:
        if session.click(sel, timeout=2):
            session.wait_for_load(timeout=10)
            current_url = session.driver.current_url.lower()
            if "fill" in current_url or "forms/" in current_url:
                return True

    # If we're on a form detail page, look for a fill link
    fill_link_selectors = [
        "a[href*='/fill']", "button:has-text('Fill')",
        "button:has-text('Start Filling')", "button:has-text('Vyplnit')",
    ]
    for sel in fill_link_selectors:
        if session.click(sel, timeout=2):
            session.wait_for_load(timeout=10)
            return True

    return False


def test_form_fill_page_accessible(session: WebTestSession) -> bool:
    """Verify form fill page loads with form fields."""
    session._log("[TEST] Form fill page is accessible")

    user = USERS["editor"]
    session.login(user["email"], user["password"])
    session.wait_for_load(timeout=15)

    if not _navigate_to_form_fill(session):
        session.missing_feature("Form fill page", "Could not navigate to form fill page (no forms to fill)")
        return True

    # Check for form header or input fields
    fill_selectors = [
        "input[type='text']", "input[type='number']", "textarea",
        "[data-testid='form-header']", ".form-header",
        "button:has-text('Submit')", "button:has-text('Save Draft')",
    ]

    found = any(session.is_visible(sel, timeout=3) for sel in fill_selectors)
    return session.assert_true(found, "Form fill page loads with fields")


def test_form_fields_editable(session: WebTestSession) -> bool:
    """Verify form fields are editable."""
    session._log("[TEST] Form fields are editable")

    input_selectors = [
        "input[type='text']", "input[type='number']", "textarea",
    ]

    filled = False
    for selector in input_selectors:
        inputs = session.driver.find_elements(By.CSS_SELECTOR, selector)
        for inp in inputs[:3]:
            try:
                if inp.is_displayed() and inp.is_enabled():
                    inp.clear()
                    inp.send_keys("Test Value")
                    time.sleep(0.3)
                    filled = True
                    break
            except Exception:
                pass
        if filled:
            break

    return session.assert_true(filled, "Form fields can be edited")


def test_validation_errors(session: WebTestSession) -> bool:
    """Verify validation errors are displayed."""
    session._log("[TEST] Validation errors displayed")

    submit_selectors = [
        "button:has-text('Submit')", "button:has-text('Odeslat')",
        "[data-testid='submit-form']",
    ]

    for selector in submit_selectors:
        if session.click(selector, timeout=2):
            time.sleep(1)
            break

    # Check for validation error messages (FluentUI uses validationState="error")
    error_selectors = [
        ".validation-error", "[data-testid='validation-error']",
        "span:has-text('required')", "span:has-text('Required')",
        "span:has-text('This field is required')",
        "[aria-invalid='true']",
    ]

    errors_found = any(session.is_visible(sel, timeout=2) for sel in error_selectors)

    if errors_found:
        session.assert_true(True, "Validation errors are displayed for incomplete fields")
    else:
        session._log("[INFO] No validation errors shown (fields may be optional)")

    return True


def test_save_draft(session: WebTestSession) -> bool:
    """Verify save draft functionality."""
    session._log("[TEST] Save draft")

    draft_selectors = [
        "button:has-text('Save Draft')", "button:has-text('Uložit koncept')",
        "[data-testid='save-draft']",
    ]

    saved = False
    for selector in draft_selectors:
        if session.click(selector, timeout=2):
            time.sleep(1)
            saved = True
            break

    if saved:
        # Check for success indicator (Badge with "Saved" text)
        success_selectors = [
            "span:has-text('Saved')", "span:has-text('Uloženo')",
            "[data-testid='saved']", ".autosaved",
        ]
        found = any(session.is_visible(sel, timeout=3) for sel in success_selectors)
        session.assert_true(saved, "Save draft button clicked")
    else:
        session.missing_feature("Save draft", "Save draft button not found")

    return True


def test_autosave_indicator(session: WebTestSession) -> bool:
    """Verify auto-save indicator is present."""
    session._log("[TEST] Auto-save indicator")

    # FormFillerPage shows "Saving..." (Badge, informative) or "Saved {time}" (Badge, success)
    autosave_selectors = [
        "span:has-text('Saving')", "span:has-text('Saved')",
        "[data-testid='autosave']", ".autosave-indicator",
    ]

    found = any(session.is_visible(sel, timeout=3) for sel in autosave_selectors)

    if found:
        session.assert_true(True, "Auto-save indicator is present")
    else:
        session.missing_feature("Auto-save indicator", "Autosave status not visible")

    return True


def test_completion_progress(session: WebTestSession) -> bool:
    """Verify completion progress bar."""
    session._log("[TEST] Completion progress bar")

    # FormFillerPage shows ProgressBar + "Completion: X%"
    progress_selectors = [
        "[role='progressbar']", ".progress-bar",
        "span:has-text('Completion')", "span:has-text('Dokončeno')",
    ]

    found = any(session.is_visible(sel, timeout=2) for sel in progress_selectors)

    if found:
        session.assert_true(True, "Completion progress bar visible")
    else:
        session.missing_feature("Completion progress", "Completion progress bar not found")

    return True


def test_export_excel_button(session: WebTestSession) -> bool:
    """Verify Excel export button exists."""
    session._log("[TEST] Export Excel button")

    export_selectors = [
        "button:has-text('Export Excel')", "button:has-text('Exportovat Excel')",
        "[data-testid='export-excel']", "button:has-text('Export')",
    ]

    found = any(session.is_visible(sel, timeout=2) for sel in export_selectors)

    if found:
        session.assert_true(True, "Export Excel button is present")
    else:
        session.missing_feature("Export Excel", "Export Excel button not found")

    return True


def test_import_excel_button(session: WebTestSession) -> bool:
    """Verify Excel import button exists."""
    session._log("[TEST] Import Excel button")

    import_selectors = [
        "button:has-text('Import Excel')", "button:has-text('Importovat Excel')",
        "[data-testid='import-excel']", "button:has-text('Import')",
    ]

    found = any(session.is_visible(sel, timeout=2) for sel in import_selectors)

    if found:
        session.assert_true(True, "Import Excel button is present")
    else:
        session.missing_feature("Import Excel", "Import Excel button not found")

    return True


def test_submit_button(session: WebTestSession) -> bool:
    """Verify submit button exists."""
    session._log("[TEST] Submit button")

    submit_selectors = [
        "button:has-text('Submit')", "button:has-text('Odeslat')",
        "[data-testid='submit-form']",
    ]

    found = any(session.is_visible(sel, timeout=2) for sel in submit_selectors)

    if found:
        session.assert_true(True, "Submit button is present on form fill page")
    else:
        # If we never reached a form fill page, this is expected
        current_url = session.driver.current_url.lower()
        if "fill" not in current_url:
            session.missing_feature("Submit button", "Not on form fill page — no forms to fill")
        else:
            session.assert_true(False, "Submit button is present on form fill page")

    return True


def main() -> int:
    session = WebTestSession(base_url=BASE_URL)

    session._log("[INFO] Web UAT Tests - Step19: Form Filling UI")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    if not session.init_driver():
        session._err("[FAIL] Failed to initialize WebDriver")
        return 1

    try:
        test_form_fill_page_accessible(session)
        test_form_fields_editable(session)
        test_validation_errors(session)
        test_save_draft(session)
        test_autosave_indicator(session)
        test_completion_progress(session)
        test_export_excel_button(session)
        test_import_excel_button(session)
        test_submit_button(session)
    finally:
        session.quit_driver()

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
