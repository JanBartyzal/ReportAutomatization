#!/bin/bash

# CloudInfraMap - Deploy Single Unit (Mac Mini M4 - ARM64)
# Auto-detects unit type (.NET 10 / Python / Node.js), builds Docker image for ARM64
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
UNIT_NAME=""

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --unit-name|-u|-UnitName) UNIT_NAME="$2"; shift ;;
        --namespace|-n|-Namespace) NAMESPACE="$2"; shift ;;
        --registry|-r|-Registry) REGISTRY="$2"; shift ;;
        --tag|-t|-Tag) TAG="$2"; shift ;;
        --values-file|-v|-ValuesFile) VALUES_FILE="$2"; shift ;;
        --replicas|-Replicas) REPLICAS="$2"; shift ;;
        --local|-Local) LOCAL=true ;;
        --skip-build|-SkipBuild) SKIP_BUILD=true ;;
        --skip-deploy|-SkipDeploy) SKIP_DEPLOY=true ;;
        --dry-run|-DryRun) DRY_RUN=true ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

if [[ -z "$UNIT_NAME" ]]; then
    echo -e "\033[0;31mERROR: --unit-name is required.\033[0m"
    exit 1
fi

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UNITS_ROOT="$SCRIPT_ROOT/microservices/units"
PLATFORM="linux/arm64"

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
        return 0
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

    local DOCKER_ARGS=("docker" "buildx" "build" "--platform" "$PLATFORM" "-t" "$IMAGE_TAG" "-f" "$DOCKERFILE" "--no-cache" "${EXTRA_ARGS[@]}" "$BUILD_CONTEXT")

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
        write_status "  SKIP DEPLOY  $UNIT  (no helm chart found at $CHART_PATH)" "DarkGray"
        return 0
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
        else
            write_status "  WARN  No values-local.yaml found, using defaults" "Yellow"
        fi
    fi

    local HELM_ARGS=("helm" "upgrade" "--install" "$UNIT" "$CHART_PATH" \
        "--namespace" "$NAMESPACE" "--create-namespace" \
        "--set" "base-service.unitName=$UNIT" \
        "--set" "base-service.image.repository=$REGISTRY/$UNIT" \
        "--set" "base-service.image.tag=$TAG" \
        "--set" "base-service.replicaCount=$REPLICAS_COUNT" \
        "--set" "base-service.resources.requests.memory=64Mi" \
        "--set" "base-service.resources.limits.memory=256Mi" \
        "--set" "base-service.resources.requests.cpu=10m" \
        "--set" "base-service.resources.limits.cpu=500m" )

    if [ -n "$RESOLVED_VALUES" ] && [ -f "$RESOLVED_VALUES" ]; then
        HELM_ARGS+=("-f" "$RESOLVED_VALUES")
    fi

    invoke_cmd "${HELM_ARGS[@]}"
    local EXIT_CODE=$?

    if [ $EXIT_CODE -ne 0 ]; then
        write_status "  FAILED  $UNIT  (helm exit $EXIT_CODE)" "Red"
        return 1
    fi

    write_status "  OK  $UNIT  (deployed to namespace: $NAMESPACE, replicas: $REPLICAS_COUNT)" "Green"
    return 0
}

# ============================================================================
# MAIN
# ============================================================================

write_status "======================================" "Cyan"
write_status "  CloudInfraMap - Deploy Single Unit" "Cyan"
write_status "  Target: Mac Mini M4 (ARM64)" "Cyan"
write_status "======================================" "Cyan"
write_status "Unit:       $UNIT_NAME" "White"
write_status "Namespace:  $NAMESPACE" "White"
write_status "Registry:   $REGISTRY" "White"
write_status "Tag:        $TAG" "White"
write_status "Platform:   $PLATFORM" "White"
write_status "Replicas:   $REPLICAS" "White"
write_status "SkipBuild:  $SKIP_BUILD" "White"
write_status "SkipDeploy: $SKIP_DEPLOY" "White"
write_status "DryRun:     $DRY_RUN" "White"
echo ""

UNIT_PATH="$UNITS_ROOT/$UNIT_NAME"
if [ ! -d "$UNIT_PATH" ]; then
    write_status "ERROR: Unit directory not found: $UNIT_PATH" "Red"
    echo ""
    write_status "Available units:" "Yellow"
    for d in "$UNITS_ROOT"/unit-*; do
        [ -d "$d" ] && write_status "  - $(basename "$d")" "Gray"
    done
    exit 1
fi

UNIT_TYPE=$(get_unit_type "$UNIT_PATH")
write_status "  Detected type: $UNIT_TYPE" "DarkGray"
echo ""

BUILD_OK=true
if [ "$SKIP_BUILD" = false ]; then
    invoke_docker_build "$UNIT_NAME" "$UNIT_PATH" "$UNIT_TYPE"
    if [ $? -ne 0 ]; then BUILD_OK=false; fi
else
    write_status "  SKIP BUILD (--skip-build)" "DarkGray"
fi

DEPLOY_OK=true
if [ "$SKIP_DEPLOY" = false ] && [ "$BUILD_OK" = true ]; then
    invoke_helm_deploy "$UNIT_NAME" "$UNIT_PATH" "$REPLICAS"
    if [ $? -ne 0 ]; then DEPLOY_OK=false; fi
elif [ "$SKIP_DEPLOY" = true ]; then
    write_status "  SKIP DEPLOY (--skip-deploy)" "DarkGray"
elif [ "$BUILD_OK" = false ]; then
    write_status "  SKIP DEPLOY (build failed)" "Red"
fi

echo ""
write_status "======================================" "Cyan"
if [ "$BUILD_OK" = true ] && [ "$DEPLOY_OK" = true ]; then
    write_status "  RESULT: SUCCESS" "Green"
    write_status "  Unit '$UNIT_NAME' [$UNIT_TYPE] deployed to '$NAMESPACE' (ARM64)" "Green"
    EXIT_FINAL=0
else
    write_status "  RESULT: FAILED" "Red"
    [ "$BUILD_OK" = false ] && write_status "  Build failed" "Red"
    [ "$DEPLOY_OK" = false ] && write_status "  Deploy failed" "Red"
    EXIT_FINAL=1
fi
write_status "======================================" "Cyan"

exit $EXIT_FINAL
