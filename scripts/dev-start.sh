#!/usr/bin/env bash
# =============================================================================
# dev-start.sh - Start the Tilt local development environment
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
NC='\033[0m' # No Color

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ---------------------------------------------------------------------------
# Pre-flight Checks
# ---------------------------------------------------------------------------
info "Running pre-flight checks..."

# Check tilt is installed
if ! command -v tilt &> /dev/null; then
    error "Tilt is not installed."
    echo "  Install it from: https://docs.tilt.dev/install.html"
    echo "  macOS:   brew install tilt-dev/tap/tilt"
    echo "  Windows: scoop install tilt"
    echo "  Linux:   curl -fsSL https://raw.githubusercontent.com/tilt-dev/tilt/master/scripts/install.sh | bash"
    exit 1
fi
info "Tilt found: $(tilt version)"

# Check docker is running
if ! docker info &> /dev/null 2>&1; then
    error "Docker is not running. Please start Docker Desktop first."
    exit 1
fi
info "Docker is running."

# Check docker compose is available
if ! docker compose version &> /dev/null 2>&1; then
    error "Docker Compose (v2) is not available."
    echo "  Make sure you have Docker Desktop >= 4.x or install docker-compose-plugin."
    exit 1
fi
info "Docker Compose found: $(docker compose version --short)"

# ---------------------------------------------------------------------------
# Environment File
# ---------------------------------------------------------------------------
ENV_FILE="${PROJECT_ROOT}/infra/docker/.env"
ENV_EXAMPLE="${PROJECT_ROOT}/infra/docker/.env.example"

if [ ! -f "${ENV_FILE}" ]; then
    if [ -f "${ENV_EXAMPLE}" ]; then
        warn ".env file not found. Copying from .env.example..."
        cp "${ENV_EXAMPLE}" "${ENV_FILE}"
        info "Created ${ENV_FILE} from .env.example"
        warn "Review and update the values in ${ENV_FILE} before proceeding."
    else
        warn "No .env or .env.example found at infra/docker/"
        warn "Services may fail to start without proper environment variables."
    fi
else
    info "Environment file found: ${ENV_FILE}"
fi

# ---------------------------------------------------------------------------
# Start Tilt
# ---------------------------------------------------------------------------
cd "${PROJECT_ROOT}"

# Pass through any extra arguments (e.g., --enable-observability)
EXTRA_ARGS=""
if [[ "${1:-}" == "--observability" ]] || [[ "${1:-}" == "--enable-observability" ]]; then
    EXTRA_ARGS="-- --enable-observability"
    shift || true
fi

info "Starting Tilt..."
echo ""
echo "  Tilt UI will be available at: http://localhost:10350"
echo "  Press Ctrl+C to stop watching (services keep running)"
echo "  Run 'scripts/dev-stop.sh' to tear down everything"
echo ""

# shellcheck disable=SC2086
exec tilt up ${EXTRA_ARGS} "$@"
