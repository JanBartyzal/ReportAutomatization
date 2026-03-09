#!/bin/bash
# update-helm-charts.sh
# Updates Helm charts for all units with proper init containers and health probes

UNITS_DIR="microservices/units"

for values_file in $(find $UNITS_DIR -path "*/helm/values.yaml"); do
    unit_dir=$(dirname $(dirname $values_file))
    unit_name=$(basename $unit_dir)

    echo "Processing: $unit_name"

    # Check if initContainers section already exists
    if ! grep -q "initContainers:" "$values_file"; then
        case $unit_name in
            unit-vault-connector)
                # Tier 1 - no init containers
                echo "  Tier 1: no initContainers"
                cat >> "$values_file" << 'EOF'

# Tier 1: No dependencies
initContainers:
  waitForConfigDistributor:
    enabled: false
EOF
                ;;
            unit-config-distributor)
                # Tier 2 - waits for vault-connector
                echo "  Tier 2: wait for vault-connector"
                cat >> "$values_file" << 'EOF'

# Tier 2: Depends on vault-connector
initContainers:
  waitForVaultConnector:
    enabled: true
    targetService: unit-vault-connector
EOF
                ;;
            *)
                # Tier 3 - waits for config-distributor
                echo "  Tier 3: wait for config-distributor"
                cat >> "$values_file" << 'EOF'

# Tier 3: Depends on config-distributor
initContainers:
  waitForConfigDistributor:
    enabled: true
EOF
                ;;
        esac
    else
        echo "  initContainers already present, skipping"
    fi

    # Check if healthCheck section exists
    if ! grep -q "healthCheck:" "$values_file"; then
        echo "  Adding healthCheck configuration"
        cat >> "$values_file" << 'EOF'

# Health check endpoints
healthCheck:
  health:
    path: /health
    port: 8080
  ready:
    path: /ready
    port: 8080
  live:
    path: /live
    port: 8080
EOF
    fi

    # Check if probes section exists
    if ! grep -q "^probes:" "$values_file"; then
        echo "  Adding probes configuration"
        cat >> "$values_file" << 'EOF'

# Probe configuration
probes:
  startup:
    initialDelaySeconds: 5
    periodSeconds: 5
    failureThreshold: 30
  readiness:
    initialDelaySeconds: 5
    periodSeconds: 10
    failureThreshold: 3
  liveness:
    initialDelaySeconds: 30
    periodSeconds: 30
    failureThreshold: 3
EOF
    fi
done

echo "Done!"
