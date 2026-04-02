# Web UI Test Common Library for ReportAutomatization (RA)
# Adapted from tests/web/web_common.py for RA frontend testing
# Supports both Selenium (Chrome/Firefox/Edge) and integrates with UAT backend state

import sys
import os
import json
import datetime
import time
from typing import Optional

import warnings
warnings.filterwarnings("ignore")

_SHARED_DIR = os.path.dirname(os.path.abspath(__file__))
_WEB_UAT_DIR = os.path.dirname(_SHARED_DIR)
if _WEB_UAT_DIR not in sys.path:
    sys.path.insert(0, _WEB_UAT_DIR)

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

try:
    from selenium import webdriver
    from selenium.webdriver.common.by import By
    from selenium.webdriver.support.ui import WebDriverWait, Select
    from selenium.webdriver.support import expected_conditions as EC
    from selenium.webdriver.common.action_chains import ActionChains
    from selenium.webdriver.common.keys import Keys
    from selenium.common.exceptions import (
        TimeoutException, NoSuchElementException,
        StaleElementReferenceException, WebDriverException
    )
    SELENIUM_AVAILABLE = True
except ImportError:
    SELENIUM_AVAILABLE = False

from config.web_config import BASE_URL, SELENIUM_CONFIG, USERS, TIMEOUTS, STATE_FILE

_LOGS_DIR_DEFAULT = os.path.join(_WEB_UAT_DIR, "logs")
_SCREENSHOTS_DIR_DEFAULT = os.path.join(_WEB_UAT_DIR, "logs", "screenshots")

# By locator strategy values
_BY_STRATEGIES = frozenset({
    "css selector", "xpath", "id", "name",
    "class name", "tag name", "link text", "partial link text"
})

import re as _re

_HAS_TEXT_RE = _re.compile(r"^(\w+):has-text\(['\"](.+?)['\"]\)$")


def _convert_has_text(selector: str) -> tuple:
    """Convert Playwright :has-text() selectors to Selenium-compatible XPath."""
    parts = [p.strip() for p in selector.split(",")]
    converted = []
    has_any_ht = False

    for part in parts:
        m = _HAS_TEXT_RE.match(part)
        if m:
            tag, text = m.group(1), m.group(2)
            converted.append(f"//{tag}[contains(text(),'{text}') or .//*[contains(text(),'{text}')]]")
            has_any_ht = True
        elif ":has-text(" in part:
            prefix_m = _re.match(r"^(.+\s+)?(\w+):has-text\(['\"](.+?)['\"]\)$", part)
            if prefix_m:
                tag = prefix_m.group(2)
                text = prefix_m.group(3)
                converted.append(f"//{tag}[contains(text(),'{text}') or .//*[contains(text(),'{text}')]]")
                has_any_ht = True
            else:
                converted.append(part)
        else:
            converted.append(part)

    if has_any_ht:
        xpath_parts = [p for p in converted if p.startswith("//")]
        if xpath_parts:
            return By.XPATH, " | ".join(xpath_parts)
    return By.CSS_SELECTOR, selector


def _resolve_by(first, second=None) -> tuple:
    """Resolve (by, selector) vs (selector_only) calling convention."""
    if second is not None:
        by, sel = first, second
    else:
        by, sel = By.CSS_SELECTOR, first

    if by == By.CSS_SELECTOR and ":has-text(" in sel:
        return _convert_has_text(sel)

    return by, sel


class WebTestSession:
    """Holds runtime context for web UI tests: WebDriver, auth tokens, logs, state."""

    def __init__(
        self,
        base_url: str = None,
        browser: str = None,
        headless: bool = None,
        state_file: str = None,
        logs_dir: str = None
    ):
        self.base_url = (base_url or BASE_URL).rstrip("/")
        self.browser = browser or SELENIUM_CONFIG["browser"]
        self.headless = headless if headless is not None else SELENIUM_CONFIG["headless"]
        self.state_file = state_file or STATE_FILE
        self.logs_dir = logs_dir or _LOGS_DIR_DEFAULT

        self.driver: Optional[object] = None
        self.token: Optional[str] = None
        self.org_id: Optional[str] = None
        self.user_id: Optional[str] = None
        self.user_key: Optional[str] = None

        # Expose By for convenient access
        self.by = By

        self._log_lines: list[str] = []
        self._error_lines: list[str] = []
        self._pass_count = 0
        self._fail_count = 0

        os.makedirs(self.logs_dir, exist_ok=True)
        os.makedirs(_SCREENSHOTS_DIR_DEFAULT, exist_ok=True)

    def _ts(self) -> str:
        return datetime.datetime.now().strftime("%H:%M:%S")

    def _log(self, line: str) -> None:
        full = f"[{self._ts()}] {line}"
        self._log_lines.append(full)
        print(full)

    def _err(self, line: str) -> None:
        self._log_lines.append(f"[{self._ts()}] {line}")
        self._error_lines.append(line)
        print(line, file=sys.stderr)

    # ------------------------------------------------------------------
    # State persistence (shared with backend UAT via uat_state.json)
    # ------------------------------------------------------------------
    def load_state(self) -> dict:
        if os.path.exists(self.state_file):
            try:
                with open(self.state_file, "r", encoding="utf-8") as f:
                    return json.load(f)
            except (json.JSONDecodeError, IOError):
                pass
        # Fallback: try UAT state file location
        uat_state = os.path.join(os.path.dirname(self.state_file), "../UAT/logs/uat_state.json")
        if os.path.exists(uat_state):
            try:
                with open(uat_state, "r", encoding="utf-8") as f:
                    return json.load(f)
            except (json.JSONDecodeError, IOError):
                pass
        return {"sessions": {}, "screenshot_count": 0}

    def save_state(self, state: dict) -> None:
        os.makedirs(os.path.dirname(self.state_file), exist_ok=True)
        with open(self.state_file, "w", encoding="utf-8") as f:
            json.dump(state, f, indent=2, ensure_ascii=False)

    def update_state(self, patch: dict) -> dict:
        state = self.load_state()
        for key, value in patch.items():
            if isinstance(value, dict) and isinstance(state.get(key), dict):
                state[key].update(value)
            else:
                state[key] = value
        self.save_state(state)
        return state

    # ------------------------------------------------------------------
    # WebDriver setup
    # ------------------------------------------------------------------
    def init_driver(self) -> bool:
        if not SELENIUM_AVAILABLE:
            self._err("[FAIL] Selenium not installed. Run: pip install selenium")
            return False

        try:
            if self.browser == "chrome":
                options = webdriver.ChromeOptions()
                if self.headless:
                    options.add_argument("--headless=new")
                options.add_argument("--no-sandbox")
                options.add_argument("--disable-dev-shm-usage")
                options.add_argument(f"--window-size={SELENIUM_CONFIG['window_width']},{SELENIUM_CONFIG['window_height']}")
                options.add_argument("--disable-gpu")
                options.add_argument("--disable-extensions")
                options.add_argument("--disable-logging")
                options.add_argument("--log-level=3")
                options.add_argument("--ignore-certificate-errors")
                options.add_argument("--allow-insecure-localhost")
                prefs = {"credentials_enable_service": False, "profile.password_manager_enabled": False}
                options.add_experimental_option("prefs", prefs)
                options.add_experimental_option("excludeSwitches", ["enable-logging"])
                self.driver = webdriver.Chrome(options=options)
            elif self.browser == "firefox":
                options = webdriver.FirefoxOptions()
                if self.headless:
                    options.add_argument("--headless")
                options.add_argument(f"--width={SELENIUM_CONFIG['window_width']}")
                options.add_argument(f"--height={SELENIUM_CONFIG['window_height']}")
                options.accept_insecure_certs = True
                self.driver = webdriver.Firefox(options=options)
            elif self.browser == "edge":
                options = webdriver.EdgeOptions()
                if self.headless:
                    options.add_argument("--headless=new")
                options.add_argument(f"--window-size={SELENIUM_CONFIG['window_width']},{SELENIUM_CONFIG['window_height']}")
                options.add_argument("--ignore-certificate-errors")
                self.driver = webdriver.Edge(options=options)
            else:
                self._err(f"[FAIL] Unsupported browser: {self.browser}")
                return False

            self.driver.implicitly_wait(SELENIUM_CONFIG["implicit_wait"])
            self.driver.set_page_load_timeout(SELENIUM_CONFIG["page_load_timeout"])
            self.driver.set_script_timeout(SELENIUM_CONFIG["script_timeout"])
            self._log(f"[OK]   WebDriver initialized: {self.browser} (headless={self.headless})")
            return True

        except WebDriverException as exc:
            self._err(f"[FAIL] WebDriver init failed: {exc}")
            return False

    def quit_driver(self) -> None:
        if self.driver:
            try:
                self.driver.quit()
                self._log("[INFO] WebDriver quit")
            except Exception:
                pass
            self.driver = None

    # ------------------------------------------------------------------
    # Navigation helpers
    # ------------------------------------------------------------------
    def navigate_to(self, path: str, wait_for_load: bool = True) -> bool:
        url = self.base_url + path
        try:
            self.driver.get(url)
            if wait_for_load:
                time.sleep(0.5)
            self._log(f"[NAV]  {url}")
            return True
        except Exception as exc:
            self._err(f"[FAIL] Navigation failed: {exc}")
            return False

    def refresh(self) -> bool:
        try:
            self.driver.refresh()
            time.sleep(0.5)
            return True
        except Exception:
            return False

    def go_back(self) -> bool:
        try:
            self.driver.back()
            time.sleep(0.5)
            return True
        except Exception:
            return False

    def wait_for_load(self, timeout=None) -> bool:
        timeout = timeout or TIMEOUTS.get("page_load", 30)
        try:
            WebDriverWait(self.driver, timeout).until(
                lambda d: d.execute_script("return document.readyState") == "complete"
            )
            time.sleep(0.3)
            return True
        except Exception:
            return False

    # ------------------------------------------------------------------
    # Element finding helpers
    # ------------------------------------------------------------------
    def find_element(self, by_or_selector, selector=None, timeout=None) -> Optional[object]:
        by, sel = _resolve_by(by_or_selector, selector)
        timeout = timeout or TIMEOUTS.get("explicit_wait", 10)
        try:
            wait = WebDriverWait(self.driver, timeout)
            element = wait.until(EC.presence_of_element_located((by, sel)))
            return element
        except (TimeoutException, NoSuchElementException):
            return None

    def find_elements(self, by_or_selector, selector=None, timeout=None) -> list:
        by, sel = _resolve_by(by_or_selector, selector)
        timeout = timeout or TIMEOUTS.get("explicit_wait", 10)
        try:
            wait = WebDriverWait(self.driver, timeout)
            wait.until(EC.presence_of_element_located((by, sel)))
            return self.driver.find_elements(by, sel)
        except (TimeoutException, NoSuchElementException):
            return []

    def click(self, by_or_selector, selector=None, timeout=None) -> bool:
        by, sel = _resolve_by(by_or_selector, selector)
        try:
            element = self.find_element(by, sel, timeout)
            if element:
                element.click()
                time.sleep(0.3)
                return True
            return False
        except Exception as exc:
            self._err(f"[FAIL] Click failed on {sel}: {exc}")
            return False

    def type_text(self, by_or_selector, selector_or_text=None, text=None,
                  clear_first=True, clear=None) -> bool:
        if clear is not None:
            clear_first = clear
        if text is not None:
            by, selector = by_or_selector, selector_or_text
        elif selector_or_text is not None:
            by = By.CSS_SELECTOR
            selector = by_or_selector
            text = selector_or_text
        else:
            self._err("[FAIL] type_text: selector and text required")
            return False
        try:
            element = self.find_element(by, selector)
            if element:
                if clear_first:
                    element.clear()
                element.send_keys(text)
                return True
            return False
        except Exception as exc:
            self._err(f"[FAIL] Type text failed on {selector}: {exc}")
            return False

    def get_text(self, by_or_selector, selector=None, timeout=None) -> str:
        by, sel = _resolve_by(by_or_selector, selector)
        element = self.find_element(by, sel, timeout)
        if element:
            return element.text or ""
        return ""

    def get_attribute(self, by_or_selector, selector_or_attr=None, attr=None,
                      timeout=None) -> str:
        if attr is not None:
            by, selector = by_or_selector, selector_or_attr
        elif selector_or_attr is not None:
            by = By.CSS_SELECTOR
            selector = by_or_selector
            attr = selector_or_attr
        else:
            return ""
        element = self.find_element(by, selector, timeout)
        if element:
            return element.get_attribute(attr) or ""
        return ""

    def is_visible(self, by_or_selector, selector=None, timeout=None) -> bool:
        by, sel = _resolve_by(by_or_selector, selector)
        try:
            element = self.find_element(by, sel, timeout)
            return element is not None and element.is_displayed()
        except Exception:
            return False

    def is_element_present(self, by_or_selector, selector=None, timeout=None) -> bool:
        """Check if element exists in DOM (may or may not be visible)."""
        by, sel = _resolve_by(by_or_selector, selector)
        try:
            return self.find_element(by, sel, timeout) is not None
        except Exception:
            return False

    def is_enabled(self, by_or_selector, selector=None, timeout=None) -> bool:
        by, sel = _resolve_by(by_or_selector, selector)
        try:
            element = self.find_element(by, sel, timeout)
            return element is not None and element.is_enabled()
        except Exception:
            return False

    def wait_for_url_contains(self, text: str, timeout: int = None) -> bool:
        timeout = timeout or TIMEOUTS.get("explicit_wait", 10)
        try:
            wait = WebDriverWait(self.driver, timeout)
            wait.until(EC.url_contains(text))
            return True
        except TimeoutException:
            return False

    def wait_for_element_visible(self, by_or_selector, selector=None, timeout=None) -> bool:
        by, sel = _resolve_by(by_or_selector, selector)
        timeout = timeout or TIMEOUTS.get("explicit_wait", 10)
        try:
            wait = WebDriverWait(self.driver, timeout)
            wait.until(EC.visibility_of_element_located((by, sel)))
            return True
        except TimeoutException:
            return False

    def wait_for_element_clickable(self, by_or_selector, selector=None, timeout=None) -> bool:
        by, sel = _resolve_by(by_or_selector, selector)
        timeout = timeout or TIMEOUTS.get("explicit_wait", 10)
        try:
            wait = WebDriverWait(self.driver, timeout)
            wait.until(EC.element_to_be_clickable((by, sel)))
            return True
        except TimeoutException:
            return False

    def execute_script(self, script: str, *args) -> any:
        try:
            return self.driver.execute_script(script, *args)
        except Exception:
            return None

    # ------------------------------------------------------------------
    # Screenshot helpers
    # ------------------------------------------------------------------
    def take_screenshot(self, name: str = None) -> str:
        if not self.driver:
            return ""
        os.makedirs(_SCREENSHOTS_DIR_DEFAULT, exist_ok=True)
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{name or 'screenshot'}_{timestamp}.png"
        filepath = os.path.join(_SCREENSHOTS_DIR_DEFAULT, filename)
        try:
            self.driver.save_screenshot(filepath)
            self._log(f"[INFO] Screenshot saved: {filename}")
            return filepath
        except Exception as exc:
            self._log(f"[WARN] Screenshot failed: {exc}")
            return ""

    # ------------------------------------------------------------------
    # Authentication (dev bypass — uses API key from state)
    # ------------------------------------------------------------------
    # Cache for discovered login selectors (shared across login calls)
    _login_cache: dict = {}

    def login(self, email: str, password: str = "") -> bool:
        """Login — handles three scenarios:

        1. **No-auth DEV mode** (direct localhost:5173, no router):
           No login page exists. Navigate to /dashboard and confirm app loads.
        2. **Dev bypass** (login page exists but empty password accepted):
           Fill email, submit, verify redirect.
        3. **MSAL / real auth** (production-like):
           Full login form with email + password.

        Detection: navigate to /dashboard first.  If the app renders content
        (no redirect to /login), we are in no-auth mode → done instantly.
        """
        self._log(f"[AUTH] Login attempt: {email}")
        cache = WebTestSession._login_cache

        # ------------------------------------------------------------------
        # 0.  Already logged in?  (fast path for repeated calls)
        # ------------------------------------------------------------------
        if self.driver.current_url and self._check_logged_in():
            self.user_key = self._identify_user(email)
            self._log(f"[OK]   Already logged in, reusing session for {email}")
            return True

        # ------------------------------------------------------------------
        # 1.  Detect no-auth DEV mode:  go to /dashboard directly.
        #     If the app shows content (no redirect to /login), we're done.
        # ------------------------------------------------------------------
        if not cache.get("auth_mode"):
            self.navigate_to("/dashboard", wait_for_load=False)
            time.sleep(1)
            self.wait_for_load(timeout=10)

            current_url = self.driver.current_url.lower()
            redirected_to_login = any(kw in current_url for kw in ("login", "signin", "auth"))

            if not redirected_to_login:
                # No-auth DEV mode — app loaded without login
                cache["auth_mode"] = "noauth"
                self.user_key = self._identify_user(email)
                self._log(f"[OK]   No-auth DEV mode detected — app accessible without login")
                return True
            else:
                cache["auth_mode"] = "login"
                self._log(f"[INFO] Login page detected — using login flow")

        # If previously detected no-auth, just navigate to dashboard
        if cache.get("auth_mode") == "noauth":
            # Ensure we are on a page (not about:blank)
            if "localhost" not in self.driver.current_url:
                self.navigate_to("/dashboard", wait_for_load=False)
                time.sleep(0.5)
            self.user_key = self._identify_user(email)
            self._log(f"[OK]   No-auth DEV mode — skipping login for {email}")
            return True

        # ------------------------------------------------------------------
        # 2.  Login flow (dev-bypass or real auth)
        # ------------------------------------------------------------------
        state = self.load_state()
        api_key = state.get("api_key")
        dev_token = state.get("dev_token")

        # Try cached path first, then primary, then fallback
        cached_path = cache.get("login_path")
        login_paths = ["/login", "/auth/login", "/signin"]
        if cached_path:
            login_paths = [cached_path] + [p for p in login_paths if p != cached_path]

        logged_in = False
        for path in login_paths:
            if self.navigate_to(path, wait_for_load=False):
                time.sleep(0.5)
                if self._check_logged_in():
                    logged_in = True
                    cache["login_path"] = path
                    break
                if self._perform_dev_bypass_login(email, api_key, dev_token):
                    logged_in = True
                    cache["login_path"] = path
                    break
            # Only try first 2 paths to avoid long waits
            if path == login_paths[1] and not logged_in:
                break

        if not logged_in:
            self._err(f"[FAIL] Login failed for {email}")
            self.take_screenshot("login_failed")
            return False

        self.user_key = self._identify_user(email)
        self._log(f"[OK]   Logged in as {email} (user_key={self.user_key})")
        return True

    def _check_logged_in(self) -> bool:
        """Check if user is already logged in (session persistence or no-auth)."""
        try:
            current_url = self.driver.current_url.lower()
        except Exception:
            return False
        # If we're on an app page and not on login page, consider it logged in
        app_keywords = ("dashboard", "files", "forms", "reports", "periods",
                        "admin", "upload", "viewer", "templates", "notifications",
                        "settings", "dashboards")
        if any(kw in current_url for kw in app_keywords):
            if "login" not in current_url and "signin" not in current_url:
                return True
        # Quick check for user menu (short timeout to avoid blocking)
        user_menu_selectors = [
            "[data-testid='user-menu']",
            "[data-testid='user-avatar']",
            ".user-menu",
        ]
        for selector in user_menu_selectors:
            if self.is_visible(selector, timeout=1):
                return True
        return False

    def _perform_dev_bypass_login(self, email: str, api_key: str = None, dev_token: str = None) -> bool:
        """Perform login using dev bypass token or API key.

        Optimized: uses cached selectors from previous successful logins,
        short timeouts for element discovery.
        """
        by = By.CSS_SELECTOR
        cache = WebTestSession._login_cache

        # Find email input — use cached selector or discover
        email_input = None
        cached_email_sel = cache.get("email_selector")
        if cached_email_sel:
            email_input = self.find_element(by, cached_email_sel, timeout=2)

        if not email_input:
            email_selectors = [
                "input[type='email']", "input[name='email']", "#email",
                "[data-testid='email-input']", "input[placeholder*='email']"
            ]
            for selector in email_selectors:
                el = self.find_element(by, selector, timeout=1)
                if el:
                    email_input = el
                    cache["email_selector"] = selector
                    break

        if not email_input:
            return self._check_logged_in()

        # Find password input — use cached or discover (short timeouts)
        pass_input = None
        cached_pass_sel = cache.get("password_selector")
        if cached_pass_sel:
            pass_input = self.find_element(by, cached_pass_sel, timeout=1)

        if not pass_input:
            password_selectors = [
                "input[type='password']", "input[name='password']", "#password",
                "[data-testid='password-input']"
            ]
            for selector in password_selectors:
                el = self.find_element(by, selector, timeout=0.5)
                if el:
                    pass_input = el
                    cache["password_selector"] = selector
                    break

        # Find submit button — use cached or discover (short timeouts)
        submit_btn = None
        cached_submit_sel = cache.get("submit_selector")
        if cached_submit_sel:
            submit_btn = self.find_element(by, cached_submit_sel, timeout=1)

        if not submit_btn:
            submit_selectors = [
                "button[type='submit']", "button[data-testid='login-button']",
                "button:has-text('Sign in')", "button:has-text('Login')",
                "button:has-text('Přihlásit')"
            ]
            for selector in submit_selectors:
                el = self.find_element(by, selector, timeout=0.5)
                if el and el.is_enabled():
                    submit_btn = el
                    cache["submit_selector"] = selector
                    break

        if pass_input:
            pass_input.clear()
        email_input.clear()
        email_input.send_keys(email)

        if submit_btn:
            submit_btn.click()
        time.sleep(1.5)

        # Check for error message (short timeout)
        if self.is_visible(by, ".alert-danger, .error-message, [data-testid='error-message']", timeout=1):
            self._err(f"[FAIL] Login rejected for {email}")
            return False

        return True

    def _identify_user(self, email: str) -> Optional[str]:
        for key, user in USERS.items():
            if user["email"] == email:
                return key
        return None

    def logout(self) -> bool:
        # In no-auth DEV mode, logout is a no-op — just navigate to root
        if WebTestSession._login_cache.get("auth_mode") == "noauth":
            self._log("[OK]   No-auth DEV mode — logout is no-op")
            self.navigate_to("/", wait_for_load=True)
            return True

        by = By.CSS_SELECTOR
        logout_selectors = [
            "button[data-testid='user-menu']",
            "[data-testid='user-dropdown']",
            ".user-menu button",
            "button:has-text('Logout')",
            "button:has-text('Sign out')",
            "button:has-text('Odhlásit')",  # Czech
            "a[href='/logout']",
            "a[href='/auth/logout']",
        ]

        for selector in logout_selectors:
            if self.click(by, selector, timeout=3):
                time.sleep(1)
                break

        self.navigate_to("/login", wait_for_load=True)
        time.sleep(1)

        if "login" in self.driver.current_url.lower():
            self._log("[OK]   Logged out")
            return True
        return False

    # ------------------------------------------------------------------
    # Assertion helpers
    # ------------------------------------------------------------------
    def assert_true(self, condition: bool, message: str) -> bool:
        if condition:
            self._pass_count += 1
            self._log(f"[OK]   {message}")
        else:
            self._fail_count += 1
            self._err(f"[FAIL] {message}")
            self._error_lines.append(f"## Assertion Failed\n- Message: {message}\n")
        return condition

    def assert_equal(self, actual: any, expected: any, message: str = None) -> bool:
        condition = actual == expected
        msg = message or f"Expected {expected!r}, got {actual!r}"
        return self.assert_true(condition, msg)

    def assert_visible(self, by_or_selector, selector_or_message=None, message=None) -> bool:
        if message is not None:
            by, sel = by_or_selector, selector_or_message
        elif selector_or_message is not None:
            if by_or_selector in _BY_STRATEGIES:
                by, sel = by_or_selector, selector_or_message
            else:
                by, sel = By.CSS_SELECTOR, by_or_selector
                message = selector_or_message
        else:
            by, sel = By.CSS_SELECTOR, by_or_selector
        visible = self.is_visible(by, sel)
        msg = message or f"Element visible: {sel}"
        return self.assert_true(visible, msg)

    def assert_text_contains(self, by_or_selector, selector_or_text=None,
                             text_or_message=None, message=None) -> bool:
        if message is not None:
            by, sel, text = by_or_selector, selector_or_text, text_or_message
        elif text_or_message is not None:
            if by_or_selector in _BY_STRATEGIES:
                by, sel, text = by_or_selector, selector_or_text, text_or_message
            else:
                by = By.CSS_SELECTOR
                sel = by_or_selector
                text = selector_or_text
                message = text_or_message
        elif selector_or_text is not None:
            by = By.CSS_SELECTOR
            sel = by_or_selector
            text = selector_or_text
        else:
            return self.assert_true(False, "assert_text_contains: missing arguments")
        actual_text = self.get_text(by, sel).lower()
        contains = text.lower() in actual_text
        msg = message or f"Element text contains '{text}' (got '{actual_text[:50]}')"
        return self.assert_true(contains, msg)

    def assert_url_contains(self, text: str, message: str = None) -> bool:
        contains = text in self.driver.current_url
        msg = message or f"URL contains '{text}' (got '{self.driver.current_url}')"
        return self.assert_true(contains, msg)

    def missing_feature(self, feature: str, description: str) -> None:
        line = f"[MISSING_FEATURE] {feature} — {description}"
        self._log(line)
        self._error_lines.append(f"## Missing Feature (informational)\n- Feature: `{feature}`\n- Description: {description}\n")

    # ------------------------------------------------------------------
    # Save logs
    # ------------------------------------------------------------------
    def save_log(self, step_name: str) -> bool:
        timestamp = datetime.datetime.now().isoformat(timespec="seconds")
        summary = (
            f"\n{'='*60}\n"
            f"Step: {step_name}\n"
            f"Timestamp: {timestamp}\n"
            f"PASSED: {self._pass_count}  FAILED: {self._fail_count}\n"
            f"{'='*60}\n"
        )
        print(summary)

        if self._error_lines:
            err_path = os.path.join(self.logs_dir, f"{step_name}_errors.md")
            with open(err_path, "w", encoding="utf-8") as f:
                f.write(f"# Web UI Test Errors - {step_name}\n\nTimestamp: {timestamp}\n\n")
                f.write("\n".join(self._error_lines))
            print(f"[INFO] Errors: {err_path}")

        return self._fail_count == 0

    def exit_code(self) -> int:
        return 0 if self._fail_count == 0 else 1


class PageObject:
    """Base class for page objects."""

    def __init__(self, session: WebTestSession):
        self.session = session
        self.driver = session.driver
        self.by = By

    def navigate(self, path: str = "") -> bool:
        return self.session.navigate_to(self.PATH + path)

    def is_loaded(self) -> bool:
        raise NotImplementedError

    def wait_for_load(self, timeout: int = None) -> bool:
        timeout = timeout or TIMEOUTS.get("explicit_wait", 10)
        try:
            wait = WebDriverWait(self.driver, timeout)
            wait.until(lambda d: self.is_loaded())
            return True
        except TimeoutException:
            return False


def login(session: WebTestSession, email: str, password: str = "") -> bool:
    return session.login(email, password)


def logout(session: WebTestSession) -> bool:
    return session.logout()
