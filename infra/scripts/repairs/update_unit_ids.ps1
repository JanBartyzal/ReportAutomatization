# update_unit_ids.ps1
# Updates unitId in helm/values.yaml files based on status_microsrv.md mapping

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$unitsPath = Join-Path $scriptPath "units"

# Mapping from unit name to correct unitId (from status_microsrv.md)
$unitIdMapping = @{
    # Phase 1: Identity Domain (IDN)
    "unit-user-registry" = "U-IDN-001"
    "unit-organization-manager" = "U-IDN-002"
    "unit-rbac-enforcer" = "U-IDN-003"
    "unit-token-issuer" = "U-IDN-004"
    "unit-api-key-manager" = "U-IDN-005"
    "unit-audit-logger" = "U-IDN-006"

    # Phase 2: Pricing Domain (PRC)
    "unit-aws-pricing-fetcher" = "U-PRC-001"
    "unit-azure-retail-api-adapter" = "U-PRC-002"
    "unit-gcp-catalog-adapter" = "U-PRC-003"
    "unit-currency-converter" = "U-PRC-004"
    "unit-pricing-cache-manager" = "U-PRC-005"
    "unit-custom-pricebook-handler" = "U-PRC-006"
    "unit-price-change-detector" = "U-PRC-007"

    # Phase 2: Parser Domain (PRS)
    "unit-terraform-parser" = "U-PRS-001"
    "unit-bicep-parser" = "U-PRS-002"
    "unit-cloudformation-parser" = "U-PRS-003"
    "unit-rvtools-ingester" = "U-PRS-004"
    "unit-custom-format-mapper" = "U-PRS-005"
    "unit-focus-validator" = "U-PRS-006"

    # Phase 3: Strategic Portfolio Management (SPM)
    "unit-portfolio-aggregator" = "U-SPM-001"
    "unit-burnrate-calculator" = "U-SPM-002"
    "unit-guardrail-enforcer" = "U-SPM-003"
    "unit-scenario-manager" = "U-SPM-004"

    # Phase 3: Governance Domain (GOV)
    "unit-approval-workflow-engine" = "U-GOV-001"
    "unit-approval-policy-manager" = "U-GOV-002"
    "unit-policy-validator" = "U-GOV-003"

    # Phase 3: Remediation Domain (REM)
    "unit-remediation-planner" = "U-REM-001"
    "unit-aws-executor" = "U-REM-002"
    "unit-azure-executor" = "U-REM-003"
    "unit-gcp-executor" = "U-REM-004"
    "unit-audit-chain-verifier" = "U-REM-005"

    # Phase 4: AI Analysis Domain (AIA)
    "unit-architecture-analyzer" = "U-AIA-001"
    "unit-overprovisioning-detector" = "U-AIA-002"
    "unit-spot-ri-recommender" = "U-AIA-003"
    "unit-sku-validator" = "U-AIA-004"
    "unit-nlq-processor" = "U-AIA-005"

    # Phase 4: Forecasting Domain (FCT)
    "unit-cost-forecaster" = "U-FCT-001"
    "unit-anomaly-detector" = "U-FCT-002"

    # Phase 4: Executive Domain (EXE)
    "unit-executive-dashboard" = "U-EXE-001"
    "unit-snapshot-generator" = "U-EXE-002"
    "unit-business-unit-manager" = "U-EXE-003"
    "unit-unit-economics-calculator" = "U-EXE-004"

    # Phase 5: ITBM Domain (ITB)
    "unit-labor-cost-tracker" = "U-ITB-001"
    "unit-asset-registry" = "U-ITB-002"
    "unit-license-manager" = "U-ITB-003"
    "unit-tco-calculator" = "U-ITB-004"

    # Phase 5: Billing Domain (BIL)
    "unit-stripe-checkout-handler" = "U-BIL-001"
    "unit-subscription-manager" = "U-BIL-002"
    "unit-usage-metering" = "U-BIL-003"
    "unit-invoice-generator" = "U-BIL-004"

    # Phase 5: Real-World Billing Domain (RWB)
    "unit-aws-cur-connector" = "U-RWB-001"
    "unit-azure-ea-connector" = "U-RWB-002"
    "unit-plan-vs-reality-analyzer" = "U-RWB-003"
    "unit-billing-sync-orchestrator" = "U-RWB-004"

    # Phase 5: On-Premise Domain (OPR)
    "unit-datacenter-manager" = "U-OPR-001"
    "unit-server-inventory" = "U-OPR-002"
    "unit-power-cost-calculator" = "U-OPR-003"
    "unit-software-license-tracker" = "U-OPR-004"
    "unit-facility-cost-tracker" = "U-OPR-005"
    "unit-power-profile-library" = "U-OPR-006"
    "unit-power-efficiency-engine" = "U-OPR-007"
    "unit-software-group-manager" = "U-OPR-008"
    "unit-onprem-tco-reporter" = "U-OPR-009"

    # Phase 5: Marketplace Domain (MKT)
    "unit-blueprint-registry" = "U-MKT-001"
    "unit-blueprint-applier" = "U-MKT-002"

    # Phase 5: Integrations Domain (INT)
    "unit-jira-connector" = "U-INT-001"
    "unit-servicenow-connector" = "U-INT-002"
    "unit-notification-dispatcher" = "U-INT-003"
    "unit-alert-manager" = "U-INT-004"
    "unit-notification-template-engine" = "U-INT-005"

    # Phase 5: Compliance Domain (CMP) - GDPR/NIS2
    "unit-gdpr-export-generator" = "U-CMP-001"
    "unit-gdpr-deletion-handler" = "U-CMP-002"
    "unit-nis2-compliance-checker" = "U-CMP-003"

    # Phase 5: Sustainability Domain (SUS)
    "unit-emission-calculator" = "U-SUS-001"
    "unit-csrd-report-generator" = "U-SUS-002"
    "unit-energy-tracker" = "U-SUS-003"

    # Phase 5: Network Calculator Domain (NET)
    "unit-egress-calculator" = "U-NET-001"
    "unit-vnet-peering-estimator" = "U-NET-002"
    "unit-network-topology-analyzer" = "U-NET-003"

    # Phase 5: Storage Calculator Domain (STO)
    "unit-storage-cost-calculator" = "U-STO-001"
    "unit-capacity-planner" = "U-STO-002"

    # Phase 5: Benchmark Domain (BEN)
    "unit-benchmark-comparator" = "U-BEN-001"
    "unit-benchmark-data-manager" = "U-BEN-002"
    "unit-benchmark-report-generator" = "U-BEN-003"

    # Phase 5: Rightsizing Domain (RSZ)
    "unit-rightsizing-analyzer" = "U-RSZ-001"
    "unit-rightsizing-recommender" = "U-RSZ-002"
    "unit-rightsizing-executor" = "U-RSZ-003"
    "unit-rightsizing-history-tracker" = "U-RSZ-004"

    # Phase 5: Designer Domain (DES)
    "unit-canvas-manager" = "U-DES-001"
    "unit-ai-layout-generator" = "U-DES-002"
    "unit-component-library" = "U-DES-003"
    "unit-canvas-diff-engine" = "U-DES-004"

    # Phase 5: Cross-Cloud Mapping Domain (XCM)
    "unit-sku-taxonomy-manager" = "U-XCM-001"
    "unit-cross-cloud-mapper" = "U-XCM-002"
    "unit-spec-based-matcher" = "U-XCM-003"
    "unit-generic-resource-translator" = "U-XCM-004"
    "unit-multicloud-terraform-generator" = "U-XCM-005"
    "unit-migration-path-analyzer" = "U-XCM-006"

    # Phase 6: Branding Domain (BRN)
    "unit-branding-manager" = "U-BRN-001"
    "unit-pdf-branding-applier" = "U-BRN-002"
    "unit-geoarbitrage-calculator" = "U-BRN-003"
    "unit-precision-tracker" = "U-BRN-004"

    # Phase 6: Quick Win Dashboard Domain (QWD)
    "unit-instant-value-calculator" = "U-QWD-001"
    "unit-savings-analyzer" = "U-QWD-002"
    "unit-accuracy-scorer" = "U-QWD-003"
    "unit-benchmark-matcher" = "U-QWD-004"
    "unit-email-alert-generator" = "U-QWD-005"

    # Phase 6: Export Domain (EXP)
    "unit-focus-json-exporter" = "U-EXP-001"
    "unit-svg-diagram-exporter" = "U-EXP-002"
    "unit-csv-bom-exporter" = "U-EXP-003"
    "unit-tier-export-enforcer" = "U-EXP-004"

    # Phase 6: Search Domain (SRH)
    "unit-sku-parameter-search" = "U-SRH-001"
    "unit-plan-component-search" = "U-SRH-002"
    "unit-search-index-builder" = "U-SRH-003"

    # Phase 6: Utilization Domain (UTL)
    "unit-azure-monitor-connector" = "U-UTL-001"
    "unit-aws-cloudwatch-connector" = "U-UTL-002"
    "unit-prometheus-connector" = "U-UTL-003"
    "unit-utilization-correlator" = "U-UTL-004"
    "unit-waste-identifier" = "U-UTL-005"

    # Phase 6: Onboarding Domain (ONB)
    "unit-sample-data-loader" = "U-ONB-001"
    "unit-feedback-collector" = "U-ONB-002"
    "unit-sandbox-manager" = "U-ONB-003"
    "unit-guided-tour-engine" = "U-ONB-004"
    "unit-context-switcher" = "U-ONB-005"

    # Phase 6: Identity Provider Domain (IDP)
    "unit-oidc-saml-connector" = "U-IDP-001"
    "unit-social-login-handler" = "U-IDP-002"
    "unit-local-account-manager" = "U-IDP-003"
    "unit-mfa-manager" = "U-IDP-004"
    "unit-scim-provisioner" = "U-IDP-005"

    # Phase 7: Enterprise Integration Domain (ERP)
    "unit-sap-bridge" = "U-ERP-001"
    "unit-oracle-bridge" = "U-ERP-002"
    "unit-bi-exporter" = "U-ERP-003"
    "unit-finops-connector" = "U-ERP-004"

    # Phase 7: NIS2 Compliance Domain (NIS)
    "unit-nis2-assessment-engine" = "U-NIS-001"
    "unit-nis2-dashboard-generator" = "U-NIS-002"
    "unit-nis2-report-exporter" = "U-NIS-003"

    # Phase 7: Backup Domain (BCK)
    "unit-backup-orchestrator" = "U-BCK-001"
    "unit-backup-exporter" = "U-BCK-002"
    "unit-restore-manager" = "U-BCK-003"

    # Phase 7: Retention Domain (RET)
    "unit-retention-policy-manager" = "U-RET-001"
    "unit-retention-executor" = "U-RET-002"
    "unit-anonymization-engine" = "U-RET-003"

    # Phase 7: Multi-Plan & Scenarios Domain (MPV)
    "unit-multiplan-aggregator" = "U-MPV-001"
    "unit-default-scenario-library" = "U-MPV-002"
    "unit-gap-analyzer" = "U-MPV-003"
    "unit-scenario-comparator" = "U-MPV-004"

    # Phase 8: SOC2 Compliance Domain (SOC)
    "unit-soc2-control-framework" = "U-SOC-001"
    "unit-evidence-collector" = "U-SOC-002"
    "unit-soc2-dashboard" = "U-SOC-003"
    "unit-audit-package-generator" = "U-SOC-004"
    "unit-control-remediation-tracker" = "U-SOC-005"

    # Phase 8: HIPAA Compliance Domain (HIP)
    "unit-hipaa-security-mapper" = "U-HIP-001"
    "unit-phi-data-tracker" = "U-HIP-002"
    "unit-baa-manager" = "U-HIP-003"
    "unit-hipaa-report-generator" = "U-HIP-004"

    # Phase 8: FedRAMP Readiness Domain (FED)
    "unit-fedramp-control-mapper" = "U-FED-001"
    "unit-poam-manager" = "U-FED-002"
    "unit-conmon-reporter" = "U-FED-003"
    "unit-3pao-prep" = "U-FED-004"

    # Phase 8: PCI DSS Compliance Domain (PCI)
    "unit-pci-requirements-mapper" = "U-PCI-001"
    "unit-cde-scope-analyzer" = "U-PCI-002"
    "unit-saq-generator" = "U-PCI-003"
    "unit-aoc-generator" = "U-PCI-004"

    # Phase 8: US GAAP Financial Domain (GAP)
    "unit-software-capitalization-tracker" = "U-GAP-001"
    "unit-lease-accounting-mapper" = "U-GAP-002"
    "unit-cost-center-mapper" = "U-GAP-003"
    "unit-financial-report-generator" = "U-GAP-004"

    # Phase 9: Autonomous FinOps Domain (AFA)
    "unit-optimization-policy-engine" = "U-AFA-001"
    "unit-autonomous-action-executor" = "U-AFA-002"
    "unit-savings-attribution-tracker" = "U-AFA-003"
    "unit-human-in-loop-orchestrator" = "U-AFA-004"
    "unit-autonomy-dashboard" = "U-AFA-005"

    # Phase 9: Natural Language Infrastructure Domain (NLI)
    "unit-nlq-query-processor" = "U-NLI-001"
    "unit-intent-classifier" = "U-NLI-002"
    "unit-nl-provisioner" = "U-NLI-003"
    "unit-conversational-optimizer" = "U-NLI-004"

    # Phase 9: Predictive Anomaly Resolution Domain (PAR)
    "unit-anomaly-predictor" = "U-PAR-001"
    "unit-root-cause-analyzer" = "U-PAR-002"
    "unit-preventive-action-engine" = "U-PAR-003"
    "unit-anomaly-resolution-dashboard" = "U-PAR-004"

    # Feature Set 47: Dynamic Config & Secrets (DCS)
    "unit-config-distributor" = "U-DCS-001"
    "unit-environment-profile-manager" = "U-DCS-003"
    "unit-secret-discovery-engine" = "U-DCS-008"
    "unit-secret-health-monitor" = "U-DCS-009"
    "unit-vault-connector" = "U-DCS-010"

    # Future 1: Composite Patterns Domain (CPT)
    "unit-composite-sku-registry" = "U-CPT-001"
    "unit-pattern-library-manager" = "U-CPT-002"
    "unit-plan-as-component" = "U-CPT-003"
    "unit-hierarchy-visualizer" = "U-CPT-004"
    "unit-pattern-designer" = "U-CPT-005"
    "unit-pattern-instantiator" = "U-CPT-006"

    # Worker Domain (WRK)
    "unit-job-orchestrator" = "U-WRK-001"
    "unit-price-sync-runner" = "U-WRK-002"
    "unit-asset-depreciation-runner" = "U-WRK-003"
    "unit-license-alert-runner" = "U-WRK-004"
    "unit-sandbox-cleanup-runner" = "U-WRK-005"

    # Telemetry Domain (TEL)
    "unit-telemetry-collector" = "U-TEL-001"
    "unit-ml-model-trainer" = "U-TEL-002"

    # Visualization Domain (VIZ)
    "unit-report-generator" = "U-VIZ-001"
    "unit-svg-renderer" = "U-VIZ-002"
    "unit-data-exporter" = "U-VIZ-003"

    # Web Domain (WEB)
    "unit-web-landing-page" = "U-WEB-001"
    "unit-web-frontend" = "U-WEB-002"
    "unit-web-gateway" = "U-WEB-003"
}

$updatedCount = 0
$skippedCount = 0
$notFoundCount = 0

# Get all unit folders
$unitFolders = Get-ChildItem -Path $unitsPath -Directory | Where-Object { $_.Name -like "unit-*" } | Sort-Object Name

foreach ($unit in $unitFolders) {
    $unitName = $unit.Name
    $helmValuesPath = Join-Path $unit.FullName "helm\values.yaml"

    if (-not (Test-Path $helmValuesPath)) {
        Write-Host "SKIP: $unitName - no helm/values.yaml" -ForegroundColor Yellow
        $skippedCount++
        continue
    }

    $content = Get-Content $helmValuesPath -Raw -Encoding UTF8

    # Check if it has U-XXX-000
    if ($content -notmatch 'unitId:\s*"U-XXX-000"') {
        Write-Host "SKIP: $unitName - already has correct unitId" -ForegroundColor Gray
        $skippedCount++
        continue
    }

    # Look up the correct unitId
    if ($unitIdMapping.ContainsKey($unitName)) {
        $correctUnitId = $unitIdMapping[$unitName]
        $newContent = $content -replace 'unitId:\s*"U-XXX-000"', "unitId: `"$correctUnitId`""

        Set-Content -Path $helmValuesPath -Value $newContent -Encoding UTF8 -NoNewline
        Write-Host "UPDATED: $unitName -> $correctUnitId" -ForegroundColor Green
        $updatedCount++
    } else {
        Write-Host "NOT FOUND: $unitName - no mapping defined" -ForegroundColor Red
        $notFoundCount++
    }
}

Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
Write-Host "Updated: $updatedCount"
Write-Host "Skipped: $skippedCount"
Write-Host "Not Found (no mapping): $notFoundCount"
