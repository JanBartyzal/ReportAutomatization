# Base Page Objects for ReportAutomatization Web UI Tests
# Covers FS07, FS09, FS11, FS13, FS14, FS15, FS16, FS17, FS18, FS19, FS20 frontend functionality

from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException
import time

import re as _re

_HAS_TEXT_RE_BP = _re.compile(r"^(\w+):has-text\(['\"](.+?)['\"]\)$")
_HAS_TEXT_COMPLEX_RE_BP = _re.compile(r"^(.+\s+)?(\w+):has-text\(['\"](.+?)['\"]\)$")


def _sel(selector: str) -> tuple:
    """Convert selector — :has-text() → XPath, otherwise CSS."""
    m = _HAS_TEXT_RE_BP.match(selector.strip())
    if m:
        tag, text = m.group(1), m.group(2)
        return By.XPATH, f"//{tag}[contains(text(),'{text}') or .//*[contains(text(),'{text}')]]"
    m2 = _HAS_TEXT_COMPLEX_RE_BP.match(selector.strip())
    if m2:
        tag, text = m2.group(2), m2.group(3)
        return By.XPATH, f"//{tag}[contains(text(),'{text}') or .//*[contains(text(),'{text}')]]"
    return By.CSS_SELECTOR, selector


class BasePage:
    """Base page object with common functionality."""

    PATH = ""

    def __init__(self, session):
        self.session = session
        self.driver = session.driver
        self.by = By

    def navigate(self, path: str = "") -> bool:
        full_path = self.PATH + path
        return self.session.navigate_to(full_path)

    def is_loaded(self) -> bool:
        raise NotImplementedError

    def wait_for_load(self, timeout: int = None) -> bool:
        timeout = timeout or 10
        try:
            wait = WebDriverWait(self.driver, timeout)
            wait.until(lambda d: self.is_loaded())
            return True
        except TimeoutException:
            return False

    def get_title(self) -> str:
        return self.driver.title

    def get_current_url(self) -> str:
        return self.driver.current_url

    def is_visible(self, selector: str, timeout: int = 10) -> bool:
        try:
            by, sel = _sel(selector)
            wait = WebDriverWait(self.driver, timeout)
            wait.until(EC.visibility_of_element_located((by, sel)))
            return True
        except TimeoutException:
            return False

    def is_element_present(self, selector: str, timeout: int = 10) -> bool:
        try:
            by, sel = _sel(selector)
            wait = WebDriverWait(self.driver, timeout)
            wait.until(EC.presence_of_element_located((by, sel)))
            return True
        except TimeoutException:
            return False

    def click(self, selector: str, timeout: int = 10) -> bool:
        try:
            by, sel = _sel(selector)
            element = WebDriverWait(self.driver, timeout).until(
                EC.element_to_be_clickable((by, sel))
            )
            element.click()
            time.sleep(0.3)
            return True
        except Exception:
            return False

    def type_text(self, selector: str, text: str, clear: bool = True) -> bool:
        try:
            by, sel = _sel(selector)
            element = self.driver.find_element(by, sel)
            if clear:
                element.clear()
            element.send_keys(text)
            return True
        except Exception:
            return False

    def get_text(self, selector: str, timeout: int = 10) -> str:
        try:
            by, sel = _sel(selector)
            element = WebDriverWait(self.driver, timeout).until(
                EC.presence_of_element_located((by, sel))
            )
            return element.text or ""
        except Exception:
            return ""

    def get_attribute(self, selector: str, attr: str, timeout: int = 10) -> str:
        try:
            by, sel = _sel(selector)
            element = WebDriverWait(self.driver, timeout).until(
                EC.presence_of_element_located((by, sel))
            )
            return element.get_attribute(attr) or ""
        except Exception:
            return ""

    def is_checked(self, selector: str) -> bool:
        try:
            by, sel = _sel(selector)
            element = self.driver.find_element(by, sel)
            return element.is_selected()
        except Exception:
            return False

    def select_dropdown(self, selector: str, value: str) -> bool:
        from selenium.webdriver.support.ui import Select
        try:
            by, sel = _sel(selector)
            element = self.driver.find_element(by, sel)
            select = Select(element)
            select.select_by_value(value)
            time.sleep(0.3)
            return True
        except Exception:
            return False

    def upload_file(self, selector: str, file_path: str) -> bool:
        """Upload file using input[type='file']."""
        try:
            by, sel = _sel(selector)
            element = self.driver.find_element(by, sel)
            element.send_keys(file_path)
            time.sleep(1)
            return True
        except Exception:
            return False


class LoginPage(BasePage):
    """Login page — FS09: MSAL authentication."""

    PATH = "/login"

    EMAIL_INPUT = "input[type='email'], input[name='email'], #email, [data-testid='email-input']"
    PASSWORD_INPUT = "input[type='password'], input[name='password'], #password, [data-testid='password-input']"
    SUBMIT_BUTTON = "button[type='submit'], button[data-testid='login-button'], button:has-text('Sign in'), button:has-text('Login'), button:has-text('Přihlásit')"
    ERROR_MESSAGE = ".alert-danger, .error-message, [data-testid='error-message'], .MuiAlert-root"
    FORGOT_PASSWORD = "a:has-text('Forgot'), a:has-text('Zapomenuté heslo')"
    REMEMBER_ME = "input[type='checkbox'][name='remember'], [data-testid='remember-me']"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.EMAIL_INPUT, timeout=10)

    def login(self, email: str, password: str = "") -> bool:
        if not self.is_loaded():
            self.navigate()

        self.type_text(self.EMAIL_INPUT, email)
        if self.is_element_present(self.PASSWORD_INPUT, timeout=2):
            self.type_text(self.PASSWORD_INPUT, password)
        self.click(self.SUBMIT_BUTTON)
        time.sleep(2)

        if self.is_element_present(self.ERROR_MESSAGE, timeout=3):
            return False
        return True

    def get_error_message(self) -> str:
        return self.get_text(self.ERROR_MESSAGE, timeout=3)


class DashboardPage(BasePage):
    """Dashboard page — main entry point after login."""

    PATH = "/dashboard"

    SIDEBAR_NAV = "nav.sidebar, [data-testid='sidebar'], .sidebar-nav, aside.sidebar"
    USER_MENU = "[data-testid='user-menu'], button:has-text('User'), .user-menu"
    LOGOUT_BUTTON = "button:has-text('Logout'), button:has-text('Sign out'), button:has-text('Odhlásit')"
    HERO_METRICS = ".hero-metrics, [data-testid='hero-metrics'], .metrics-cards, .stat-cards"
    QUICK_ACTIONS = ".quick-actions, [data-testid='quick-actions'], .action-cards"
    RECENT_FILES = ".recent-files, [data-testid='recent-files'], .file-list"
    ALERT_BANNER = ".alert, .notification, [data-testid='alert'], .MuiAlert-root"
    PERIOD_SELECTOR = "[data-testid='period-selector'], select[name='period'], .period-select"
    ORG_NAME = "[data-testid='org-name'], .org-name, .company-name"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.SIDEBAR_NAV + ", " + self.HERO_METRICS, timeout=15)

    def get_hero_metrics(self) -> list:
        metrics = []
        metric_selectors = [
            ".metric-card", "[data-testid='metric-card']",
            ".hero-metrics .metric", ".metrics-cards .card",
            ".stat-card", "[data-testid='stat-card']"
        ]
        for selector in metric_selectors:
            elements = self.driver.find_elements(By.CSS_SELECTOR, selector)
            if elements:
                for el in elements:
                    if el.text:
                        metrics.append(el.text)
                break
        return metrics

    def navigate_to_files(self) -> bool:
        file_links = [
            "a[href*='/files']", "a:has-text('Files')", "a:has-text('Soubory')",
            "[data-testid='nav-files']", "nav a:has-text('Files')"
        ]
        for link in file_links:
            if self.click(link, timeout=3):
                return True
        return False

    def navigate_to_forms(self) -> bool:
        form_links = [
            "a[href*='/forms']", "a:has-text('Forms')", "a:has-text('Formuláře')",
            "[data-testid='nav-forms']", "nav a:has-text('Forms')"
        ]
        for link in form_links:
            if self.click(link, timeout=3):
                return True
        return False

    def navigate_to_reports(self) -> bool:
        report_links = [
            "a[href*='/reports']", "a:has-text('Reports')", "a:has-text('Reporty')",
            "[data-testid='nav-reports']", "nav a:has-text('Reports')"
        ]
        for link in report_links:
            if self.click(link, timeout=3):
                return True
        return False

    def navigate_to_periods(self) -> bool:
        period_links = [
            "a[href*='/periods']", "a:has-text('Periods')", "a:has-text('Období')",
            "[data-testid='nav-periods']", "nav a:has-text('Periods')"
        ]
        for link in period_links:
            if self.click(link, timeout=3):
                return True
        return False

    def navigate_to_dashboards(self) -> bool:
        dash_links = [
            "a[href*='/dashboards']", "a:has-text('Dashboards')",
            "[data-testid='nav-dashboards']", "nav a:has-text('Dashboards')"
        ]
        for link in dash_links:
            if self.click(link, timeout=3):
                return True
        return False

    def navigate_to_admin(self) -> bool:
        admin_links = [
            "a[href*='/admin']", "a:has-text('Admin')",
            "[data-testid='nav-admin']", "nav a:has-text('Admin')"
        ]
        for link in admin_links:
            if self.click(link, timeout=3):
                return True
        return False

    def logout(self) -> bool:
        if self.click(self.USER_MENU, timeout=5):
            time.sleep(0.5)
            if self.click(self.LOGOUT_BUTTON, timeout=3):
                time.sleep(1)
                return True
        return False


class UploadPage(BasePage):
    """File upload page — FS02/FS09: Drag & drop upload."""

    PATH = "/upload"

    DROPZONE = "[data-testid='dropzone'], .dropzone, #file-upload, .upload-zone"
    FILE_INPUT = "input[type='file'], [data-testid='file-input']"
    UPLOAD_BUTTON = "button:has-text('Upload'), button:has-text('Nahrát'), [data-testid='upload-btn']"
    PROGRESS_BAR = "[data-testid='progress-bar'], .progress-bar, .MuiLinearProgress-root"
    FILE_LIST = ".file-list, [data-testid='file-list'], .uploaded-files"
    FILE_ITEM = ".file-item, [data-testid='file-item'], .file-list li"
    SUCCESS_MESSAGE = "[data-testid='upload-success'], .success-message, .MuiAlert-root:has-text('success')"
    ERROR_MESSAGE = "[data-testid='upload-error'], .error-message"
    BROWSE_BUTTON = "button:has-text('Browse'), button:has-text('Procházet')"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.DROPZONE + ", " + self.FILE_INPUT, timeout=10)

    def upload_file(self, file_path: str) -> bool:
        """Upload file using the file input."""
        if not self.is_element_present(self.FILE_INPUT, timeout=5):
            # Try dropzone
            if self.is_element_present(self.DROPZONE, timeout=3):
                dropzone = self.driver.find_element(By.CSS_SELECTOR, self.DROPZONE)
                dropzone.send_keys(file_path)
                time.sleep(1)
                return True
            return False

        file_input = self.driver.find_element(By.CSS_SELECTOR, self.FILE_INPUT)
        file_input.send_keys(file_path)
        time.sleep(1)
        return True

    def wait_for_upload_complete(self, timeout: int = 60) -> bool:
        """Wait for upload progress bar to disappear or success message to appear."""
        try:
            # Check for success message
            if self.is_element_present(self.SUCCESS_MESSAGE, timeout=timeout):
                return True
            # Wait for progress bar to complete
            WebDriverWait(self.driver, timeout).until_not(
                EC.visibility_of_element_located((By.CSS_SELECTOR, self.PROGRESS_BAR))
            )
            return True
        except TimeoutException:
            return False

    def get_uploaded_files(self) -> list:
        items = self.driver.find_elements(By.CSS_SELECTOR, self.FILE_ITEM)
        return [item.text for item in items if item.text]


class FilesPage(BasePage):
    """File listing page — FS09: View uploaded files."""

    PATH = "/files"

    FILE_TABLE = ".file-table, [data-testid='file-table'], table.files"
    FILE_ROWS = ".file-row, [data-testid='file-row'], tbody tr"
    FILE_NAME = ".file-name, [data-testid='file-name'], tbody tr td:first-child"
    FILE_STATUS = ".file-status, [data-testid='file-status'], .status-badge"
    FILE_TYPE_FILTER = "select[name='fileType'], [data-testid='file-type-filter']"
    SEARCH_INPUT = "input[type='search'], input[placeholder*='Search'], [data-testid='search-input']"
    UPLOAD_BUTTON = "button:has-text('Upload'), button:has-text('Nahrát')"
    VIEW_BUTTON = "button:has-text('View'), button:has-text('Zobrazit')"
    DELETE_BUTTON = "button:has-text('Delete'), button:has-text('Smazat')"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.FILE_TABLE + ", " + self.FILE_ROWS, timeout=10)

    def get_file_count(self) -> int:
        rows = self.driver.find_elements(By.CSS_SELECTOR, self.FILE_ROWS)
        return len(rows) if rows else 0

    def click_file(self, index: int = 0) -> bool:
        try:
            rows = self.driver.find_elements(By.CSS_SELECTOR, self.FILE_ROWS)
            if rows and index < len(rows):
                rows[index].click()
                time.sleep(1)
                return True
        except Exception:
            pass
        return False

    def filter_by_type(self, file_type: str) -> bool:
        """Filter by file type: pptx, xlsx, pdf, csv."""
        return self.select_dropdown(self.FILE_TYPE_FILTER, file_type)


class ViewerPage(BasePage):
    """File viewer page — FS09: View parsed PPTX/Excel content."""

    PATH = "/viewer"

    SLIDE_LIST = ".slide-list, [data-testid='slide-list'], .slides-sidebar"
    SLIDE_ITEM = ".slide-item, [data-testid='slide-item']"
    SLIDE_PREVIEW = "[data-testid='slide-preview'], .slide-preview, .preview-image"
    SLIDE_NAV = ".slide-nav, [data-testid='slide-nav'], .nav-buttons"
    PREV_BUTTON = "button:has-text('Previous'), button:has-text('Předchozí'), [data-testid='prev-slide']"
    NEXT_BUTTON = "button:has-text('Next'), button:has-text('Další'), [data-testid='next-slide']"
    TABLE_DATA = ".table-data, [data-testid='table-data'], table.extracted-data"
    TEXT_CONTENT = ".text-content, [data-testid='text-content'], .extracted-text"
    ZOOM_CONTROL = "[data-testid='zoom'], .zoom-control, .zoom-slider"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.SLIDE_LIST + ", " + self.SLIDE_PREVIEW, timeout=10)

    def click_slide(self, index: int) -> bool:
        try:
            slides = self.driver.find_elements(By.CSS_SELECTOR, self.SLIDE_ITEM)
            if slides and index < len(slides):
                slides[index].click()
                time.sleep(0.5)
                return True
        except Exception:
            pass
        return False

    def navigate_next_slide(self) -> bool:
        return self.click(self.NEXT_BUTTON)

    def navigate_prev_slide(self) -> bool:
        return self.click(self.PREV_BUTTON)

    def get_table_count(self) -> int:
        tables = self.driver.find_elements(By.CSS_SELECTOR, self.TABLE_DATA)
        return len(tables) if tables else 0


class FormsListPage(BasePage):
    """Forms listing page — FS19: List of available forms."""

    PATH = "/forms"

    FORM_CARDS = ".form-card, [data-testid='form-card']"
    FORM_NAME = ".form-name, [data-testid='form-name']"
    FORM_STATUS = ".form-status, [data-testid='form-status'], .status-badge"
    CREATE_FORM_BUTTON = "button:has-text('Create Form'), button:has-text('Nový formulář'), [data-testid='create-form']"
    PERIOD_FILTER = "select[name='period'], [data-testid='period-filter']"
    SEARCH_INPUT = "input[type='search'], [data-testid='search-forms']"
    FILL_BUTTON = "button:has-text('Fill'), button:has-text('Vyplnit')"
    VIEW_BUTTON = "button:has-text('View'), button:has-text('Zobrazit')"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.FORM_CARDS + ", " + self.CREATE_FORM_BUTTON, timeout=10)

    def get_form_count(self) -> int:
        cards = self.driver.find_elements(By.CSS_SELECTOR, self.FORM_CARDS)
        return len(cards) if cards else 0


class FormBuilderPage(BasePage):
    """Form builder page — FS19: Create/edit forms with drag & drop."""

    PATH = "/forms/builder"

    FIELD_PALETTE = ".field-palette, [data-testid='field-palette'], .component-palette"
    DRAG_FIELD = ".drag-field, [data-testid='drag-field'], .palette-item"
    FORM_CANVAS = ".form-canvas, [data-testid='form-canvas'], .canvas"
    FIELD_TYPES = ".field-type, [data-testid='field-type']"
    TEXT_FIELD = "[data-testid='field-text'], .field-text"
    NUMBER_FIELD = "[data-testid='field-number'], .field-number"
    DATE_FIELD = "[data-testid='field-date'], .field-date"
    DROPDOWN_FIELD = "[data-testid='field-dropdown'], .field-dropdown"
    TABLE_FIELD = "[data-testid='field-table'], .field-table"
    SAVE_BUTTON = "button:has-text('Save'), button:has-text('Uložit'), [data-testid='save-form']"
    PUBLISH_BUTTON = "button:has-text('Publish'), button:has-text('Publikovat'), [data-testid='publish-form']"
    PREVIEW_BUTTON = "button:has-text('Preview'), button:has-text('Náhled'), [data-testid='preview-form']"
    FORM_NAME_INPUT = "input[name='formName'], [data-testid='form-name-input']"
    SECTION_BUTTON = "button:has-text('Add Section'), button:has-text('Přidat sekci')"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.FIELD_PALETTE + ", " + self.FORM_CANVAS, timeout=10)

    def add_text_field(self, label: str = "Text Field") -> bool:
        """Add a text field to the form."""
        if self.click(self.TEXT_FIELD):
            time.sleep(0.5)
            return True
        return False

    def add_number_field(self, label: str = "Number Field") -> bool:
        if self.click(self.NUMBER_FIELD):
            time.sleep(0.5)
            return True
        return False

    def set_form_name(self, name: str) -> bool:
        return self.type_text(self.FORM_NAME_INPUT, name)


class FormFillPage(BasePage):
    """Form filling page — FS19: Fill out a form."""

    PATH = "/forms/fill"

    FORM_HEADER = ".form-header, [data-testid='form-header']"
    FIELD_INPUTS = "input[type='text'], input[type='number'], textarea, [data-testid='field-input']"
    REQUIRED_FIELDS = "[data-testid='required-field'], .required-field"
    AUTOSAVEIndicator = "[data-testid='autosave'], .autosave-indicator, .autosaved"
    SUBMIT_BUTTON = "button:has-text('Submit'), button:has-text('Odeslat'), [data-testid='submit-form']"
    SAVE_DRAFT_BUTTON = "button:has-text('Save Draft'), button:has-text('Uložit concept'), [data-testid='save-draft']"
    VALIDATION_ERRORS = ".validation-error, [data-testid='validation-error'], .Mui-error"
    COMMENT_BUTTON = "button:has-text('Comment'), button:has-text('Komentář')"
    EXPORT_EXCEL_BUTTON = "button:has-text('Export Excel'), button:has-text('Exportovat Excel')"
    IMPORT_EXCEL_BUTTON = "button:has-text('Import Excel'), button:has-text('Importovat Excel')"
    CHECKLIST = ".checklist, [data-testid='submission-checklist']"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.FORM_HEADER + ", " + self.FIELD_INPUTS, timeout=10)

    def fill_field(self, field_label: str, value: str) -> bool:
        """Fill a form field by its label."""
        # Try to find input near the label
        selectors = [
            f"input[name='{field_label}']",
            f"input[placeholder*='{field_label}']",
            f"textarea[name='{field_label}']",
            f"[data-testid='field-{field_label}']",
            f"label:has-text('{field_label}') + input",
            f"div:has-text('{field_label}') input"
        ]
        for selector in selectors:
            if self.type_text(selector, value):
                return True
        return False

    def submit_form(self) -> bool:
        if self.click(self.SUBMIT_BUTTON):
            time.sleep(2)
            return True
        return False

    def save_draft(self) -> bool:
        if self.click(self.SAVE_DRAFT_BUTTON):
            time.sleep(1)
            return True
        return False

    def get_validation_errors(self) -> list:
        errors = []
        error_elements = self.driver.find_elements(By.CSS_SELECTOR, self.VALIDATION_ERRORS)
        for el in error_elements:
            if el.text:
                errors.append(el.text)
        return errors


class ReportsListPage(BasePage):
    """Reports listing page — FS17: List of OPEX reports."""

    PATH = "/reports"

    REPORT_TABLE = ".report-table, [data-testid='report-table'], table.reports"
    REPORT_ROWS = ".report-row, [data-testid='report-row'], tbody tr"
    REPORT_NAME = ".report-name, [data-testid='report-name']"
    REPORT_STATUS = ".report-status, [data-testid='report-status'], .status-badge"
    PERIOD_COLUMN = ".period-column, [data-testid='period-column']"
    COMPANY_COLUMN = ".company-column, [data-testid='company-column']"
    SUBMIT_BUTTON = "button:has-text('Submit'), [data-testid='submit-report']"
    APPROVE_BUTTON = "button:has-text('Approve'), [data-testid='approve-report']"
    REJECT_BUTTON = "button:has-text('Reject'), [data-testid='reject-report']"
    FILTER_STATUS = "select[name='status'], [data-testid='status-filter']"
    FILTER_PERIOD = "select[name='period'], [data-testid='period-filter']"
    MATRIX_VIEW = "[data-testid='matrix-view'], .matrix-view"
    LIST_VIEW = "[data-testid='list-view'], .list-view"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.REPORT_TABLE + ", " + self.REPORT_ROWS, timeout=10)

    def get_report_count(self) -> int:
        rows = self.driver.find_elements(By.CSS_SELECTOR, self.REPORT_ROWS)
        return len(rows) if rows else 0

    def get_report_status(self, index: int = 0) -> str:
        """Get status of a specific report row."""
        try:
            rows = self.driver.find_elements(By.CSS_SELECTOR, self.REPORT_ROWS)
            if rows and index < len(rows):
                status_elements = rows[index].find_elements(By.CSS_SELECTOR, ".status-badge, [data-testid='status']")
                if status_elements:
                    return status_elements[0].text
        except Exception:
            pass
        return ""


class ReportDetailPage(BasePage):
    """Report detail page — FS17: View/report state transitions."""

    PATH = "/reports/"

    REPORT_HEADER = ".report-header, [data-testid='report-header']"
    STATUS_BADGE = ".status-badge, [data-testid='status-badge']"
    STATE_HISTORY = ".state-history, [data-testid='state-history'], .timeline"
    SUBMIT_FOR_REVIEW_BUTTON = "button:has-text('Submit'), button:has-text('Odeslat k revizi')"
    APPROVE_BUTTON = "button:has-text('Approve'), button:has-text('Schválit')"
    REJECT_BUTTON = "button:has-text('Reject'), button:has-text('Zamítnout')"
    REJECTION_COMMENT = "textarea[name='rejectionReason'], [data-testid='rejection-comment']"
    DATA_TAB = "button:has-text('Data'), [data-testid='tab-data']"
    HISTORY_TAB = "button:has-text('History'), [data-testid='tab-history']"
    ATTachmENTS_TAB = "button:has-text('Attachments'), [data-testid='tab-attachments']"
    CHECKLIST_TAB = "button:has-text('Checklist'), [data-testid='tab-checklist']"
    CHECKLIST_ITEMS = ".checklist-item, [data-testid='checklist-item']"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.REPORT_HEADER + ", " + self.STATUS_BADGE, timeout=10)

    def submit_for_review(self) -> bool:
        if self.click(self.SUBMIT_FOR_REVIEW_BUTTON):
            time.sleep(1)
            # Handle confirmation dialog if present
            return True
        return False

    def approve_report(self) -> bool:
        if self.click(self.APPROVE_BUTTON):
            time.sleep(1)
            return True
        return False

    def reject_report(self, reason: str) -> bool:
        if not self.click(self.REJECT_BUTTON):
            return False
        time.sleep(0.5)
        # Fill in rejection reason
        if self.is_element_present(self.REJECTION_COMMENT, timeout=3):
            self.type_text(self.REJECTION_COMMENT, reason)
        time.sleep(0.5)
        # Confirm rejection
        confirm_selectors = [
            "button:has-text('Confirm'), button:has-text('Potvrdit')",
            "button:has-text('Reject'), button:has-text('Zamítnout')"
        ]
        for selector in confirm_selectors:
            if self.click(selector, timeout=3):
                break
        return True

    def get_current_status(self) -> str:
        if self.is_element_present(self.STATUS_BADGE, timeout=3):
            return self.get_text(self.STATUS_BADGE)
        return ""

    def get_state_history(self) -> list:
        """Get list of state transitions."""
        history = []
        if self.is_element_present(self.STATE_HISTORY, timeout=3):
            items = self.driver.find_elements(By.CSS_SELECTOR, self.STATE_HISTORY + " li, .timeline-item")
            history = [item.text for item in items if item.text]
        return history


class PeriodsListPage(BasePage):
    """Periods listing page — FS20: Manage reporting periods."""

    PATH = "/periods"

    PERIOD_TABLE = ".period-table, [data-testid='period-table'], table.periods"
    PERIOD_ROWS = ".period-row, [data-testid='period-row'], tbody tr"
    PERIOD_NAME = ".period-name, [data-testid='period-name']"
    PERIOD_STATUS = ".period-status, [data-testid='period-status'], .status-badge"
    DEADLINE_COLUMN = ".deadline-column, [data-testid='deadline-column']"
    COMPLETION_COLUMN = ".completion-column, [data-testid='completion-column']"
    CREATE_PERIOD_BUTTON = "button:has-text('Create Period'), button:has-text('Vytvořit období')"
    CLONE_PERIOD_BUTTON = "button:has-text('Clone'), button:has-text('Klonovat')"
    VIEW_MATRIX_BUTTON = "button:has-text('View Matrix'), button:has-text('Zobrazit matici')"
    FILTER_STATUS = "select[name='status'], [data-testid='status-filter']"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.PERIOD_TABLE + ", " + self.PERIOD_ROWS, timeout=10)

    def get_period_count(self) -> int:
        rows = self.driver.find_elements(By.CSS_SELECTOR, self.PERIOD_ROWS)
        return len(rows) if rows else 0


class PeriodDetailPage(BasePage):
    """Period detail page — FS20: Period dashboard with company matrix."""

    PATH = "/periods/"

    PERIOD_HEADER = ".period-header, [data-testid='period-header']"
    MATRIX_VIEW = ".matrix-view, [data-testid='matrix-view']"
    MATRIX_CELL = ".matrix-cell, [data-testid='matrix-cell']"
    STATUS_LEGEND = ".status-legend, [data-testid='status-legend']"
    COMPLETION_BAR = "[data-testid='completion-bar'], .completion-bar"
    EXPORT_BUTTON = "button:has-text('Export'), [data-testid='export-period']"
    DEADLINE_WARNING = "[data-testid='deadline-warning'], .deadline-warning"
    ESCALATION_BUTTON = "button:has-text('Escalate'), [data-testid='escalate']"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.PERIOD_HEADER + ", " + self.MATRIX_VIEW, timeout=10)

    def get_completion_percentage(self) -> str:
        if self.is_element_present(self.COMPLETION_BAR, timeout=3):
            return self.get_text(self.COMPLETION_BAR)
        return "0%"

    def get_matrix_dimensions(self) -> tuple:
        """Get (rows, cols) of the matrix."""
        try:
            rows = self.driver.find_elements(By.CSS_SELECTOR, self.MATRIX_VIEW + " tr")
            if rows:
                cols = len(rows[0].find_elements(By.CSS_SELECTOR, "td, th"))
                return len(rows), cols
        except Exception:
            pass
        return 0, 0


class NotificationsPage(BasePage):
    """Notifications page — FS13: Notification center."""

    PATH = "/notifications"

    NOTIFICATION_LIST = ".notification-list, [data-testid='notification-list']"
    NOTIFICATION_ITEM = ".notification-item, [data-testid='notification-item']"
    NOTIFICATION_UNREAD = ".unread, [data-testid='unread']"
    MARK_READ_BUTTON = "button:has-text('Mark Read'), [data-testid='mark-read']"
    FILTER_TYPE = "select[name='type'], [data-testid='type-filter']"
    SETTINGS_BUTTON = "button:has-text('Settings'), [data-testid='notification-settings']"
    NOTIFICATION_BELL = "[data-testid='notification-bell'], .notification-bell"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.NOTIFICATION_LIST + ", " + self.NOTIFICATION_ITEM, timeout=10)

    def get_unread_count(self) -> int:
        try:
            unread = self.driver.find_elements(By.CSS_SELECTOR, self.NOTIFICATION_UNREAD)
            return len(unread)
        except Exception:
            return 0


class TemplatesPage(BasePage):
    """PPTX Templates page — FS18: Manage PPTX templates."""

    PATH = "/templates"

    TEMPLATE_CARDS = ".template-card, [data-testid='template-card']"
    TEMPLATE_NAME = ".template-name, [data-testid='template-name']"
    UPLOAD_TEMPLATE_BUTTON = "button:has-text('Upload Template'), button:has-text('Nahrát šablonu')"
    PLACEHOLDER_LIST = ".placeholder-list, [data-testid='placeholder-list']"
    VERSION_BADGE = ".version-badge, [data-testid='version-badge']"
    GENERATE_BUTTON = "button:has-text('Generate'), button:has-text('Generovat')"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.TEMPLATE_CARDS + ", " + self.UPLOAD_TEMPLATE_BUTTON, timeout=10)


class AdminPage(BasePage):
    """Admin page — FS07: User management, API keys, failed jobs."""

    PATH = "/admin"

    USERS_TAB = "button:has-text('Users'), [data-testid='tab-users']"
    ORGANIZATIONS_TAB = "button:has-text('Organizations'), [data-testid='tab-organizations']"
    API_KEYS_TAB = "button:has-text('API Keys'), [data-testid='tab-apikeys']"
    FAILED_JOBS_TAB = "button:has-text('Failed Jobs'), [data-testid='tab-failedjobs']"
    AUDIT_TAB = "button:has-text('Audit Log'), [data-testid='tab-audit']"
    USER_TABLE = ".users-table, [data-testid='users-table'], table.users"
    USER_ROWS = ".user-row, [data-testid='user-row']"
    INVITE_USER_BUTTON = "button:has-text('Invite User'), button:has-text('Pozvat uživatele')"
    FAILED_JOBS_TABLE = ".failed-jobs-table, [data-testid='failed-jobs-table']"
    REPROCESS_BUTTON = "button:has-text('Reprocess'), [data-testid='reprocess']"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.USERS_TAB + ", " + self.USER_TABLE, timeout=10)

    def click_failed_jobs_tab(self) -> bool:
        return self.click(self.FAILED_JOBS_TAB)

    def click_audit_tab(self) -> bool:
        return self.click(self.AUDIT_TAB)

    def get_failed_jobs_count(self) -> int:
        try:
            rows = self.driver.find_elements(By.CSS_SELECTOR, self.FAILED_JOBS_TABLE + " tbody tr")
            return len(rows)
        except Exception:
            return 0


class DashboardsPage(BasePage):
    """BI Dashboards page — FS11: Dashboard creation, chart display, SQL config."""

    PATH = "/dashboards"

    DASHBOARD_LIST = ".dashboard-list, [data-testid='dashboard-list'], .dashboard-grid"
    DASHBOARD_CARD = ".dashboard-card, [data-testid='dashboard-card'], .dashboard-item"
    CREATE_BUTTON = "button:has-text('Create Dashboard'), button:has-text('Vytvořit dashboard'), [data-testid='create-dashboard']"
    CHART_CONTAINER = ".recharts-wrapper, .nivo-chart, .chart-container, [data-testid='chart']"
    QUERY_CONFIG = "[data-testid='query-config'], .query-config"
    GROUP_BY = "[data-testid='group-by'], select[name='groupBy']"
    ORDER_BY = "[data-testid='order-by'], select[name='orderBy']"
    SQL_EDITOR = "[data-testid='sql-editor'], .sql-editor"
    DATE_FILTER = "[data-testid='date-filter'], input[type='date']"
    ORG_FILTER = "[data-testid='org-filter'], select[name='organization']"
    SOURCE_TYPE = "[data-testid='source-type'], .source-type"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.DASHBOARD_LIST + ", " + self.CREATE_BUTTON, timeout=10)

    def get_dashboard_count(self) -> int:
        cards = self.driver.find_elements(By.CSS_SELECTOR, self.DASHBOARD_CARD)
        return len(cards) if cards else 0


class VersioningPage(BasePage):
    """Data versioning page — FS14: Version history, diff tool."""

    PATH = "/files"

    VERSION_HISTORY = "[data-testid='version-history'], .version-history, [data-testid='versions']"
    VERSION_BADGE = ".version-badge, [data-testid='version-badge'], .version-tag"
    DIFF_BUTTON = "button:has-text('Compare'), button:has-text('Diff'), [data-testid='diff-tool']"
    DIFF_VIEW = ".diff-view, [data-testid='diff-view'], .diff-table"
    VERSION_SELECT_FROM = "select[name='fromVersion'], [data-testid='version-from']"
    VERSION_SELECT_TO = "select[name='toVersion'], [data-testid='version-to']"
    DIFF_ADDED = ".added, [data-testid='diff-added']"
    DIFF_REMOVED = ".removed, [data-testid='diff-removed']"
    DELTA_VALUE = ".delta-value, [data-testid='delta']"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.VERSION_HISTORY + ", " + self.VERSION_BADGE, timeout=10)


class SchemaMappingPage(BasePage):
    """Schema mapping page — FS15: Mapping editor, auto-suggest."""

    PATH = "/admin"

    MAPPING_LIST = ".mapping-list, [data-testid='mapping-list'], .mapping-table"
    MAPPING_EDITOR = "[data-testid='mapping-editor'], .mapping-editor"
    SOURCE_COLUMN = "[data-testid='source-column'], input[name='sourceColumn']"
    TARGET_COLUMN = "[data-testid='target-column'], input[name='targetColumn']"
    CREATE_MAPPING = "button:has-text('Create Mapping'), [data-testid='create-mapping']"
    AUTO_SUGGEST = "button:has-text('Auto Suggest'), [data-testid='auto-suggest']"
    MAPPING_ROW = ".mapping-row, .column-mapping"
    SLIDE_METADATA = "[data-testid='slide-metadata'], .slide-metadata"
    CONFIDENCE = ".mapping-confidence, [data-testid='confidence']"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.MAPPING_LIST + ", " + self.CREATE_MAPPING, timeout=10)


class AuditLogPage(BasePage):
    """Audit log page — FS16: Immutable audit trail, export."""

    PATH = "/admin/audit"

    AUDIT_TABLE = "[data-testid='audit-table'], .audit-table, table.audit"
    AUDIT_ROWS = "[data-testid='audit-row'], .audit-row, tbody tr"
    ACTION_FILTER = "select[name='actionType'], [data-testid='action-filter']"
    DATE_FILTER = "input[type='date'], [data-testid='date-filter']"
    USER_FILTER = "[data-testid='user-filter'], input[type='search']"
    EXPORT_CSV = "button:has-text('Export CSV'), [data-testid='export-csv']"
    EXPORT_JSON = "button:has-text('Export JSON'), [data-testid='export-json']"
    EXPORT_BUTTON = "button:has-text('Export'), [data-testid='export-audit']"

    def is_loaded(self) -> bool:
        return self.is_element_present(self.AUDIT_TABLE + ", " + self.AUDIT_ROWS, timeout=10)

    def get_log_count(self) -> int:
        rows = self.driver.find_elements(By.CSS_SELECTOR, self.AUDIT_ROWS)
        return len(rows) if rows else 0
