# UAT Master Runner - ReportAutomatization (RA)
# Spusti vsechny kroky Step00 az Step25 v poradi, pak vygeneruje report.

param(
    [string]$StartStep = "00",
    [string]$EndStep = "25",
    [switch]$StopOnFail
)

$ErrorActionPreference = "Continue"
$rootDir = $(if ($PSScriptRoot) { $PSScriptRoot } else { $PWD.Path })
$logsDir = Join-Path $rootDir "logs"

if (-not (Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir | Out-Null
}

if ( (Test-Path $logsDir)) {
    Get-ChildItem -Path $logsDir -File | Remove-Item -Force
}


Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  RA UAT Master Runner" -ForegroundColor Cyan
Write-Host "  Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""

$steps = @(
    @{ Num = "00"; Name = "Init"; Dir = "Step00_Init"; Script = "test_00_init.py"; Log = "step00_Init" },
    @{ Num = "01"; Name = "Infrastructure_Auth"; Dir = "Step01_Infrastructure_Auth"; Script = "test_01_infrastructure_auth.py"; Log = "step01_Infrastructure_Auth" },
    @{ Num = "02"; Name = "File_Upload"; Dir = "Step02_File_Upload"; Script = "test_02_file_upload.py"; Log = "step02_File_Upload" },
    @{ Num = "03"; Name = "Atomizer_PPTX"; Dir = "Step03_Atomizer_PPTX"; Script = "test_03_atomizer_pptx.py"; Log = "step03_Atomizer_PPTX" },
    @{ Num = "04"; Name = "Orchestrator_Workflow"; Dir = "Step04_Orchestrator_Workflow"; Script = "test_04_orchestrator_workflow.py"; Log = "step04_Orchestrator_Workflow" },
    @{ Num = "05"; Name = "Sinks_Persistence"; Dir = "Step05_Sinks_Persistence"; Script = "test_05_sinks_persistence.py"; Log = "step05_Sinks_Persistence" },
    @{ Num = "06"; Name = "Analytics_Query"; Dir = "Step06_Analytics_Query"; Script = "test_06_analytics_query.py"; Log = "step06_Analytics_Query" },
    @{ Num = "07"; Name = "Admin_Management"; Dir = "Step07_Admin_Management"; Script = "test_07_admin_management.py"; Log = "step07_Admin_Management" },
    @{ Num = "08"; Name = "Batch_Organization"; Dir = "Step08_Batch_Organization"; Script = "test_08_batch_organization.py"; Log = "step08_Batch_Organization" },
    @{ Num = "09"; Name = "Frontend_SPA"; Dir = "Step09_Frontend_SPA"; Script = "test_09_frontend_spa.py"; Log = "step09_Frontend_SPA" },
    @{ Num = "10"; Name = "Atomizer_Excel"; Dir = "Step10_Atomizer_Excel"; Script = "test_10_atomizer_excel.py"; Log = "step10_Atomizer_Excel" },
    @{ Num = "11"; Name = "Dashboards_SQL"; Dir = "Step11_Dashboards_SQL"; Script = "test_11_dashboards_sql.py"; Log = "step11_Dashboards_SQL" },
    @{ Num = "12"; Name = "API_AI_MCP"; Dir = "Step12_API_AI_MCP"; Script = "test_12_api_ai_mcp.py"; Log = "step12_API_AI_MCP" },
    @{ Num = "13"; Name = "Notifications"; Dir = "Step13_Notifications"; Script = "test_13_notifications.py"; Log = "step13_Notifications" },
    @{ Num = "14"; Name = "Data_Versioning"; Dir = "Step14_Data_Versioning"; Script = "test_14_data_versioning.py"; Log = "step14_Data_Versioning" },
    @{ Num = "15"; Name = "Schema_Mapping"; Dir = "Step15_Schema_Mapping"; Script = "test_15_schema_mapping.py"; Log = "step15_Schema_Mapping" },
    @{ Num = "16"; Name = "Audit_Compliance"; Dir = "Step16_Audit_Compliance"; Script = "test_16_audit_compliance.py"; Log = "step16_Audit_Compliance" },
    @{ Num = "17"; Name = "Report_Lifecycle"; Dir = "Step17_Report_Lifecycle"; Script = "test_17_report_lifecycle.py"; Log = "step17_Report_Lifecycle" },
    @{ Num = "18"; Name = "PPTX_Generation"; Dir = "Step18_PPTX_Generation"; Script = "test_18_pptx_generation.py"; Log = "step18_PPTX_Generation" },
    @{ Num = "19"; Name = "Form_Builder"; Dir = "Step19_Form_Builder"; Script = "test_19_form_builder.py"; Log = "step19_Form_Builder" },
    @{ Num = "20"; Name = "Period_Management"; Dir = "Step20_Period_Management"; Script = "test_20_period_management.py"; Log = "step20_Period_Management" },
    @{ Num = "21"; Name = "Local_Forms"; Dir = "Step21_Local_Forms"; Script = "test_21_local_forms.py"; Log = "step21_Local_Forms" },
    @{ Num = "22"; Name = "Period_Comparison"; Dir = "Step22_Period_Comparison"; Script = "test_22_period_comparison.py"; Log = "step22_Period_Comparison" },
    @{ Num = "23"; Name = "ServiceNow_Integration"; Dir = "Step23_ServiceNow_Integration"; Script = "test_23_servicenow_integration.py"; Log = "step23_ServiceNow_Integration" },
    @{ Num = "24"; Name = "Smart_Persistence"; Dir = "Step24_Smart_Persistence"; Script = "test_24_smart_persistence.py"; Log = "step24_Smart_Persistence" },
    @{ Num = "25"; Name = "DevOps_Observability"; Dir = "Step25_DevOps_Observability"; Script = "test_25_devops_observability.py"; Log = "step25_DevOps_Observability" },
    @{ Num = "26"; Name = "Excel_Sync"; Dir = "Step26_Excel_Sync"; Script = "test_26_excel_sync.py"; Log = "step26_Excel_Sync" }
)

$results = @()
$totalPass = 0
$totalFail = 0

foreach ($step in $steps) {
    if ([int]$step.Num -lt [int]$StartStep -or [int]$step.Num -gt [int]$EndStep) {
        continue
    }

    $stepDir = Join-Path $rootDir $step.Dir
    $logFile = Join-Path $logsDir "$($step.Log).log"
    $scriptPath = Join-Path $stepDir $step.Script

    if (-not (Test-Path $scriptPath)) {
        Write-Host "  [SKIP] Step$($step.Num) $($step.Name) - script not found" -ForegroundColor Yellow
        $results += [PSCustomObject]@{ Step = $step.Name; Status = "SKIP"; ExitCode = -1 }
        continue
    }

    Write-Host "------------------------------------------------------" -ForegroundColor DarkGray
    Write-Host "  Step $($step.Num): $($step.Name)" -ForegroundColor White
    Write-Host "  Script: $scriptPath" -ForegroundColor DarkGray

    $startTime = Get-Date
    Push-Location $stepDir
    try {
        $output = & python $step.Script 2>&1
        $exitCode = $LASTEXITCODE
        $output | Tee-Object -FilePath $logFile
    }
    finally {
        Pop-Location
    }

    $elapsed = [math]::Round(((Get-Date) - $startTime).TotalSeconds, 1)

    if ($exitCode -eq 0) {
        $status = "PASS"
        $color = "Green"
        $totalPass++
    }
    else {
        $status = "FAIL"
        $color = "Red"
        $totalFail++
    }

    $msg = "  Result: {0}  (exit={1}, {2}s)" -f $status, $exitCode, $elapsed
    Write-Host $msg -ForegroundColor $color
    $results += [PSCustomObject]@{ Step = $step.Name; Status = $status; ExitCode = $exitCode; Elapsed = $elapsed }

    if ($StopOnFail -and $exitCode -ne 0) {
        Write-Host "`n[!] StopOnFail set - aborting after failed step." -ForegroundColor Red
        break
    }
}

# Generate report
Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  Generating UAT Report..." -ForegroundColor Cyan
python (Join-Path $rootDir "shared/report_generator.py") $logsDir
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Steps run: $($results.Count)" -ForegroundColor White
Write-Host "  PASSED:    $totalPass" -ForegroundColor Green
Write-Host "  FAILED:    $totalFail" -ForegroundColor Red
Write-Host "  Finished:  $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White
Write-Host ""

foreach ($r in $results) {
    $color = $(if ($r.Status -eq "PASS") { "Green" } elseif ($r.Status -eq "FAIL") { "Red" } else { "Yellow" })
    Write-Host "  [$($r.Status.PadRight(4))] $($r.Step)" -ForegroundColor $color
}

Write-Host ""
if ($totalFail -eq 0) {
    Write-Host "  ALL STEPS PASSED" -ForegroundColor Green
    exit 0
}
else {
    Write-Host "  $totalFail STEP(S) FAILED" -ForegroundColor Red
    exit 1
}
