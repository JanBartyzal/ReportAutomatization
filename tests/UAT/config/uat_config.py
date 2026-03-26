# UAT Configuration for ReportAutomatization (RA)
# Base URL and shared settings for all UAT test steps
#
# UAT tests bypass nginx and call services directly to avoid ForwardAuth
# recursive issues. Each service has its own port.

BASE_URL = "http://localhost:8081"  # engine-core (auth, admin, batch, versioning, audit)

LOGS_DIR = "../logs"
STATE_FILE = "../logs/uat_state.json"

USERS = {
    "admin1": {"email": "admin1@testorg1.ra", "password": "", "org_slug": "test-org-1", "role": "admin"},
    "user1":  {"email": "user1@testorg1.ra",  "password": "",  "org_slug": "test-org-1", "role": "editor"},
    "admin2": {"email": "admin2@testorg2.ra", "password": "", "org_slug": "test-org-2", "role": "admin"},
    "user2":  {"email": "user2@testorg2.ra",  "password": "",  "org_slug": "test-org-2", "role": "viewer"},
}

TIMEOUTS = {
    "default": 30,
    "upload": 60,
    "export": 60,
}

# ---------------------------------------------------------------------------
# Service URLs — direct access (bypass nginx)
# Based on docker-compose port mapping
# ---------------------------------------------------------------------------
SERVICES = {
    "engine_core":         "http://localhost:8081",   # auth, admin, batch, versioning, audit
    "engine_ingestor":     "http://localhost:8082",   # upload, scanner
    "engine_orchestrator": "http://localhost:8083",   # workflow, saga
    "engine_data":         "http://localhost:8100",   # sinks, query, dashboard, search, template
    "engine_reporting":    "http://localhost:8105",   # lifecycle, period, form, pptx-template, notification
    "engine_integrations": "http://localhost:8106",   # servicenow
    "processor_atomizers": "http://localhost:8088",   # pptx, xls, pdf, csv, ai, cleanup
    "processor_generators":"http://localhost:8111",   # pptx gen, xls gen, mcp
    "nginx":               "http://localhost",        # API gateway (port 80)
}

# ---------------------------------------------------------------------------
# API path prefixes per service
# Tests use: SERVICES["engine_core"] + API["auth"] + "/verify"
# ---------------------------------------------------------------------------
API = {
    # engine-core (8081)
    "auth":          "/api/auth",
    "admin":         "/api/admin",
    # engine-ingestor (8082)
    "upload":        "/api/upload",
    # engine-data (8100)
    "query":         "/api/query",
    "dashboards":    "/api/dashboards",
    "search":        "/api/search",
    "data_tables":   "/api/query/tables",
    "data_documents":"/api/query/documents",
    "templates_slide_metadata": "/api/query/templates/slide-metadata",
    # engine-reporting (8105)
    "reports":       "/api/reports",
    "periods":       "/api/periods",
    "notifications": "/api/notifications",
    "forms":         "/api/forms",
    "templates_pptx":"/api/templates/pptx",
    # engine-integrations (8106)
    "integrations":  "/api/admin/integrations",
    # nginx (80)
    "health":        "/health",
}
