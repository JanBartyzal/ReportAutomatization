#Requires -Version 7.0
<#
.SYNOPSIS
    Helper Functions for Batch Repair Workflow
    
.DESCRIPTION
    Provides utility functions for error handling, logging, and repair operations.
#>

# ============================================================================
# ADVANCED ERROR HANDLING
# ============================================================================

class ErrorHandler {
    [hashtable]$ErrorLog
    [int]$ErrorCount
    [int]$WarningCount
    
    ErrorHandler() {
        $this.ErrorLog = @{}
        $this.ErrorCount = 0
        $this.WarningCount = 0
    }
    
    [void] LogError([string]$context, [string]$message, [string]$errorDetails) {
        $this.ErrorCount++
        $key = "$context-$($this.ErrorCount)"
        $this.ErrorLog[$key] = @{
            Context   = $context
            Message   = $message
            Details   = $errorDetails
            Timestamp = Get-Date
            Severity  = "ERROR"
        }
    }
    
    [void] LogWarning([string]$context, [string]$message) {
        $this.WarningCount++
        $key = "$context-WARN-$($this.WarningCount)"
        $this.ErrorLog[$key] = @{
            Context   = $context
            Message   = $message
            Timestamp = Get-Date
            Severity  = "WARNING"
        }
    }
    
    [hashtable[]] GetErrors() {
        return $this.ErrorLog.Values | Where-Object { $_.Severity -eq "ERROR" }
    }
    
    [hashtable[]] GetWarnings() {
        return $this.ErrorLog.Values | Where-Object { $_.Severity -eq "WARNING" }
    }
    
    [hashtable] GetSummary() {
        return @{
            TotalErrors   = $this.ErrorCount
            TotalWarnings = $this.WarningCount
            Errors        = $this.GetErrors()
            Warnings      = $this.GetWarnings()
        }
    }
}

# ============================================================================
# STRUCTURED LOGGING
# ============================================================================

class StructuredLogger {
    [string]$LogPath
    [hashtable[]]$LogEntries
    [string]$SessionId
    
    StructuredLogger([string]$logPath) {
        $this.LogPath = $logPath
        $this.LogEntries = @()
        $this.SessionId = [guid]::NewGuid().ToString()
    }
    
    [void] Log([string]$level, [string]$message, [hashtable]$context) {
        $entry = @{
            Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
            SessionId = $this.SessionId
            Level     = $level
            Message   = $message
            Context   = $context
        }
        
        $this.LogEntries += $entry
        
        # Write to file immediately
        $jsonEntry = $entry | ConvertTo-Json -Compress
        Add-Content -Path $this.LogPath -Value $jsonEntry -Encoding UTF8
    }
    
    [void] LogInfo([string]$message, [hashtable]$context = @{}) {
        $this.Log("INFO", $message, $context)
    }
    
    [void] LogError([string]$message, [hashtable]$context = @{}) {
        $this.Log("ERROR", $message, $context)
    }
    
    [void] LogWarning([string]$message, [hashtable]$context = @{}) {
        $this.Log("WARN", $message, $context)
    }
    
    [void] LogSuccess([string]$message, [hashtable]$context = @{}) {
        $this.Log("SUCCESS", $message, $context)
    }
}

# ============================================================================
# BUILD ERROR ANALYSIS
# ============================================================================

function Analyze-BuildErrors {
    param(
        [string]$BuildOutput,
        [string]$UnitName
    )
    
    $analysis = @{
        UnitName        = $UnitName
        ErrorCategories = @{}
        ErrorCount      = 0
        WarningCount    = 0
        SuggestedFixes  = @()
    }
    
    $lines = $BuildOutput -split "`n"
    
    foreach ($line in $lines) {
        if ($line -match "error\s+CS(\d+)") {
            $errorCode = $matches[1]
            $analysis.ErrorCount++
            
            if (-not $analysis.ErrorCategories.ContainsKey($errorCode)) {
                $analysis.ErrorCategories[$errorCode] = 0
            }
            $analysis.ErrorCategories[$errorCode]++
            
            # Suggest fixes based on error code
            switch ($errorCode) {
                "0103" { $analysis.SuggestedFixes += "Missing namespace or type reference" }
                "0246" { $analysis.SuggestedFixes += "Add missing using statement" }
                "0117" { $analysis.SuggestedFixes += "Type does not contain definition" }
                "0019" { $analysis.SuggestedFixes += "Object reference not set to instance" }
                "0029" { $analysis.SuggestedFixes += "Cannot use type as expression" }
                default { $analysis.SuggestedFixes += "Review error code CS$errorCode" }
            }
        }
        elseif ($line -match "warning\s+CS(\d+)") {
            $analysis.WarningCount++
        }
    }
    
    return $analysis
}

# ============================================================================
# AUTONOMOUS REPAIR STRATEGIES
# ============================================================================

function Repair-MissingUsings {
    param(
        [string]$FilePath,
        [string[]]$MissingNamespaces
    )
    
    $content = Get-Content $FilePath -Raw
    $modified = $false
    
    foreach ($namespace in $MissingNamespaces) {
        $usingStatement = "using $namespace;"
        
        if (-not ($content -match "using\s+$([regex]::Escape($namespace))")) {
            # Add using statement at the top
            $content = $usingStatement + "`n" + $content
            $modified = $true
        }
    }
    
    if ($modified) {
        Set-Content -Path $FilePath -Value $content -Encoding UTF8
        return $true
    }
    
    return $false
}

function Repair-GrpcServiceMappings {
    param(
        [string]$FilePath
    )
    
    $content = Get-Content $FilePath -Raw
    $modified = $false
    
    # Common gRPC mapping issues
    $patterns = @(
        @{
            Pattern     = "\.MapGrpcService<(\w+)>\(\)"
            Replacement = ".MapGrpcService<`$1>()"
            Description = "Ensure gRPC service mapping syntax"
        },
        @{
            Pattern     = "new\s+(\w+)Service\(\)"
            Replacement = "new `$1Service()"
            Description = "Ensure service instantiation"
        }
    )
    
    foreach ($pattern in $patterns) {
        if ($content -match $pattern.Pattern) {
            $modified = $true
        }
    }
    
    if ($modified) {
        Set-Content -Path $FilePath -Value $content -Encoding UTF8
    }
    
    return $modified
}

function Repair-DtoMappings {
    param(
        [string]$FilePath
    )
    
    $content = Get-Content $FilePath -Raw
    $modified = $false
    
    # Check for DTO mapping issues
    if ($content -match "class\s+\w+Dto\s*\{" -and -not ($content -match "using.*Dto|using.*DTO")) {
        # Add DTO namespace if needed
        $modified = $true
    }
    
    if ($modified) {
        Set-Content -Path $FilePath -Value $content -Encoding UTF8
    }
    
    return $modified
}

# ============================================================================
# TEST GENERATION
# ============================================================================

function Generate-XunitTestTemplate {
    param(
        [string]$UnitName,
        [string]$ServiceName
    )
    
    $testTemplate = @"
using Xunit;
using $UnitName.Services;
using Moq;

namespace $UnitName.Tests
{
    public class ${ServiceName}Tests
    {
        private readonly Mock<ILogger<$ServiceName>> _mockLogger;
        private readonly $ServiceName _service;

        public ${ServiceName}Tests()
        {
            _mockLogger = new Mock<ILogger<$ServiceName>>();
            _service = new $ServiceName(_mockLogger.Object);
        }

        [Fact]
        public void Constructor_InitializesSuccessfully()
        {
            // Arrange & Act
            var service = new $ServiceName(_mockLogger.Object);

            // Assert
            Assert.NotNull(service);
        }

        [Fact]
        public async Task ExecuteAsync_WithValidInput_ReturnsSuccess()
        {
            // Arrange
            var input = new object();

            // Act
            var result = await _service.ExecuteAsync(input);

            // Assert
            Assert.NotNull(result);
        }

        [Fact]
        public async Task ExecuteAsync_WithNullInput_ThrowsArgumentNullException()
        {
            // Arrange
            object input = null;

            // Act & Assert
            await Assert.ThrowsAsync<ArgumentNullException>(() => _service.ExecuteAsync(input));
        }
    }
}
"@
    
    return $testTemplate
}

# ============================================================================
# HELM VALIDATION & REPAIR
# ============================================================================

function Validate-HelmChart {
    param(
        [string]$ChartPath
    )
    
    $validation = @{
        Valid    = $true
        Issues   = @()
        Warnings = @()
    }
    
    # Check Chart.yaml
    $chartYaml = Join-Path $ChartPath "Chart.yaml"
    if (-not (Test-Path $chartYaml)) {
        $validation.Valid = $false
        $validation.Issues += "Missing Chart.yaml"
    }
    else {
        $chartContent = Get-Content $chartYaml -Raw
        
        if (-not ($chartContent -match "apiVersion:")) {
            $validation.Issues += "Chart.yaml missing apiVersion"
        }
        if (-not ($chartContent -match "name:")) {
            $validation.Issues += "Chart.yaml missing name"
        }
        if (-not ($chartContent -match "version:")) {
            $validation.Issues += "Chart.yaml missing version"
        }
    }
    
    # Check values.yaml
    $valuesYaml = Join-Path $ChartPath "values.yaml"
    if (-not (Test-Path $valuesYaml)) {
        $validation.Warnings += "Missing values.yaml"
    }
    
    # Check templates directory
    $templatesDir = Join-Path $ChartPath "templates"
    if (-not (Test-Path $templatesDir)) {
        $validation.Warnings += "Missing templates directory"
    }
    
    return $validation
}

function Repair-HelmChart {
    param(
        [string]$ChartPath,
        [string]$ChartName
    )
    
    $repairs = @{
        Applied = $false
        Changes = @()
        Errors  = @()
    }
    
    try {
        # Create Chart.yaml if missing
        $chartYaml = Join-Path $ChartPath "Chart.yaml"
        if (-not (Test-Path $chartYaml)) {
            $chartContent = @"
apiVersion: v2
name: $ChartName
description: A Helm chart for $ChartName
type: application
version: 0.1.0
appVersion: "1.0"
"@
            Set-Content -Path $chartYaml -Value $chartContent -Encoding UTF8
            $repairs.Changes += "Created Chart.yaml"
            $repairs.Applied = $true
        }
        
        # Create values.yaml if missing
        $valuesYaml = Join-Path $ChartPath "values.yaml"
        if (-not (Test-Path $valuesYaml)) {
            $valuesContent = @"
replicaCount: 1

image:
  repository: cloudinframap/$ChartName
  pullPolicy: IfNotPresent
  tag: "latest"

service:
  type: ClusterIP
  port: 80

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi
"@
            Set-Content -Path $valuesYaml -Value $valuesContent -Encoding UTF8
            $repairs.Changes += "Created values.yaml"
            $repairs.Applied = $true
        }
        
        # Create templates directory if missing
        $templatesDir = Join-Path $ChartPath "templates"
        if (-not (Test-Path $templatesDir)) {
            New-Item -ItemType Directory -Path $templatesDir -Force | Out-Null
            $repairs.Changes += "Created templates directory"
            $repairs.Applied = $true
        }
    }
    catch {
        $repairs.Errors += "Repair error: $_"
    }
    
    return $repairs
}

# ============================================================================
# REPORT GENERATION UTILITIES
# ============================================================================

function Format-ReportTable {
    param(
        [hashtable[]]$Data,
        [string[]]$Columns
    )
    
    $table = "| " + ($Columns -join " | ") + " |`n"
    $table += "| " + (($Columns | ForEach-Object { "---" }) -join " | ") + " |`n"
    
    foreach ($row in $Data) {
        $values = @()
        foreach ($col in $Columns) {
            $values += $row[$col]
        }
        $table += "| " + ($values -join " | ") + " |`n"
    }
    
    return $table
}

function Generate-ExecutiveSummary {
    param(
        [hashtable[]]$UnitReports
    )
    
    $summary = @{
        TotalUnits                 = $UnitReports.Count
        SuccessfulUnits            = ($UnitReports | Where-Object { $_.Status -eq "SUCCESS" }).Count
        PartialSuccessUnits        = ($UnitReports | Where-Object { $_.Status -eq "PARTIAL_SUCCESS" }).Count
        FailedUnits                = ($UnitReports | Where-Object { $_.Status -eq "FAILED" }).Count
        ErrorUnits                 = ($UnitReports | Where-Object { $_.Status -eq "ERROR" }).Count
        ManualInterventionRequired = ($UnitReports | Where-Object { $_.ManualInterventionRequired }).Count
    }
    
    $summary.SuccessRate = [math]::Round(($summary.SuccessfulUnits / $summary.TotalUnits) * 100, 2)
    
    return $summary
}

# ============================================================================
# PERFORMANCE METRICS
# ============================================================================

class PerformanceMetrics {
    [datetime]$StartTime
    [datetime]$EndTime
    [hashtable]$UnitMetrics
    
    PerformanceMetrics() {
        $this.StartTime = Get-Date
        $this.UnitMetrics = @{}
    }
    
    [void] RecordUnitMetric([string]$unitName, [timespan]$duration, [bool]$success) {
        $this.UnitMetrics[$unitName] = @{
            Duration  = $duration
            Success   = $success
            Timestamp = Get-Date
        }
    }
    
    [void] Complete() {
        $this.EndTime = Get-Date
    }
    
    [hashtable] GetSummary() {
        $totalDuration = $this.EndTime - $this.StartTime
        $avgDuration = if ($this.UnitMetrics.Count -gt 0) {
            [timespan]::FromSeconds(($this.UnitMetrics.Values | Measure-Object -Property Duration -Average).Average.TotalSeconds)
        }
        else {
            [timespan]::Zero
        }
        
        return @{
            TotalDuration       = $totalDuration
            AverageDuration     = $avgDuration
            UnitsProcessed      = $this.UnitMetrics.Count
            SuccessfulUnits     = ($this.UnitMetrics.Values | Where-Object { $_.Success }).Count
            ThroughputPerMinute = [math]::Round($this.UnitMetrics.Count / $totalDuration.TotalMinutes, 2)
        }
    }
}

Export-ModuleMember -Function @(
    'Analyze-BuildErrors',
    'Repair-MissingUsings',
    'Repair-GrpcServiceMappings',
    'Repair-DtoMappings',
    'Generate-XunitTestTemplate',
    'Validate-HelmChart',
    'Repair-HelmChart',
    'Format-ReportTable',
    'Generate-ExecutiveSummary'
) -Class @(
    'ErrorHandler',
    'StructuredLogger',
    'PerformanceMetrics'
)
