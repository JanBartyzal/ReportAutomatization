#!/bin/bash

# CloudInfraMap - Deploy All Units (Mac Mini M4 - ARM64)
# Auto-detects unit type (.NET 10 / Python / Node.js), builds Docker images for ARM64
# architecture and deploys via Helm. Skips units without a Dockerfile.

# Default values
NAMESPACE="cim"
REGISTRY="cim"
TAG="latest"
VALUES_FILE=""
REPLICAS=1
LOCAL=false
SKIP_BUILD=false
SKIP_DEPLOY=false
DRY_RUN=false
SKIP_UNITS=""

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --namespace|-n|-Namespace) NAMESPACE="$2"; shift ;;
        --registry|-r|-Registry) REGISTRY="$2"; shift ;;
        --tag|-t|-Tag) TAG="$2"; shift ;;
        --values-file|-v|-ValuesFile) VALUES_FILE="$2"; shift ;;
        --replicas|-Replicas) REPLICAS="$2"; shift ;;
        --skip-units|-SkipUnits) SKIP_UNITS="$2"; shift ;;
        --local|-Local) LOCAL=true ;;
        --skip-build|-SkipBuild) SKIP_BUILD=true ;;
        --skip-deploy|-SkipDeploy) SKIP_DEPLOY=true ;;
        --dry-run|-DryRun) DRY_RUN=true ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UNITS_ROOT="$SCRIPT_ROOT/microservices/units"
PLATFORM="linux/arm64"

# Parse skip list
IFS=',' read -ra SKIP_ARRAY <<< "$SKIP_UNITS"
# Trim whitespace using string substitution loop
for i in "${!SKIP_ARRAY[@]}"; do
    tmp="${SKIP_ARRAY[$i]}"
    # Remove leading
    tmp="${tmp#"${tmp%%[![:space:]]*}"}"
    # Remove trailing
    tmp="${tmp%"${tmp##*[![:space:]]}"}"
    SKIP_ARRAY[$i]="$tmp"
done

in_skip_list() {
    local e
    for e in "${SKIP_ARRAY[@]}"; do
        if [[ "$e" == "$1" ]]; then return 0; fi
    done
    return 1
}

# ============================================================================
# HELPERS
# ============================================================================

write_status() {
    local MSG=$1
    local COLOR=$2
    case $COLOR in
        "White") echo -e "\033[1;37m$MSG\033[0m" ;;
        "DarkGray") echo -e "\033[1;30m$MSG\033[0m" ;;
        "Gray") echo -e "\033[0;37m$MSG\033[0m" ;;
        "Red") echo -e "\033[0;31m$MSG\033[0m" ;;
        "DarkRed") echo -e "\033[0;31m$MSG\033[0m" ;;
        "Green") echo -e "\033[0;32m$MSG\033[0m" ;;
        "Yellow") echo -e "\033[0;33m$MSG\033[0m" ;;
        "Cyan") echo -e "\033[0;36m$MSG\033[0m" ;;
        "DarkCyan") echo -e "\033[0;36m$MSG\033[0m" ;;
        *) echo "$MSG" ;;
    esac
}

invoke_cmd() {
    if [ "$DRY_RUN" = true ]; then
        write_status "  [DRY-RUN] $*" "DarkGray"
        return 0
    fi
    "$@"
    return $?
}

# ============================================================================
# UNIT TYPE DETECTION
# ============================================================================

get_unit_type() {
    local UNIT_PATH=$1
    local SRC_PATH="$UNIT_PATH/src"

    if [ -f "$SRC_PATH/main.py" ] || find "$UNIT_PATH" -name "main.py" -print -quit | grep -q .; then
        echo "Python"
        return
    fi

    if [ -f "$SRC_PATH/package.json" ] || find "$UNIT_PATH" -maxdepth 4 -name "package.json" \! -path "*/node_modules/*" -print -quit | grep -q .; then
        echo "NodeJS"
        return
    fi

    if find "$UNIT_PATH" -name "*.csproj" \! -name "*Test*.csproj" -print -quit | grep -q .; then
        echo "DotNet"
        return
    fi

    echo "Unknown"
}

# ============================================================================
# BUILD (ARM64 for Mac Mini M4)
# ============================================================================

invoke_docker_build() {
    local UNIT=$1
    local PATH_=$2
    local TYPE=$3

    local DOCKERFILE="$PATH_/Dockerfile"
    if [ ! -f "$DOCKERFILE" ]; then
        write_status "  SKIP BUILD  $UNIT -- no Dockerfile found (library/support unit)" "DarkGray"
        return 2 # 2 means skipped
    fi

    local BUILD_CONTEXT="$SCRIPT_ROOT"
    local IMAGE_TAG="$REGISTRY/$UNIT:$TAG"

    write_status "  BUILD [$TYPE] [ARM64]  $UNIT  ->  $IMAGE_TAG" "Cyan"

    local EXTRA_ARGS=()
    case $TYPE in
        "Python") EXTRA_ARGS=("--build-arg" "UNIT_TYPE=python") ;;
        "NodeJS") EXTRA_ARGS=("--build-arg" "UNIT_TYPE=nodejs") ;;
        "DotNet") EXTRA_ARGS=("--build-arg" "UNIT_TYPE=dotnet") ;;
    esac

    local DOCKER_ARGS=("docker" "buildx" "build" "--platform" "$PLATFORM" "-t" "$IMAGE_TAG" "-f" "$DOCKERFILE" "${EXTRA_ARGS[@]}" "$BUILD_CONTEXT")

    invoke_cmd "${DOCKER_ARGS[@]}"
    local EXIT_CODE=$?

    if [ $EXIT_CODE -ne 0 ]; then
        write_status "  FAILED  $UNIT  (docker build exit $EXIT_CODE)" "Red"
        return 1
    fi

    write_status "  OK  $UNIT  (image: $IMAGE_TAG, platform: $PLATFORM)" "Green"
    return 0
}

# ============================================================================
# DEPLOY
# ============================================================================

invoke_helm_deploy() {
    local UNIT=$1
    local PATH_=$2
    local REPLICAS_COUNT=$3

    write_status "  HELM  $UNIT" "White"

    local CHART_PATH="$PATH_/helm"
    if [ ! -d "$CHART_PATH" ]; then
        write_status "  SKIP DEPLOY  $UNIT  (no helm chart found)" "DarkGray"
        return 2 # 2 means skipped
    fi

    write_status "  Updating Helm dependencies..." "Gray"
    invoke_cmd helm dependency update "$CHART_PATH"
    local DEP_EXIT=$?

    if [ $DEP_EXIT -ne 0 ]; then
        write_status "  FAILED  $UNIT  (helm dependency update exit $DEP_EXIT)" "Red"
        return 1
    fi

    local RESOLVED_VALUES="$VALUES_FILE"
    if [ "$LOCAL" = true ] && [ -z "$RESOLVED_VALUES" ]; then
        local LOCAL_VALUES="$CHART_PATH/values-local.yaml"
        if [ -f "$LOCAL_VALUES" ]; then
            RESOLVED_VALUES="$LOCAL_VALUES"
            write_status "  Using local values: $LOCAL_VALUES" "DarkCyan"
        fi
    fi

    local HELM_ARGS=("helm" "upgrade" "--install" "$UNIT" "$CHART_PATH" \
        "--namespace" "$NAMESPACE" "--create-namespace" \
        "--set" "base-service.unitName=$UNIT" \
        "--set" "base-service.image.repository=$REGISTRY/$UNIT" \
        "--set" "base-service.image.tag=$TAG" \
        "--set" "base-service.replicaCount=$REPLICAS_COUNT" \
        "--set" "base-service.region=default" \
        "--set" "base-service.domain=common")

    if [ -n "$RESOLVED_VALUES" ] && [ -f "$RESOLVED_VALUES" ]; then
        HELM_ARGS+=("-f" "$RESOLVED_VALUES")
    fi

    invoke_cmd "${HELM_ARGS[@]}"
    local EXIT_CODE=$?

    if [ $EXIT_CODE -ne 0 ]; then
        write_status "  FAILED  $UNIT  (helm exit $EXIT_CODE)" "Red"
        return 1
    fi

    write_status "  OK  $UNIT  (deployed, replicas: $REPLICAS_COUNT)" "Green"
    return 0
}

# ============================================================================
# MAIN
# ============================================================================

write_status "======================================" "Cyan"
write_status "  CloudInfraMap - Deploy All Units"    "Cyan"
write_status "  Target: Mac Mini M4 (ARM64)"         "Cyan"
write_status "======================================" "Cyan"
write_status "Namespace:  $NAMESPACE" "White"
write_status "Registry:   $REGISTRY" "White"
write_status "Tag:        $TAG" "White"
write_status "Platform:   $PLATFORM" "White"
write_status "Replicas:   $REPLICAS" "White"
write_status "Local:      $LOCAL" "White"
if [ -n "$SKIP_UNITS" ]; then
    write_status "Skip:       $SKIP_UNITS" "White"
else
    write_status "Skip:       (none)" "White"
fi
write_status "DryRun:     $DRY_RUN" "White"
echo ""

if [ ! -d "$UNITS_ROOT" ]; then
    write_status "ERROR: No unit directories found in $UNITS_ROOT" "Red"
    exit 1
fi

DOTNET=0
PYTHON=0
NODEJS=0
UNKNOWN=0
SKIPPED_COUNT=0
BUILD_FAILED=0
DEPLOY_FAILED=0
BUILD_OK=0
DEPLOY_OK=0

BUILD_FAILURES=()
BUILD_FAILURES_TYPES=()
BUILD_FAILURES_ERRORS=()

DEPLOY_FAILURES=()
DEPLOY_FAILURES_TYPES=()
DEPLOY_FAILURES_ERRORS=()

# Note: Using subshell and simple looping to find directories starting with unit-
for d in "$UNITS_ROOT"/unit-*; do
    [ ! -d "$d" ] && continue
    UNIT_NAME=$(basename "$d")
    UNIT_PATH="$d"

    write_status "----------------------------------------------------" "Yellow"
    write_status "Unit: $UNIT_NAME" "Yellow"

    if in_skip_list "$UNIT_NAME"; then
        write_status "  SKIP  $UNIT_NAME (excluded)" "DarkGray"
        ((SKIPPED_COUNT++))
        continue
    fi

    UNIT_TYPE=$(get_unit_type "$UNIT_PATH")
    write_status "  Type: $UNIT_TYPE" "DarkGray"
    
    case $UNIT_TYPE in
        "DotNet") ((DOTNET++)) ;;
        "Python") ((PYTHON++)) ;;
        "NodeJS") ((NODEJS++)) ;;
        *) ((UNKNOWN++)) ;;
    esac

    IS_BUILD_OK=true
    if [ "$SKIP_BUILD" = false ]; then
        invoke_docker_build "$UNIT_NAME" "$UNIT_PATH" "$UNIT_TYPE"
        BRES=$?
        if [ $BRES -eq 1 ]; then
            IS_BUILD_OK=false
            ((BUILD_FAILED++))
            BUILD_FAILURES+=("$UNIT_NAME")
            BUILD_FAILURES_TYPES+=("$UNIT_TYPE")
            BUILD_FAILURES_ERRORS+=("Docker build failed")
        elif [ $BRES -eq 0 ]; then
            ((BUILD_OK++))
        fi
    fi

    if [ "$SKIP_DEPLOY" = false ] && [ "$IS_BUILD_OK" = true ]; then
        invoke_helm_deploy "$UNIT_NAME" "$UNIT_PATH" "$REPLICAS"
        DRES=$?
        if [ $DRES -eq 1 ]; then
            ((DEPLOY_FAILED++))
            DEPLOY_FAILURES+=("$UNIT_NAME")
            DEPLOY_FAILURES_TYPES+=("$UNIT_TYPE")
            DEPLOY_FAILURES_ERRORS+=("Helm deploy failed")
        elif [ $DRES -eq 0 ]; then
            ((DEPLOY_OK++))
        fi
    elif [ "$IS_BUILD_OK" = false ]; then
        ((DEPLOY_FAILED++))
        DEPLOY_FAILURES+=("$UNIT_NAME")
        DEPLOY_FAILURES_TYPES+=("$UNIT_TYPE")
        DEPLOY_FAILURES_ERRORS+=("Skipped - build failed")
    fi
done

# ============================================================================
# SUMMARY
# ============================================================================

write_status ""
write_status "======================================" "Cyan"
write_status "  Deployment Summary (ARM64)"           "Cyan"
write_status "======================================" "Cyan"
write_status "  .NET units:    $DOTNET" "White"
write_status "  Python units:  $PYTHON" "White"
write_status "  Node.js units: $NODEJS" "White"
write_status "  Unknown:       $UNKNOWN" "White"
write_status "  Skipped:       $SKIPPED_COUNT" "White"
write_status ""
write_status "  Builds OK:     $BUILD_OK" "Green"
write_status "  Deploys OK:    $DEPLOY_OK" "Green"

TOTAL_FAILED=$((BUILD_FAILED + DEPLOY_FAILED))
if [ $TOTAL_FAILED -gt 0 ]; then
    write_status ""
    write_status "  Build failures:  $BUILD_FAILED" "Red"
    write_status "  Deploy failures: $DEPLOY_FAILED" "Red"
else
    write_status "  Failures:      0" "Green"
fi

# ============================================================================
# FAILURE DETAILS
# ============================================================================

if [ ${#BUILD_FAILURES[@]} -gt 0 ]; then
    write_status ""
    write_status "======================================" "Red"
    write_status "  BUILD FAILURES (${#BUILD_FAILURES[@]})" "Red"
    write_status "======================================" "Red"
    for i in "${!BUILD_FAILURES[@]}"; do
        write_status "  [${BUILD_FAILURES_TYPES[$i]}]  ${BUILD_FAILURES[$i]}" "Red"
        write_status "         ${BUILD_FAILURES_ERRORS[$i]}" "DarkRed"
    done
fi

if [ ${#DEPLOY_FAILURES[@]} -gt 0 ]; then
    write_status ""
    write_status "======================================" "Red"
    write_status "  DEPLOY FAILURES (${#DEPLOY_FAILURES[@]})" "Red"
    write_status "======================================" "Red"
    for i in "${!DEPLOY_FAILURES[@]}"; do
        write_status "  [${DEPLOY_FAILURES_TYPES[$i]}]  ${DEPLOY_FAILURES[$i]}" "Red"
        write_status "         ${DEPLOY_FAILURES_ERRORS[$i]}" "DarkRed"
    done
fi

write_status ""
write_status "======================================" "Cyan"

if [ $TOTAL_FAILED -gt 0 ]; then
    exit 1
else
    exit 0
fi
