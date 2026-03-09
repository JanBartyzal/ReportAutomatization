#Requires -Version 5.1
<#
.SYNOPSIS
    Temporary Helper Script for Kilocode CLI Invocation
    
.DESCRIPTION
    This is a temporary helper script generated dynamically by ai_repair_orchestrator.ps1
    to invoke kilocode CLI with AI repair prompts. The script accepts parameters for
    unit name, repair prompt, and AI model, then executes the kilocode new-task command
    with comprehensive error handling and logging.
    
.PARAMETER UnitName
    Name of the unit being repaired (required)
    Example: "unit-architecture-analyzer"
    
.PARAMETER PromptFile
    Path to the prompt template file (optional, defaults to ai_repair_prompt_template.txt)
    The file will be read and {{UNIT_NAME}} placeholder will be substituted with actual unit name
    
.PARAMETER Model
    AI model name to use for the repair task (required)
    Example: "anthropic/claude-haiku-4.5"
    
.PARAMETER LogFile
    Path to the log file for recording operations (required)
    
.EXAMPLE
    # Invoke repair for a specific unit (using default prompt template)
    .\ai_call_kilo_temp.ps1 -UnitName "unit-architecture-analyzer" `
        -Model "anthropic/claude-haiku-4.5" `
        -LogFile "C:\logs\repair.log"
    
.EXAMPLE
    # Invoke repair with custom prompt template file
    .\ai_call_kilo_temp.ps1 -UnitName "unit-architecture-analyzer" `
        -PromptFile "C:\templates\custom_prompt.txt" `
        -Model "anthropic/claude-haiku-4.5" `
        -LogFile "C:\logs\repair.log"
#>

#Requires -Version 5.1
<#
.SYNOPSIS
    Temporary Helper Script for Kilocode CLI Invocation
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$UnitName,
    
    [Parameter(Mandatory = $false)]
    [string]$PromptFile = "ai_repair_prompt_template.txt",
    
    [Parameter(Mandatory = $false)]
    [string]$Model = "kilo/anthropic/claude-haiku-4.5",
    
    [Parameter(Mandatory = $false)]
    [string]$LogFile = "ai_repair_prompt_log.txt"
)

$ErrorActionPreference = "Continue"
$executionTimestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$scriptStartTime = Get-Date

function Write-Log {
    param(
        [string]$Message,
        [ValidateSet("INFO", "WARN", "ERROR", "SUCCESS", "DEBUG")]
        [string]$Level = "INFO"
    )
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logEntry = "[$timestamp] [$Level] $Message"
    $color = switch ($Level) {
        "ERROR" { "Red" }
        "WARN" { "Yellow" }
        "SUCCESS" { "Green" }
        "DEBUG" { "Gray" }
        default { "White" }
    }
    Write-Host $logEntry -ForegroundColor $color
    try {
        Add-Content -Path $LogFile -Value $logEntry -ErrorAction SilentlyContinue
    }
    catch {
        Write-Host "WARNING: Could not write to log file: $_" -ForegroundColor Yellow
    }
}

function Write-Section {
    param([string]$Title)
    $separator = "=" * 80
    Write-Log $separator
    Write-Log $Title
    Write-Log $separator
}

function Validate-Parameters {
    Write-Section "PARAMETER VALIDATION"
    if ([string]::IsNullOrWhiteSpace($UnitName)) { return $false }
    if (-not (Test-Path $PromptFile)) { return $false }
    
    $logDir = Split-Path -Parent $LogFile
    if (-not (Test-Path $logDir)) {
        try { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
        catch { return $false }
    }
    return $true
}

function Read-PromptFile {
    try {
        return Get-Content -Path $PromptFile -Raw -ErrorAction Stop
    }
    catch {
        return $null
    }
}

function Substitute-UnitName {
    param([string]$PromptContent)
    try {
        return $PromptContent -replace '{{UNIT_NAME}}', $UnitName
    }
    catch {
        return $null
    }
}

function Test-KilcodeAvailability {
    try {
        $kilcodeCheck = Get-Command kilocode -ErrorAction SilentlyContinue
        return $null -ne $kilcodeCheck
    }
    catch {
        return $false
    }
}

function Extract-TaskId {
    param([string[]]$Output)
    foreach ($line in $Output) {
        if ($line -match "task[_-]?id[:\s]+([a-zA-Z0-9\-_]+)" -or $line -match "created.*?([a-zA-Z0-9\-_]{8,})") {
            return $Matches[1]
        }
    }
    return "UNKNOWN"
}

function Invoke-KilcodeTask {
    param([string]$PromptContent)
    Write-Section "INVOKING KILOCODE CLI"
    try {
        $output = & kilocode run $PromptContent --model $Model 2>&1
        $exitCode = $LASTEXITCODE
        return @{
            Success  = ($exitCode -eq 0)
            ExitCode = $exitCode
            Output   = $output
            TaskId   = Extract-TaskId -Output $output
        }
    }
    catch {
        return @{ Success = $false; ExitCode = -1; TaskId = $null }
    }
}

function Generate-ExecutionSummary {
    param([hashtable]$Result)
    Write-Section "EXECUTION SUMMARY"
    Write-Log "Unit Name: $UnitName"
    Write-Log "Status: $(if ($Result.Success) { 'SUCCESS' } else { 'FAILED' })"
}

function Main {
    if (-not (Validate-Parameters)) { exit 1 }
    $promptContent = Read-PromptFile
    if ($null -eq $promptContent) { exit 3 }
    
    $substitutedPrompt = Substitute-UnitName -PromptContent $promptContent
    if (-not (Test-KilcodeAvailability)) { exit 2 }
    
    $result = Invoke-KilcodeTask -PromptContent $substitutedPrompt
    Generate-ExecutionSummary -Result $result
    
    if ($result.Success) { exit 0 } else { exit $result.ExitCode }
}

Main