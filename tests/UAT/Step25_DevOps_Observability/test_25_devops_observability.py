#!/usr/bin/env python3
# Step25: DevOps & Observability (FS99)
# Verify health endpoints, Prometheus metrics, OpenTelemetry, probes, info, and log management.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step25_DevOps_Observability"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})

    session._log(f"[INFO] Step25 — DevOps & Observability Verification")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    # ---------------------------------------------------------------
    # 1. Load tokens
    # ---------------------------------------------------------------
    admin1 = USERS["admin1"]
    admin1_token = tokens.get("admin1")
    if not admin1_token:
        admin1_token = session.login(admin1["email"], admin1["password"])
        if admin1_token:
            tokens["admin1"] = admin1_token
    else:
        session.token = admin1_token
        session._log("[INFO] Reusing admin1 token from state")

    session.assert_true(admin1_token is not None, "admin1 token available")
    if not admin1_token:
        session._err("[FATAL] Cannot proceed without admin1 token.")
        session.save_log(STEP_NAME)
        return 1

    session.token = admin1_token
    session.restore_auth_from_state()

    # Create sub-session for nginx (health endpoint)
    nginx_session = session.for_service(SERVICES["nginx"])

    # ---------------------------------------------------------------
    # 2. Health endpoints of all services
    # ---------------------------------------------------------------
    # Gateway / router health (via nginx)
    status, body = nginx_session.call("GET", "/health",
                                      expected_status=200,
                                      tag="health-gateway")

    # Engine-core health (uses /actuator/health, not /health)
    status, body = session.call("GET", "/actuator/health",
                                expected_status=200,
                                tag="health-engine-core")
    if status in (404, 500):
        session.missing_feature("GET /actuator/health (engine-core)", "Endpoint not implemented yet")

    # ---------------------------------------------------------------
    # 3. Prometheus metrics — GET /actuator/prometheus or /metrics
    # ---------------------------------------------------------------
    status, body = session.call("GET", "/actuator/prometheus",
                                expected_status=200,
                                tag="prometheus-metrics")
    if status not in (200,):
        # Try alternative /metrics endpoint
        status, body = session.call("GET", "/metrics",
                                    expected_status=200,
                                    tag="prometheus-metrics-alt")
        if status not in (200,):
            session.missing_feature("/actuator/prometheus",
                                    "Prometheus metrics endpoint")

    # ---------------------------------------------------------------
    # 4. OpenTelemetry check — GET /actuator/health, check for tracing info
    # ---------------------------------------------------------------
    status, body = session.call("GET", "/actuator/health",
                                expected_status=200,
                                tag="actuator-health-otel")
    if status == 200 and isinstance(body, dict):
        # Check for tracing/observability components
        components = body.get("components", {})
        has_tracing = any(
            key in components for key in ("tracing", "openTelemetry", "otel", "zipkin", "jaeger")
        )
        session._log(f"[INFO] Tracing info present in /actuator/health: {has_tracing}")

    # ---------------------------------------------------------------
    # 5. Readiness probe — GET /actuator/health/readiness
    # ---------------------------------------------------------------
    status, body = session.call("GET", "/actuator/health/readiness",
                                expected_status=200,
                                tag="readiness-probe")
    if status not in (200,):
        session.missing_feature("/actuator/health/readiness",
                                "Readiness probe endpoint")

    # ---------------------------------------------------------------
    # 6. Liveness probe — GET /actuator/health/liveness
    # ---------------------------------------------------------------
    status, body = session.call("GET", "/actuator/health/liveness",
                                expected_status=200,
                                tag="liveness-probe")
    if status not in (200,):
        session.missing_feature("/actuator/health/liveness",
                                "Liveness probe endpoint")

    # ---------------------------------------------------------------
    # 7. Info endpoint — GET /actuator/info, check version/build info
    # ---------------------------------------------------------------
    status, body = session.call("GET", "/actuator/info",
                                expected_status=200,
                                tag="actuator-info")
    if status == 200 and isinstance(body, dict):
        build_info = body.get("build", body.get("app", {}))
        version = build_info.get("version", build_info.get("name", "unknown"))
        session._log(f"[INFO] Application version/build: {version}")
        session.assert_true(len(body) > 0, "Info endpoint returns non-empty response")
    if status not in (200,):
        session.missing_feature("/actuator/info",
                                "Info endpoint with version/build information")

    # ---------------------------------------------------------------
    # 8. Log level management — GET /actuator/loggers
    # ---------------------------------------------------------------
    status, body = session.call("GET", "/actuator/loggers",
                                expected_status=200,
                                tag="log-level-management")
    if status == 200 and isinstance(body, dict):
        loggers = body.get("loggers", {})
        session._log(f"[INFO] Number of loggers available: {len(loggers)}")
    if status in (404, 500):
        session.missing_feature("GET /actuator/loggers", "Endpoint may not be exposed (404/500)")
    elif status not in (200,):
        session.missing_feature("/actuator/loggers",
                                "Log level management endpoint")

    # ---------------------------------------------------------------
    # Sync counters from sub-session
    # ---------------------------------------------------------------
    session.sync_counters_from(nginx_session)

    # ---------------------------------------------------------------
    # Save state
    # ---------------------------------------------------------------
    session.update_state({"tokens": tokens})

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
