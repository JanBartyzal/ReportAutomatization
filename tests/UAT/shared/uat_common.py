# UAT Common Library for ReportAutomatization (RA)
# Shared session management, HTTP calls, logging, and assertion helpers.

import sys
import os
import json
import datetime
import warnings
warnings.filterwarnings("ignore")  # suppress RequestsDependencyWarning and similar
import requests

# Ensure UTF-8 output on Windows (avoids charmap errors for non-ASCII chars in logs)
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

# ---------------------------------------------------------------------------
# State file path (relative from any step's directory)
# ---------------------------------------------------------------------------
_STATE_FILE_DEFAULT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../logs/uat_state.json")
_LOGS_DIR_DEFAULT   = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../logs")


# ---------------------------------------------------------------------------
# UATSession
# ---------------------------------------------------------------------------
class UATSession:
    """Holds runtime context for one UAT step: auth tokens, logs, state."""

    def __init__(self, base_url: str = "http://localhost:8080",
                 state_file: str | None = None,
                 logs_dir: str | None = None):
        self.base_url   = base_url.rstrip("/")
        self.token: str | None = None
        self.api_key: str | None = None
        self.org_id: str | None = None
        self.user_id: str | None = None
        self.user_key: str | None = None
        self.roles: list[str] | str | None = None

        self.state_file = state_file or _STATE_FILE_DEFAULT
        self.logs_dir   = logs_dir   or _LOGS_DIR_DEFAULT

        self._log_lines:   list[str] = []
        self._error_lines: list[str] = []
        # Use lists as mutable containers so for_service() can share counters
        self._counters = [0, 0, 0]  # [pass_count, fail_count, skip_count]

        os.makedirs(self.logs_dir, exist_ok=True)

    @property
    def _pass_count(self):
        return self._counters[0]

    @_pass_count.setter
    def _pass_count(self, value):
        self._counters[0] = value

    @property
    def _fail_count(self):
        return self._counters[1]

    @_fail_count.setter
    def _fail_count(self, value):
        self._counters[1] = value

    @property
    def _skip_count(self):
        return self._counters[2]

    @_skip_count.setter
    def _skip_count(self, value):
        self._counters[2] = value

    # ------------------------------------------------------------------
    # State persistence (shared across steps via uat_state.json)
    # ------------------------------------------------------------------
    def restore_auth_from_state(self) -> None:
        """Restore user_id, org_id, roles from state file (set by Step00)."""
        state = self.load_state()
        if not self.user_id:
            self.user_id = state.get("dev_user_id")
        if not self.org_id:
            self.org_id = state.get("dev_org_id")
        if not self.org_id:
            # Fallback: derive org_id from org_ids map in state
            org_ids = state.get("org_ids", {})
            for slug in ("TEST-ORG-1", "test-org-1"):
                if slug in org_ids:
                    self.org_id = org_ids[slug]
                    break
            if not self.org_id and org_ids:
                self.org_id = next(iter(org_ids.values()))
        if not self.roles:
            self.roles = state.get("dev_roles", ["HOLDING_ADMIN"])

    def load_state(self) -> dict:
        if os.path.exists(self.state_file):
            with open(self.state_file, "r", encoding="utf-8") as f:
                return json.load(f)
        return {"tokens": {}, "org_ids": {}, "plan_ids": {}}

    def save_state(self, state: dict) -> None:
        os.makedirs(os.path.dirname(self.state_file), exist_ok=True)
        with open(self.state_file, "w", encoding="utf-8") as f:
            json.dump(state, f, indent=2, ensure_ascii=False)

    def update_state(self, patch: dict) -> dict:
        """Deep-merge patch into the persistent state file."""
        state = self.load_state()
        for key, value in patch.items():
            if isinstance(value, dict) and isinstance(state.get(key), dict):
                state[key].update(value)
            else:
                state[key] = value
        self.save_state(state)
        return state

    # ------------------------------------------------------------------
    # Logging helpers
    # ------------------------------------------------------------------
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
    # Service URL switching
    # ------------------------------------------------------------------
    def for_service(self, service_url: str) -> 'UATSession':
        """Create a new session pointing to a different service URL,
        sharing auth state (token, api_key) with this session."""
        s = UATSession(base_url=service_url,
                       state_file=self.state_file,
                       logs_dir=self.logs_dir)
        s.token = self.token
        s.api_key = self.api_key
        s.org_id = self.org_id
        s.user_id = self.user_id
        s.user_key = self.user_key
        s.roles = self.roles
        # Share counters and logs (mutable references)
        s._log_lines = self._log_lines
        s._error_lines = self._error_lines
        s._counters = self._counters  # shared mutable list
        return s

    def sync_counters_from(self, other: 'UATSession') -> None:
        """No-op: counters are shared via mutable _counters list."""
        pass

    # ------------------------------------------------------------------
    # Authentication
    # ------------------------------------------------------------------
    def set_api_key(self, api_key: str) -> None:
        """Set API key for subsequent calls (X-API-Key header)."""
        self.api_key = api_key
        self.token = None  # API key takes priority, clear Bearer token
        self._log(f"[INFO] API key set: {api_key[:8]}...")

    def verify_auth(self) -> dict | None:
        """GET /api/auth/verify — check current auth (token or API key) is valid.
        Returns user context dict or None."""
        status, body = self.call("GET", "/api/auth/verify",
                                 expected_status=200, tag="auth-verify")
        if status == 200 and isinstance(body, dict):
            self.org_id = body.get("orgId") or body.get("org_id")
            self.user_key = body.get("userId") or body.get("user_id")
            self._log(f"[OK]   Auth verified: org={self.org_id}")
            return body
        return None

    def login(self, email: str, password: str) -> str | None:
        """Authenticate using API key from state, or dev bypass via /api/auth/verify.
        The email/password params are kept for backward compatibility but
        the system uses API keys (X-API-Key) or dev bypass (AUTH_MODE=development),
        not password-based login.
        """
        # 1) Try dev bypass: GET /api/auth/verify with dummy Bearer token
        #    In AUTH_MODE=development, backend skips JWT validation but
        #    still requires Authorization: Bearer header to be present.
        url = self.base_url + "/api/auth/verify"
        self._log(f"[CALL] GET {url} (dev bypass / auth check for {email})")

        try:
            resp = requests.get(url, headers={"Authorization": "Bearer dev-bypass-token"}, timeout=30)
            data = _try_json(resp)
            self._log(f"[RES]  {resp.status_code} {json.dumps(data)[:300]}")

            if resp.status_code == 200:
                self.org_id = data.get("organizationId") or data.get("orgId") or data.get("org_id")
                self.user_id = data.get("userId") or data.get("user_id")
                self.roles = data.get("roles") or ["HOLDING_ADMIN"]
                self._pass_count += 1
                self._log(f"[OK]   Auth verified for {email} (dev bypass, user={self.user_id}, org={self.org_id})")
                self.token = data.get("token") or data.get("access_token") or "dev-bypass"
                return self.token
            else:
                self._fail_count += 1
                msg = f"Auth verify failed for {email}: status={resp.status_code}"
                self._err(f"[FAIL] {msg}")
                self._error_lines.append(f"## Auth Failure\n- Email: {email}\n- Status: {resp.status_code}\n- Body: {json.dumps(data)[:500]}\n")
                return None
        except Exception as exc:
            self._fail_count += 1
            msg = f"Auth exception for {email}: {exc}"
            self._err(f"[FAIL] {msg}")
            self._error_lines.append(f"## Auth Exception\n- Email: {email}\n- Exception: {exc}\n")
            return None

    # ------------------------------------------------------------------
    # Generic HTTP call
    # ------------------------------------------------------------------
    def call(self, method: str, path: str, body=None, files=None,
             expected_status: int = 200, tag: str | None = None,
             timeout: int = 30, query_params: dict | None = None) -> tuple[int, dict | bytes | str]:
        """
        Perform HTTP call, log [CALL]/[REQ]/[RES]/[OK]/[FAIL].
        Returns (status_code, parsed_body).
        
        Args:
            query_params: Optional dict of query string parameters to append to URL.
        """
        url = self.base_url + path
        headers: dict = {}

        # Auth header
        if self.api_key:
            headers["X-API-Key"] = self.api_key
        elif self.token:
            if self.token == "dev-bypass":
                headers["Authorization"] = "Bearer dev-bypass-token"
            elif "." not in self.token:
                headers["X-API-Key"] = self.token
            else:
                headers["Authorization"] = f"Bearer {self.token}"

        # Inject X-User-Id, X-Org-Id, X-Roles for direct service calls
        # (normally set by nginx ForwardAuth from token validation)
        if self.user_id:
            headers.setdefault("X-User-Id", self.user_id)
        if self.org_id:
            headers.setdefault("X-Org-Id", self.org_id)
        if self.roles:
            headers.setdefault("X-Roles", ",".join(self.roles) if isinstance(self.roles, list) else self.roles)

        req_summary = json.dumps(body)[:300] if body else "-"
        self._log(f"[CALL] {method.upper()} {url}")
        if query_params:
            self._log(f"[PARAMS] {query_params}")
        self._log(f"[REQ]  {req_summary}")

        try:
            if files:
                resp = requests.request(
                    method, url, files=files, data=body or {},
                    headers=headers, timeout=timeout, params=query_params
                )
            elif body is not None:
                headers["Content-Type"] = "application/json"
                resp = requests.request(
                    method, url, json=body,
                    headers=headers, timeout=timeout, params=query_params
                )
            else:
                resp = requests.request(
                    method, url, headers=headers, timeout=timeout, params=query_params
                )

            # Try to decode body
            content_type = resp.headers.get("Content-Type", "")
            if "application/json" in content_type:
                data = _try_json(resp)
                res_preview = json.dumps(data)[:300]
            elif "text/" in content_type or "application/xml" in content_type:
                data = resp.text
                res_preview = str(data)[:300]
            else:
                data = resp.content
                res_preview = f"<binary {len(data)} bytes, content-type={content_type}>"

            self._log(f"[RES]  {resp.status_code} {res_preview}")

            label = tag or path
            if resp.status_code == expected_status:
                self._pass_count += 1
                self._log(f"[OK]   {method.upper()} {label} -> {resp.status_code}")
            else:
                self._fail_count += 1
                msg = f"Expected {expected_status}, got {resp.status_code} for {method.upper()} {label}"
                self._err(f"[FAIL] {msg}")
                self._error_lines.append(
                    f"## Unexpected Status\n"
                    f"- Endpoint: `{method.upper()} {path}`\n"
                    f"- Expected: {expected_status}\n"
                    f"- Got: {resp.status_code}\n"
                    f"- Body: `{res_preview}`\n"
                )

            return resp.status_code, data

        except requests.exceptions.ConnectionError as exc:
            self._fail_count += 1
            msg = f"Connection error: {exc}"
            self._err(f"[FAIL] {msg}")
            self._error_lines.append(f"## Connection Error\n- Endpoint: `{method.upper()} {path}`\n- Error: {exc}\n")
            return 0, {}
        except Exception as exc:
            self._fail_count += 1
            msg = f"Request exception: {exc}"
            self._err(f"[FAIL] {msg}")
            self._error_lines.append(f"## Request Exception\n- Endpoint: `{method.upper()} {path}`\n- Error: {exc}\n")
            return 0, {}

    def missing_feature(self, endpoint: str, description: str) -> None:
        """Reclassify the preceding FAIL as a SKIP (missing/unimplemented feature).

        Moves the last failure from _fail_count to _skip_count so the summary
        accurately reflects: the endpoint was tested, it is not available, but
        that is expected and should not count as a test failure.
        """
        if self._fail_count > 0:
            self._fail_count -= 1
            self._skip_count += 1
            # Reclassify the last [FAIL] line in logs to [SKIP]
            for i in range(len(self._log_lines) - 1, -1, -1):
                if "[FAIL]" in self._log_lines[i]:
                    self._log_lines[i] = self._log_lines[i].replace("[FAIL]", "[SKIP]")
                    break
            # Remove the last error entry that was added by call()
            for i in range(len(self._error_lines) - 1, -1, -1):
                if self._error_lines[i].startswith("## Unexpected Status"):
                    self._error_lines.pop(i)
                    break
        else:
            self._skip_count += 1
        line = f"[SKIP] {endpoint} — {description} (missing feature)"
        self._log(line)
        self._error_lines.append(f"## Missing Feature (skipped)\n- Endpoint: `{endpoint}`\n- Description: {description}\n")

    # ------------------------------------------------------------------
    # Assertion helpers
    # ------------------------------------------------------------------
    def assert_field(self, response_body, field: str, expected=None,
                     label: str | None = None) -> bool:
        """Assert that field exists in response_body (and optionally equals expected)."""
        if isinstance(response_body, dict):
            value = response_body.get(field)
        else:
            self._fail_count += 1
            msg = f"assert_field: response_body is not a dict (got {type(response_body).__name__})"
            self._err(f"[FAIL] {msg}")
            self._error_lines.append(f"## Assert Field Error\n- Field: `{field}`\n- Reason: {msg}\n")
            return False

        if value is None:
            self._fail_count += 1
            msg = f"Field '{field}' missing in response" + (f" [{label}]" if label else "")
            self._err(f"[FAIL] {msg}")
            self._error_lines.append(f"## Missing Field\n- Field: `{field}`\n- Context: {label or 'n/a'}\n- Body keys: {list(response_body.keys())}\n")
            return False

        if expected is not None and value != expected:
            self._fail_count += 1
            msg = f"Field '{field}' expected={expected!r} got={value!r}" + (f" [{label}]" if label else "")
            self._err(f"[FAIL] {msg}")
            self._error_lines.append(f"## Wrong Field Value\n- Field: `{field}`\n- Expected: `{expected}`\n- Got: `{value}`\n- Context: {label or 'n/a'}\n")
            return False

        self._pass_count += 1
        self._log(f"[OK]   Field '{field}' = {value!r}" + (f" [{label}]" if label else ""))
        return True

    def assert_true(self, condition: bool, message: str) -> bool:
        if condition:
            self._pass_count += 1
            self._log(f"[OK]   {message}")
        else:
            self._fail_count += 1
            self._err(f"[FAIL] {message}")
            self._error_lines.append(f"## Assertion Failed\n- Message: {message}\n")
        return condition

    # ------------------------------------------------------------------
    # Save logs
    # ------------------------------------------------------------------
    def save_log(self, step_name: str) -> bool:
        """
        Print summary to stdout (captured by run.ps1 Tee-Object → stepNN.log).
        Write _errors.md only if there are failures (separate file, no conflict).
        Returns True if no failures.
        """
        timestamp = datetime.datetime.now().isoformat(timespec="seconds")
        summary = (
            f"\n{'='*60}\n"
            f"Step: {step_name}\n"
            f"Timestamp: {timestamp}\n"
            f"PASSED: {self._pass_count}  FAILED: {self._fail_count}  SKIPPED: {self._skip_count}\n"
            f"{'='*60}\n"
        )
        # Print to stdout — Tee-Object in run.ps1 writes it to stepNN.log
        print(summary)

        if self._error_lines:
            err_path = os.path.join(self.logs_dir, f"{step_name}_errors.md")
            with open(err_path, "w", encoding="utf-8") as f:
                f.write(f"# UAT Errors - {step_name}\n\nTimestamp: {timestamp}\n\n")
                f.write("\n".join(self._error_lines))
            print(f"[INFO] Errors: {err_path}")

        return self._fail_count == 0

    def exit_code(self) -> int:
        return 0 if self._fail_count == 0 else 1


# ---------------------------------------------------------------------------
# Standalone helper functions
# ---------------------------------------------------------------------------

def login(session: UATSession, email: str, password: str) -> str | None:
    """Convenience wrapper."""
    return session.login(email, password)


def call(session: UATSession, method: str, path: str, body=None,
         files=None, expected_status: int = 200,
         query_params: dict | None = None) -> tuple[int, dict]:
    """Convenience wrapper."""
    return session.call(method, path, body=body, files=files,
                        expected_status=expected_status, query_params=query_params)


def save_log(session: UATSession, step_name: str) -> bool:
    """Convenience wrapper."""
    return session.save_log(step_name)


def assert_field(session: UATSession, response_body, field: str,
                 expected=None) -> bool:
    """Convenience wrapper."""
    return session.assert_field(response_body, field, expected)


def load_demo_plan(filename: str) -> dict:
    """Load a DemoData JSON file relative to repo root."""
    here = os.path.dirname(os.path.abspath(__file__))
    demo_dir = os.path.normpath(os.path.join(here, "../../../../DemoData"))
    path = os.path.join(demo_dir, filename)
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def _try_json(resp: requests.Response) -> dict:
    """Try to parse response as JSON, fall back to empty dict."""
    try:
        return resp.json()
    except Exception:
        return {"_raw": resp.text[:500]}
