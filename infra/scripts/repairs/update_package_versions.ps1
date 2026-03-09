#Requires -Version 5.1
<#
.SYNOPSIS
    Update package versions in all unit .csproj files to match BaseUnit
#>

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$unitsRoot = Join-Path $scriptRoot "units"

# Package versions from BaseUnit - these are the canonical versions
$packageVersions = @{
    "Grpc.AspNetCore" = "2.67.0"
    "Grpc.AspNetCore.Server.Reflection" = "2.67.0"
    "Grpc.Net.Client" = "2.67.0"
    "Grpc.Tools" = "2.67.0"
    "Google.Protobuf" = "3.29.3"
    "Dapr.AspNetCore" = "1.14.0"
    "Dapr.Client" = "1.14.0"
    "Serilog.AspNetCore" = "8.0.1"
    "Serilog.Sinks.Console" = "5.0.1"
    "OpenTelemetry.Extensions.Hosting" = "1.8.1"
    "OpenTelemetry.Instrumentation.AspNetCore" = "1.8.1"
    "OpenTelemetry.Instrumentation.GrpcNetClient" = "1.8.0-beta.1"
    "OpenTelemetry.Exporter.OpenTelemetryProtocol" = "1.8.1"
    "prometheus-net.AspNetCore" = "8.2.1"
    "StackExchange.Redis" = "2.7.33"
}

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  Update Package Versions" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$updatedCount = 0
$filesChanged = 0

Get-ChildItem -Path $unitsRoot -Recurse -Filter "*.csproj" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $originalContent = $content
    $changed = $false

    foreach ($package in $packageVersions.Keys) {
        $version = $packageVersions[$package]
        # Match PackageReference with any version and update to canonical version
        $pattern = "(<PackageReference\s+Include=`"$([regex]::Escape($package))`"\s+Version=)`"[^`"]+`""
        $replacement = "`$1`"$version`""

        $newContent = $content -replace $pattern, $replacement
        if ($newContent -ne $content) {
            $content = $newContent
            $changed = $true
            $updatedCount++
        }
    }

    if ($changed) {
        Set-Content -Path $_.FullName -Value $content -NoNewline
        Write-Host "  UPDATED $($_.Name)" -ForegroundColor Green
        $filesChanged++
    }
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Updated $updatedCount package references in $filesChanged files"
