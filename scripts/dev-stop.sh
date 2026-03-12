#!/usr/bin/env bash
# =============================================================================
# dev-stop.sh - Stop the Tilt local development environment
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

# ---------------------------------------------------------------------------
# Stop Tilt
# ---------------------------------------------------------------------------
cd "${PROJECT_ROOT}"

info "Stopping Tilt and all services..."

if command -v tilt &> /dev/null; then
    tilt down "$@"
    info "Tilt environment stopped."
else
    warn "Tilt not found. Falling back to docker compose down..."
    docker compose -f infra/docker/docker-compose.yml down "$@"
    info "Docker Compose services stopped."
fi

# ---------------------------------------------------------------------------
# Optional: Clean up volumes
# ---------------------------------------------------------------------------
if [[ "${1:-}" == "--clean" ]]; then
    warn "Removing Docker volumes (data will be lost)..."
    docker compose -f infra/docker/docker-compose.yml down -v
    info "Volumes removed."
fi

info "Done."
