#!/usr/bin/env bash
# =============================================================================
# deploy.sh - Deploy services to Docker Compose environment
# =============================================================================
# Usage:
#   ./scripts/deploy.sh                    # Deploy all (build + up)
#   ./scripts/deploy.sh --up-only          # Start without rebuilding
#   ./scripts/deploy.sh --build-only       # Build only, don't start
#   ./scripts/deploy.sh --observability    # Include observability stack
#   ./scripts/deploy.sh --clean            # Clean deploy (remove volumes first)
#   ./scripts/deploy.sh engine-core        # Deploy specific service
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ---------------------------------------------------------------------------
# Colors
# ---------------------------------------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }
step()  { echo -e "${CYAN}[DEPLOY]${NC} $*"; }

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
COMPOSE_FILE="${PROJECT_ROOT}/infra/docker/docker-compose.consolidated.yml"
COMPOSE_OVERRIDE="${PROJECT_ROOT}/infra/docker/docker-compose.consolidated.override.yml"
COMPOSE_OBSERVABILITY="${PROJECT_ROOT}/infra/docker/docker-compose.observability.yml"
ENV_FILE="${PROJECT_ROOT}/infra/docker/.env"
ENV_EXAMPLE="${PROJECT_ROOT}/infra/docker/.env.example"

# Consolidated services (must match docker-compose.yml service names)
ALL_SERVICES=(
    "engine-core"
    "engine-ingestor"
    "engine-orchestrator"
    "engine-data"
    "engine-reporting"
    "engine-integrations"
    "processor-atomizers"
    "processor-generators"
    "frontend"
)

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
DO_BUILD=true
DO_UP=true
INCLUDE_OBSERVABILITY=false
CLEAN_DEPLOY=false
SPECIFIC_SERVICES=()
COMPOSE_ARGS=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --up-only)
            DO_BUILD=false
            shift
            ;;
        --build-only)
            DO_UP=false
            shift
            ;;
        --observability)
            INCLUDE_OBSERVABILITY=true
            shift
            ;;
        --clean)
            CLEAN_DEPLOY=true
            shift
            ;;
        -d|--detach)
            COMPOSE_ARGS="-d"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS] [SERVICE...]"
            echo ""
            echo "Options:"
            echo "  --up-only          Start services without rebuilding"
            echo "  --build-only       Build images only, don't start"
            echo "  --observability    Include observability stack (Grafana, Prometheus, etc.)"
            echo "  --clean            Remove volumes before deploying (data loss!)"
            echo "  -d, --detach       Run in detached mode"
            echo ""
            echo "Services: ${ALL_SERVICES[*]}"
            exit 0
            ;;
        *)
            SPECIFIC_SERVICES+=("$1")
            shift
            ;;
    esac
done

# ---------------------------------------------------------------------------
# Pre-flight
# ---------------------------------------------------------------------------
if ! docker info &> /dev/null 2>&1; then
    error "Docker is not running."
    exit 1
fi

if ! docker compose version &> /dev/null 2>&1; then
    error "Docker Compose v2 is not available."
    exit 1
fi

# Environment file
if [ ! -f "${ENV_FILE}" ]; then
    if [ -f "${ENV_EXAMPLE}" ]; then
        warn ".env not found. Copying from .env.example..."
        cp "${ENV_EXAMPLE}" "${ENV_FILE}"
        info "Created ${ENV_FILE}"
        warn "Review values in ${ENV_FILE} before proceeding."
    else
        warn "No .env file found. Services may fail to start."
    fi
fi

# Compose file check
if [ ! -f "${COMPOSE_FILE}" ]; then
    error "docker-compose.yml not found at ${COMPOSE_FILE}"
    exit 1
fi

# ---------------------------------------------------------------------------
# Build compose command
# ---------------------------------------------------------------------------
COMPOSE_CMD="docker compose -f ${COMPOSE_FILE}"

if [ -f "${COMPOSE_OVERRIDE}" ]; then
    COMPOSE_CMD="${COMPOSE_CMD} -f ${COMPOSE_OVERRIDE}"
fi

if [[ "$INCLUDE_OBSERVABILITY" == true ]] && [ -f "${COMPOSE_OBSERVABILITY}" ]; then
    COMPOSE_CMD="${COMPOSE_CMD} -f ${COMPOSE_OBSERVABILITY}"
    info "Observability stack included."
fi

COMPOSE_CMD="${COMPOSE_CMD} --env-file ${ENV_FILE}"

# ---------------------------------------------------------------------------
# Clean deploy
# ---------------------------------------------------------------------------
if [[ "$CLEAN_DEPLOY" == true ]]; then
    warn "Clean deploy requested. Removing existing containers and volumes..."
    ${COMPOSE_CMD} down -v --remove-orphans 2>/dev/null || true
    info "Cleanup complete."
fi

# ---------------------------------------------------------------------------
# Build phase
# ---------------------------------------------------------------------------
if [[ "$DO_BUILD" == true ]]; then
    step "Building Docker images..."
    if [[ ${#SPECIFIC_SERVICES[@]} -gt 0 ]]; then
        ${COMPOSE_CMD} build "${SPECIFIC_SERVICES[@]}"
    else
        ${COMPOSE_CMD} build
    fi
    info "Build complete."
fi

# ---------------------------------------------------------------------------
# Deploy phase
# ---------------------------------------------------------------------------
if [[ "$DO_UP" == true ]]; then
    step "Starting services..."

    if [[ ${#SPECIFIC_SERVICES[@]} -gt 0 ]]; then
        ${COMPOSE_CMD} up ${COMPOSE_ARGS} "${SPECIFIC_SERVICES[@]}"
    else
        ${COMPOSE_CMD} up ${COMPOSE_ARGS}
    fi

    if [[ "$COMPOSE_ARGS" == "-d" ]]; then
        echo ""
        info "Services started in detached mode."
        echo ""
        echo "  Service endpoints:"
        echo "    Frontend:            http://localhost:3000"
        echo "    API Gateway:         http://localhost:8080"
        echo "    engine-core:         http://localhost:8081"
        echo "    engine-ingestor:     http://localhost:8082"
        echo "    engine-orchestrator: http://localhost:8095"
        echo "    engine-data:         http://localhost:8100"
        echo "    engine-reporting:    http://localhost:8105"
        echo "    engine-integrations: http://localhost:8110"
        echo ""
        if [[ "$INCLUDE_OBSERVABILITY" == true ]]; then
            echo "  Observability:"
            echo "    Grafana:      http://localhost:3001"
            echo "    Prometheus:   http://localhost:9090"
            echo ""
        fi
        echo "  Logs:  docker compose -f infra/docker/docker-compose.yml logs -f [service]"
        echo "  Stop:  ./scripts/deploy.sh --stop  OR  ./scripts/dev-stop.sh"
    fi
fi
