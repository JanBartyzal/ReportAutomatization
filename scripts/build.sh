#!/usr/bin/env bash
# =============================================================================
# build.sh - Build Docker images for all consolidated services
# =============================================================================
# Usage:
#   ./scripts/build.sh              # Build all services
#   ./scripts/build.sh engine-core  # Build specific service
#   ./scripts/build.sh --java       # Build all Java services
#   ./scripts/build.sh --python     # Build all Python services
#   ./scripts/build.sh --frontend   # Build frontend only
#   ./scripts/build.sh --no-cache   # Build without Docker cache
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
step()  { echo -e "${CYAN}[BUILD]${NC} $*"; }

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
REGISTRY="${DOCKER_REGISTRY:-reportautomatization}"
TAG="${BUILD_TAG:-latest}"
DOCKER_ARGS=""

# Consolidated service definitions
# Format: name:context:dockerfile
JAVA_SERVICES=(
    "engine-core:apps/engine/engine-core:apps/engine/engine-core/Dockerfile"
    "engine-ingestor:apps/engine/engine-ingestor:apps/engine/engine-ingestor/Dockerfile"
    "engine-orchestrator:apps/engine/engine-orchestrator:apps/engine/engine-orchestrator/Dockerfile"
    "engine-data:apps/engine/engine-data:apps/engine/engine-data/Dockerfile"
    "engine-reporting:apps/engine/engine-reporting:apps/engine/engine-reporting/Dockerfile"
    "engine-integrations:apps/engine/engine-integrations:apps/engine/engine-integrations/Dockerfile"
)

PYTHON_SERVICES=(
    "processor-atomizers:apps/processor/processor-atomizers:apps/processor/processor-atomizers/Dockerfile"
    "processor-generators:apps/processor/processor-generators:apps/processor/processor-generators/Dockerfile"
)

FRONTEND_SERVICE="frontend:apps/frontend:apps/frontend/Dockerfile"

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
BUILD_TARGETS=()
SPECIFIC_SERVICE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --java)
            for svc in "${JAVA_SERVICES[@]}"; do
                BUILD_TARGETS+=("$svc")
            done
            shift
            ;;
        --python)
            for svc in "${PYTHON_SERVICES[@]}"; do
                BUILD_TARGETS+=("$svc")
            done
            shift
            ;;
        --frontend)
            BUILD_TARGETS+=("$FRONTEND_SERVICE")
            shift
            ;;
        --no-cache)
            DOCKER_ARGS="--no-cache"
            shift
            ;;
        --tag)
            TAG="$2"
            shift 2
            ;;
        --registry)
            REGISTRY="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS] [SERVICE_NAME]"
            echo ""
            echo "Options:"
            echo "  --java        Build all Java services"
            echo "  --python      Build all Python services"
            echo "  --frontend    Build frontend only"
            echo "  --no-cache    Build without Docker cache"
            echo "  --tag TAG     Docker image tag (default: latest)"
            echo "  --registry R  Docker registry prefix (default: reportautomatization)"
            echo ""
            echo "Services: engine-core, engine-ingestor, engine-orchestrator,"
            echo "          engine-data, engine-reporting, engine-integrations,"
            echo "          processor-atomizers, processor-generators, frontend"
            exit 0
            ;;
        *)
            SPECIFIC_SERVICE="$1"
            shift
            ;;
    esac
done

# If specific service requested, find it
if [[ -n "$SPECIFIC_SERVICE" ]]; then
    FOUND=false
    for svc in "${JAVA_SERVICES[@]}" "${PYTHON_SERVICES[@]}" "$FRONTEND_SERVICE"; do
        name="${svc%%:*}"
        if [[ "$name" == "$SPECIFIC_SERVICE" ]]; then
            BUILD_TARGETS+=("$svc")
            FOUND=true
            break
        fi
    done
    if [[ "$FOUND" == "false" ]]; then
        error "Unknown service: $SPECIFIC_SERVICE"
        exit 1
    fi
fi

# Default: build everything
if [[ ${#BUILD_TARGETS[@]} -eq 0 ]]; then
    for svc in "${JAVA_SERVICES[@]}"; do
        BUILD_TARGETS+=("$svc")
    done
    for svc in "${PYTHON_SERVICES[@]}"; do
        BUILD_TARGETS+=("$svc")
    done
    BUILD_TARGETS+=("$FRONTEND_SERVICE")
fi

# ---------------------------------------------------------------------------
# Pre-flight
# ---------------------------------------------------------------------------
if ! docker info &> /dev/null 2>&1; then
    error "Docker is not running."
    exit 1
fi

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------
TOTAL=${#BUILD_TARGETS[@]}
CURRENT=0
FAILED=()

info "Building ${TOTAL} service(s) with tag '${TAG}'..."
echo ""

for svc in "${BUILD_TARGETS[@]}"; do
    IFS=':' read -r name context dockerfile <<< "$svc"
    CURRENT=$((CURRENT + 1))
    IMAGE="${REGISTRY}/${name}:${TAG}"

    step "[${CURRENT}/${TOTAL}] Building ${name} → ${IMAGE}"

    if docker build \
        ${DOCKER_ARGS} \
        -t "${IMAGE}" \
        -f "${PROJECT_ROOT}/${dockerfile}" \
        "${PROJECT_ROOT}/${context}" 2>&1; then
        info "  ✓ ${name} built successfully"
    else
        error "  ✗ ${name} build FAILED"
        FAILED+=("$name")
    fi
    echo ""
done

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo "========================================="
if [[ ${#FAILED[@]} -eq 0 ]]; then
    info "All ${TOTAL} services built successfully!"
else
    error "${#FAILED[@]} service(s) failed:"
    for f in "${FAILED[@]}"; do
        echo "  - $f"
    done
    exit 1
fi
