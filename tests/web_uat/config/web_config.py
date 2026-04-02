# Web UI Test Configuration for ReportAutomatization (RA)
# Configuration for Selenium UI tests targeting the React frontend on port 5173
#
# These tests complement the backend UAT tests (tests/UAT/) by verifying
# the frontend UI behavior through Playwright-style Selenium automation.

import os

# ---------------------------------------------------------------------------
# Base URL — Vite dev server (project_charter.md: frontend on port 5173)
# ---------------------------------------------------------------------------
BASE_URL = os.environ.get("WEB_BASE_URL", "http://localhost:5173")

# ---------------------------------------------------------------------------
# Selenium configuration
# ---------------------------------------------------------------------------
SELENIUM_CONFIG = {
    "browser": os.environ.get("SELENIUM_BROWSER", "chrome"),
    "headless": os.environ.get("SELENIUM_HEADLESS", "false").lower() == "true",
    "implicit_wait": 0,       # 0 — explicit waits handle all waiting
    "page_load_timeout": 30,
    "script_timeout": 30,
    "window_width": 1920,
    "window_height": 1080,
}

# ---------------------------------------------------------------------------
# Test users — matches uat_config.py (tests/UAT/config/uat_config.py)
# Credentials are empty for dev bypass; real auth via API or MSAL
# ---------------------------------------------------------------------------
USERS = {
    "holding_admin": {
        "email": "admin1@testorg1.ra",
        "password": "",  # Dev bypass — no password needed
        "org_slug": "test-org-1",
        "role": "HOLDING_ADMIN",
        "description": "Holding Admin — can approve/reject reports, manage periods",
    },
    "editor": {
        "email": "user1@testorg1.ra",
        "password": "",
        "org_slug": "test-org-1",
        "role": "EDITOR",
        "description": "Editor — can upload files, fill forms, submit reports",
    },
    "viewer": {
        "email": "user2@testorg2.ra",
        "password": "",
        "org_slug": "test-org-2",
        "role": "VIEWER",
        "description": "Viewer — read-only access to dashboards and reports",
    },
    "admin2": {
        "email": "admin2@testorg2.ra",
        "password": "",
        "org_slug": "test-org-2",
        "role": "HOLDING_ADMIN",
        "description": "Holding Admin for org-2",
    },
}

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
LOGS_DIR = "../logs"
SCREENSHOTS_DIR = "../logs/screenshots"
EXPORTS_DIR = "../exports"
STATE_FILE = "../logs/web_uat_state.json"  # Shares auth state with backend UAT

# ---------------------------------------------------------------------------
# Timeouts (seconds)
# ---------------------------------------------------------------------------
TIMEOUTS = {
    "default": 10,
    "page_load": 30,
    "explicit_wait": 10,
    "upload": 60,
    "navigation": 15,
    "probe": 1,          # Short timeout for feature probing (missing_feature checks)
}

# ---------------------------------------------------------------------------
# Service URLs (for API calls made from UI tests if needed)
# ---------------------------------------------------------------------------
SERVICES = {
    "frontend":      BASE_URL,                          # http://localhost:5173
    "engine_core":    "http://localhost:8081",           # auth, admin
    "engine_ingestor":"http://localhost:8082",           # upload
    "engine_data":    "http://localhost:8100",           # query, dashboard
    "engine_reporting":"http://localhost:8105",         # lifecycle, period, form
}

# ---------------------------------------------------------------------------
# Frontend routes (matching actual React Router definitions in App.tsx)
# ---------------------------------------------------------------------------
ROUTES = {
    "login":              "/login",
    "dashboard":          "/dashboard",
    "upload":             "/upload",
    "files":              "/files",
    "file_detail":        "/files/:fileId",
    "forms":              "/forms",
    "form_new":           "/forms/new",
    "form_edit":          "/forms/:formId/edit",
    "form_fill":          "/forms/:formId/fill",
    "form_import":        "/forms/:formId/import",
    "form_assignments":   "/forms/:formId/assignments",
    "reports":            "/reports",
    "report_detail":      "/reports/:reportId",
    "periods":            "/periods",
    "period_detail":      "/periods/:periodId",
    "templates":          "/templates",
    "template_detail":    "/templates/:templateId",
    "dashboards":         "/dashboards",
    "dashboard_new":      "/dashboards/new",
    "dashboard_view":     "/dashboards/:dashboardId",
    "dashboard_edit":     "/dashboards/:dashboardId/edit",
    "matrix":             "/matrix",
    "admin_manage":       "/admin/manage",
    "admin_holding":      "/admin/holding",
    "admin_health":       "/admin/health",
    "admin_integrations": "/admin/integrations",
    "admin_promotions":   "/admin/promotions",
    "settings":           "/settings",
    "notification_settings": "/settings/notifications",
    "search":             "/search",
    "batch_generation":   "/batch-generation",
    "generated_reports":  "/generated-reports",
    "comparison":         "/comparison",
}
