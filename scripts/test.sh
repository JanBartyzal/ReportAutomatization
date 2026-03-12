#!/usr/bin/env bash
# =============================================================================
# test.sh - Run unit tests for all consolidated services
# =============================================================================
# Usage:
#   ./scripts/test.sh                    # Run all tests
#   ./scripts/test.sh --java             # Run Java tests only
#   ./scripts/test.sh --python           # Run Python tests only
#   ./scripts/test.sh --frontend         # Run frontend tests only
#   ./scripts/test.sh engine-core        # Run tests for specific service
#   ./scripts/test.sh --integration      # Run integration tests (requires Docker)
#   ./scripts/test.sh --e2e              # Run E2E tests (requires running stack)
#   ./scripts/test.sh --coverage         # Include coverage reports
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
step()  { echo -e "${CYAN}[TEST]${NC} $*"; }

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
JAVA_MODULES=(
    "engine-core:apps/engine/engine-core"
    "engine-ingestor:apps/engine/engine-ingestor"
    "engine-orchestrator:apps/engine/engine-orchestrator"
    "engine-data:apps/engine/engine-data"
    "engine-reporting:apps/engine/engine-reporting"
    "engine-integrations:apps/engine/engine-integrations"
)

PYTHON_MODULES=(
    "processor-atomizers:apps/processor/processor-atomizers"
    "processor-generators:apps/processor/processor-generators"
)

FRONTEND_DIR="apps/frontend"

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
RUN_JAVA=false
RUN_PYTHON=false
RUN_FRONTEND=false
RUN_INTEGRATION=false
RUN_E2E=false
WITH_COVERAGE=false
SPECIFIC_MODULE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --java)       RUN_JAVA=true; shift ;;
        --python)     RUN_PYTHON=true; shift ;;
        --frontend)   RUN_FRONTEND=true; shift ;;
        --integration) RUN_INTEGRATION=true; shift ;;
        --e2e)        RUN_E2E=true; shift ;;
        --coverage)   WITH_COVERAGE=true; shift ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS] [MODULE_NAME]"
            echo ""
            echo "Options:"
            echo "  --java          Run Java unit tests only"
            echo "  --python        Run Python unit tests only"
            echo "  --frontend      Run frontend tests only"
            echo "  --integration   Run integration tests (requires Docker services)"
            echo "  --e2e           Run E2E tests (requires full stack running)"
            echo "  --coverage      Generate coverage reports"
            echo ""
            echo "Modules: engine-core, engine-ingestor, engine-orchestrator,"
            echo "         engine-data, engine-reporting, engine-integrations,"
            echo "         processor-atomizers, processor-generators, frontend"
            exit 0
            ;;
        *)
            SPECIFIC_MODULE="$1"; shift ;;
    esac
done

# Default: run all
if [[ "$RUN_JAVA" == false && "$RUN_PYTHON" == false && "$RUN_FRONTEND" == false \
      && "$RUN_INTEGRATION" == false && "$RUN_E2E" == false && -z "$SPECIFIC_MODULE" ]]; then
    RUN_JAVA=true
    RUN_PYTHON=true
    RUN_FRONTEND=true
fi

# ---------------------------------------------------------------------------
# Results tracking
# ---------------------------------------------------------------------------
TOTAL=0
PASSED=0
FAILED_MODULES=()

run_result() {
    local name="$1" result="$2"
    TOTAL=$((TOTAL + 1))
    if [[ "$result" -eq 0 ]]; then
        PASSED=$((PASSED + 1))
        info "  ✓ ${name} — PASSED"
    else
        FAILED_MODULES+=("$name")
        error "  ✗ ${name} — FAILED"
    fi
}

# ---------------------------------------------------------------------------
# Java unit tests
# ---------------------------------------------------------------------------
run_java_tests() {
    local modules=("$@")
    step "Running Java unit tests..."

    for mod in "${modules[@]}"; do
        IFS=':' read -r name path <<< "$mod"
        local mod_path="${PROJECT_ROOT}/${path}"

        if [ ! -d "$mod_path" ]; then
            warn "  Skipping ${name}: directory not found (${path})"
            continue
        fi

        step "  Testing ${name}..."

        local gradle_cmd="./gradlew test"
        if [[ "$WITH_COVERAGE" == true ]]; then
            gradle_cmd="./gradlew test jacocoTestReport"
        fi

        if (cd "$mod_path" && ${gradle_cmd} --no-daemon 2>&1); then
            run_result "$name" 0
        else
            run_result "$name" 1
        fi
    done
}

# ---------------------------------------------------------------------------
# Python unit tests
# ---------------------------------------------------------------------------
run_python_tests() {
    local modules=("$@")
    step "Running Python unit tests..."

    for mod in "${modules[@]}"; do
        IFS=':' read -r name path <<< "$mod"
        local mod_path="${PROJECT_ROOT}/${path}"

        if [ ! -d "$mod_path" ]; then
            warn "  Skipping ${name}: directory not found (${path})"
            continue
        fi

        step "  Testing ${name}..."

        local pytest_cmd="python -m pytest tests/ -v --tb=short"
        if [[ "$WITH_COVERAGE" == true ]]; then
            pytest_cmd="python -m pytest tests/ -v --tb=short --cov=. --cov-report=html --cov-report=term"
        fi

        if (cd "$mod_path" && ${pytest_cmd} 2>&1); then
            run_result "$name" 0
        else
            run_result "$name" 1
        fi
    done
}

# ---------------------------------------------------------------------------
# Frontend tests
# ---------------------------------------------------------------------------
run_frontend_tests() {
    step "Running frontend tests..."
    local fe_path="${PROJECT_ROOT}/${FRONTEND_DIR}"

    if [ ! -d "$fe_path" ]; then
        warn "Frontend directory not found."
        return
    fi

    # Install dependencies if needed
    if [ ! -d "${fe_path}/node_modules" ]; then
        step "  Installing dependencies..."
        (cd "$fe_path" && npm ci --silent)
    fi

    # Lint
    step "  ESLint check..."
    if (cd "$fe_path" && npx eslint src/ --ext .ts,.tsx --quiet 2>&1); then
        run_result "frontend-lint" 0
    else
        run_result "frontend-lint" 1
    fi

    # TypeScript type check
    step "  TypeScript type check..."
    if (cd "$fe_path" && npx tsc --noEmit 2>&1); then
        run_result "frontend-typecheck" 0
    else
        run_result "frontend-typecheck" 1
    fi

    # Unit tests
    step "  Vitest unit tests..."
    local vitest_cmd="npx vitest run"
    if [[ "$WITH_COVERAGE" == true ]]; then
        vitest_cmd="npx vitest run --coverage"
    fi

    if (cd "$fe_path" && ${vitest_cmd} 2>&1); then
        run_result "frontend-unit" 0
    else
        run_result "frontend-unit" 1
    fi
}

# ---------------------------------------------------------------------------
# Integration tests
# ---------------------------------------------------------------------------
run_integration_tests() {
    step "Running integration tests..."
    local int_path="${PROJECT_ROOT}/tests/integration"

    if [ ! -d "$int_path" ]; then
        warn "Integration test directory not found."
        return
    fi

    # Check Docker is running (Testcontainers need it)
    if ! docker info &> /dev/null 2>&1; then
        error "Docker is required for integration tests but is not running."
        run_result "integration" 1
        return
    fi

    step "  Running Testcontainers-based integration tests..."
    if (cd "$int_path" && ./gradlew test --no-daemon 2>&1); then
        run_result "integration" 0
    else
        run_result "integration" 1
    fi
}

# ---------------------------------------------------------------------------
# E2E tests
# ---------------------------------------------------------------------------
run_e2e_tests() {
    step "Running E2E tests (Playwright)..."
    local e2e_path="${PROJECT_ROOT}/tests/e2e"

    if [ ! -d "$e2e_path" ]; then
        warn "E2E test directory not found."
        return
    fi

    # Check if stack is running
    if ! curl -sf http://localhost:8080/health > /dev/null 2>&1; then
        error "Full stack must be running for E2E tests."
        error "Run './scripts/deploy.sh -d' first."
        run_result "e2e" 1
        return
    fi

    step "  Running Playwright tests..."
    if (cd "$e2e_path" && npx playwright test 2>&1); then
        run_result "e2e" 0
    else
        run_result "e2e" 1
    fi
}

# ---------------------------------------------------------------------------
# Execute
# ---------------------------------------------------------------------------
echo ""
info "Test runner starting..."
echo ""

# Specific module
if [[ -n "$SPECIFIC_MODULE" ]]; then
    FOUND=false
    for mod in "${JAVA_MODULES[@]}"; do
        name="${mod%%:*}"
        if [[ "$name" == "$SPECIFIC_MODULE" ]]; then
            run_java_tests "$mod"
            FOUND=true
            break
        fi
    done
    for mod in "${PYTHON_MODULES[@]}"; do
        name="${mod%%:*}"
        if [[ "$name" == "$SPECIFIC_MODULE" ]]; then
            run_python_tests "$mod"
            FOUND=true
            break
        fi
    done
    if [[ "$SPECIFIC_MODULE" == "frontend" ]]; then
        run_frontend_tests
        FOUND=true
    fi
    if [[ "$FOUND" == false ]]; then
        error "Unknown module: $SPECIFIC_MODULE"
        exit 1
    fi
else
    # Category-based execution
    if [[ "$RUN_JAVA" == true ]]; then
        run_java_tests "${JAVA_MODULES[@]}"
    fi
    if [[ "$RUN_PYTHON" == true ]]; then
        run_python_tests "${PYTHON_MODULES[@]}"
    fi
    if [[ "$RUN_FRONTEND" == true ]]; then
        run_frontend_tests
    fi
    if [[ "$RUN_INTEGRATION" == true ]]; then
        run_integration_tests
    fi
    if [[ "$RUN_E2E" == true ]]; then
        run_e2e_tests
    fi
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "========================================="
echo "  Test Summary"
echo "========================================="
echo "  Total:  ${TOTAL}"
echo "  Passed: ${PASSED}"
echo "  Failed: ${#FAILED_MODULES[@]}"

if [[ ${#FAILED_MODULES[@]} -gt 0 ]]; then
    echo ""
    echo "  Failed modules:"
    for f in "${FAILED_MODULES[@]}"; do
        echo "    - $f"
    done
    echo "========================================="
    exit 1
else
    echo "========================================="
    info "All tests passed!"
    exit 0
fi
