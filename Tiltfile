# =============================================================================
# ReportAutomatization - Tiltfile (Main Orchestration)
# =============================================================================
# Tilt local development environment for microservices project.
# Uses Docker Compose as the container orchestration backend.
#
# Usage:
#   tilt up                              # Start all services
#   tilt up -- --enable-observability     # Start with observability stack
#   tilt down                            # Stop all services
# =============================================================================

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
config.define_bool("enable-observability", args=True, usage="Enable the observability stack (Grafana, Prometheus, Tempo, Loki)")

cfg = config.parse()
enable_observability = cfg.get("enable-observability", False)

# ---------------------------------------------------------------------------
# Docker Compose - Core Stack
# ---------------------------------------------------------------------------
docker_compose(
    configPaths=["./infra/docker/docker-compose.yml"],
    env_file="./infra/docker/.env",
    project_name="reportplatform",
)

# Optionally load observability overlay
if enable_observability:
    docker_compose(
        configPaths=["./infra/docker/docker-compose.observability.yml"],
        env_file="./infra/docker/.env",
        project_name="reportplatform",
    )

# ---------------------------------------------------------------------------
# Load Sub-Tiltfiles
# ---------------------------------------------------------------------------
load_dynamic("./tilt/Tiltfile.infra")
load_dynamic("./tilt/Tiltfile.java")
load_dynamic("./tilt/Tiltfile.python")
load_dynamic("./tilt/Tiltfile.frontend")

# ---------------------------------------------------------------------------
# Observability Group (conditional)
# ---------------------------------------------------------------------------
if enable_observability:
    OBSERVABILITY_RESOURCES = [
        "otel-collector",
        "tempo",
        "loki",
        "prometheus",
        "grafana",
        "promtail",
    ]

    for res in OBSERVABILITY_RESOURCES:
        dc_resource(res, labels=["observability"])

    # Grafana depends on its data sources
    dc_resource("grafana", resource_deps=["prometheus", "tempo", "loki"])
    dc_resource("promtail", resource_deps=["loki"])

# ---------------------------------------------------------------------------
# Global Update Settings
# ---------------------------------------------------------------------------
update_settings(
    max_parallel_updates=4,
    k8s_upsert_timeout_secs=120,
    suppress_unused_image_warnings=None,
)

# ---------------------------------------------------------------------------
# Custom Buttons
# ---------------------------------------------------------------------------
local_resource(
    "db-reset",
    cmd="docker exec postgres psql -U postgres -c 'SELECT 1'",
    labels=["utilities"],
    auto_init=False,
    trigger_mode=TRIGGER_MODE_MANUAL,
)

local_resource(
    "logs-all",
    cmd="docker compose -f infra/docker/docker-compose.yml logs --tail=50",
    labels=["utilities"],
    auto_init=False,
    trigger_mode=TRIGGER_MODE_MANUAL,
)
