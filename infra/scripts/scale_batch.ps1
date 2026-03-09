
param(
 
    [string]$Group = "",
    [switch]$Stop = $false,
    [switch]$StopAll = $false
)
 


$batchUnits = @("unit-vault-connector", "unit-config-distributor", "unit-audit-logger", 
    "unit-web-gw-core", "unit-web-gw-cache", "unit-web-gw-graphql", "unit-web-gw-cqrs",
    "unit-auth-service", "unit-rbac-enforcer", "unit-token-issuer", 
    "unit-oidc-saml-connector", "unit-user-registry", "unit-organization-manager",
    "unit-api-key-manager", "unit-secret-discovery-engine", "unit-secret-health-monitor"
)


# Batch 1: 1_Admin_Config_Identity
$batchUnits1 = @("unit-branding-manager", "unit-config-distributor", "unit-context-switcher", "unit-environment-profile-manager", "unit-guided-tour-engine", "unit-local-account-manager", "unit-mfa-manager", "unit-oidc-saml-connector", "unit-pdf-branding-applier", "unit-power-profile-library", "unit-sandbox-cleanup-runner", "unit-sandbox-manager", "unit-scim-provisioner", "unit-social-login-handler")
$batchUnits2 = @("unit-asset-depreciation-runner", "unit-asset-registry", "unit-aws-pricing-fetcher", "unit-bicep-parser", "unit-blueprint-applier", "unit-blueprint-registry", "unit-bulk-import-orchestrator", "unit-canvas-diff-engine", "unit-canvas-manager", "unit-capacity-planner", "unit-cloudformation-parser", "unit-component-library", "unit-composite-sku-registry", "unit-currency-converter", "unit-custom-pricebook-handler", "unit-datacenter-manager", "unit-default-scenario-library", "unit-egress-calculator", "unit-egress-cost-calculator", "unit-geoarbitrage-calculator", "unit-hierarchy-visualizer", "unit-labor-cost-tracker", "unit-license-alert-runner", "unit-license-manager", "unit-multicloud-terraform-generator", "unit-multiplan-aggregator", "unit-network-topology-analyzer", "unit-network-topology-builder", "unit-onprem-tco-reporter", "unit-pattern-designer", "unit-pattern-instantiator", "unit-pattern-library-manager", "unit-plan-as-component", "unit-plan-component-search", "unit-plan-vs-reality-analyzer", "unit-power-cost-calculator", "unit-power-efficiency-engine", "unit-price-change-detector", "unit-price-sync-runner", "unit-pricing-cache-manager", "unit-remediation-planner", "unit-scenario-comparator", "unit-scenario-manager", "unit-scenario-template-library", "unit-server-inventory", "unit-sku-parameter-search", "unit-sku-taxonomy-manager", "unit-sku-validator", "unit-software-license-tracker")
$batchUnits3 = @("unit-storage-cost-calculator", "unit-storage-price-calculator", "unit-storage-tier-advisor", "unit-tco-calculator", "unit-terraform-parser", "unit-vnet-peering-estimator")
$batchUnits4 = @("unit-ai-layout-generator", "unit-anomaly-detector", "unit-anomaly-predictor", "unit-anomaly-resolution-dashboard", "unit-architecture-analyzer", "unit-audit-chain-verifier", "unit-autonomous-action-executor", "unit-autonomy-dashboard", "unit-aws-cloudwatch-connector", "unit-azure-monitor-connector", "unit-azure-retail-api-adapter", "unit-burnrate-calculator", "unit-cde-scope-analyzer", "unit-control-remediation-tracker", "unit-conversational-optimizer", "unit-cost-forecaster", "unit-data-gravity-analyzer", "unit-email-alert-generator", "unit-executive-dashboard", "unit-gap-analyzer", "unit-guardrail-enforcer", "unit-intent-classifier", "unit-migration-path-analyzer", "unit-ml-model-trainer", "unit-nis2-dashboard-generator", "unit-nl-provisioner", "unit-nlq-processor", "unit-nlq-query-processor", "unit-overprovisioning-detector", "unit-portfolio-aggregator", "unit-precision-tracker", "unit-prometheus-connector", "unit-rightsizing-analyzer", "unit-rightsizing-executor", "unit-rightsizing-history-tracker", "unit-rightsizing-recommender", "unit-root-cause-analyzer", "unit-savings-analyzer", "unit-savings-attribution-tracker", "unit-search-index-builder", "unit-soc2-dashboard", "unit-spot-ri-recommender", "unit-telemetry-collector", "unit-utilization-correlator", "unit-waste-identifier", "unit-web-frontend", "unit-web-landing-page")
$batchUnits5 = @("unit-anonymization-engine", "unit-aoc-generator", "unit-approval-policy-manager", "unit-approval-workflow-engine", "unit-baa-manager", "unit-backup-exporter", "unit-backup-orchestrator", "unit-benchmark-report-generator", "unit-bi-exporter", "unit-conmon-reporter", "unit-csrd-report-generator", "unit-csv-bom-exporter", "unit-data-exporter", "unit-evidence-collector", "unit-export-renderer", "unit-fedramp-control-mapper", "unit-financial-report-generator", "unit-focus-json-exporter", "unit-focus-validator", "unit-gdpr-deletion-handler", "unit-gdpr-export-generator", "unit-hipaa-report-generator", "unit-hipaa-security-mapper", "unit-nis2-assessment-engine", "unit-nis2-compliance-checker", "unit-nis2-report-exporter", "unit-optimization-policy-engine", "unit-phi-data-tracker", "unit-poam-manager", "unit-policy-validator", "unit-preventive-action-engine", "unit-report-generator", "unit-restore-manager", "unit-retention-executor", "unit-retention-policy-manager", "unit-saq-generator", "unit-soc2-control-framework", "unit-svg-diagram-exporter", "unit-svg-renderer", "unit-tier-export-enforcer")
$batchUnits6 = @("unit-3pao-prep", "unit-accuracy-scorer", "unit-alert-manager", "unit-audit-package-generator", "unit-aws-cur-connector", "unit-aws-executor", "unit-azure-ea-connector", "unit-azure-executor", "unit-benchmark-comparator", "unit-benchmark-data-manager", "unit-benchmark-data-provider", "unit-benchmark-matcher", "unit-billing-sync-orchestrator", "unit-business-unit-manager", "unit-cost-center-mapper", "unit-cross-cloud-mapper", "unit-custom-format-mapper", "unit-emission-calculator", "unit-energy-tracker", "unit-facility-cost-tracker", "unit-feedback-collector", "unit-finops-connector", "unit-gcp-catalog-adapter", "unit-gcp-executor", "unit-generic-resource-translator", "unit-human-in-loop-orchestrator", "unit-instant-value-calculator", "unit-invoice-generator", "unit-jira-connector", "unit-job-orchestrator", "unit-layout-engine", "unit-lease-accounting-mapper", "unit-notification-dispatcher", "unit-notification-template-engine", "unit-oracle-bridge", "unit-pci-requirements-mapper", "unit-rvtools-ingester", "unit-sample-data-loader", "unit-sap-bridge", "unit-servicenow-connector", "unit-snapshot-generator", "unit-software-capitalization-tracker", "unit-software-group-manager", "unit-spec-based-matcher", "unit-stripe-checkout-handler", "unit-subscription-manager", "unit-unit-economics-calculator", "unit-usage-metering")


$toRun = @()

switch ($Group) {
    "0" {
        $toRun = $batchUnits
        Write-Host "Selected batch units: $toRun" -ForegroundColor Yellow 
    }
    "1" {
        $toRun = $batchUnits1
        Write-Host "Selected batch units: $toRun" -ForegroundColor Yellow 
    }
    "2" {
        $toRun = $batchUnits2
        Write-Host "Selected batch units: $toRun" -ForegroundColor Yellow 
    }
    "3" {
        $toRun = $batchUnits3
        Write-Host "Selected batch units: $toRun" -ForegroundColor Yellow 
    }
    "4" {
        $toRun = $batchUnits4
        Write-Host "Selected batch units: $toRun" -ForegroundColor Yellow 
    }
    "5" {
        $toRun = $batchUnits5
        Write-Host "Selected batch units: $toRun" -ForegroundColor Yellow 
    }
    "6" {
        $toRun = $batchUnits6
        Write-Host "Selected batch units: $toRun" -ForegroundColor Yellow 
    }
    default {
        $toRun = @() 
        Write-Host "No batch units selected" -ForegroundColor Yellow 
    }
}

if ($StopAll) {
    Write-Host "Stopping all batch pods" -ForegroundColor Yellow
    kubectl scale deployment --replicas=0 --all -n cim
    exit
}

if ($Stop) {
    Write-Host "Stopping batch pods" -ForegroundColor Yellow

    foreach ($u in $toRun) {
        try {
            kubectl scale deployment --replicas=0 $u -n cim
        }
        catch {
            Write-Host "Error scaling $u : $($_.Exception.Message)" -ForegroundColor Red
        }
    }
 
}
else {
    
    Write-Host "Starting batch pods" -ForegroundColor Yellow

    foreach ($u in $toRun) {
        try {
            kubectl scale deployment --replicas=1 $u -n cim
        }
        catch {
            Write-Host "Error scaling $u : $($_.Exception.Message)" -ForegroundColor Red
        }
    }


    
}


Write-Host "`nBatch pods scaled!" -ForegroundColor Green