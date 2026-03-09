#Requires -Version 5.1
<#
.SYNOPSIS
    Migrate units to use shared BaseUnit library

.DESCRIPTION
    This script:
    1. Adds ProjectReference to BaseUnit in all unit .csproj files
    2. Removes local Base directories from units
    3. Updates Dockerfiles to not copy Base separately

.PARAMETER DryRun
    If set, shows what would be changed without making changes

.PARAMETER SkipUnits
    Comma-separated list of units to skip

.EXAMPLE
    .\migrate_to_shared_base.ps1 -DryRun
    .\migrate_to_shared_base.ps1
#>
param(
    [switch]$DryRun,
    [string]$SkipUnits = ""
)

$ErrorActionPreference = "Stop"
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$unitsRoot = Join-Path $scriptRoot "units"
$baseUnitPath = "$(DotnetBaseProject)"

$skipList = $SkipUnits -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ }

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  Migrate to Shared BaseUnit Library" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Mode:   $(if ($DryRun) { 'DRY RUN' } else { 'LIVE' })"
Write-Host ""

$units = Get-ChildItem -Path $unitsRoot -Directory -Filter "unit-*" -ErrorAction SilentlyContinue
$migratedCount = 0
$skippedCount = 0
$errorCount = 0

foreach ($unit in $units) {
    if ($unit.Name -in $skipList) {
        Write-Host "  SKIP  $($unit.Name)" -ForegroundColor DarkGray
        $skippedCount++
        continue
    }

    # Find .csproj in src/ directory
    $srcDir = Join-Path $unit.FullName "src"
    $csproj = $null

    if (Test-Path $srcDir) {
        $csproj = Get-ChildItem -Path $srcDir -Filter "*.csproj" -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch "\.Tests?\.csproj$" } |
            Select-Object -First 1
    }

    # Fallback: check unit root for some units with different structure
    if (-not $csproj) {
        $csproj = Get-ChildItem -Path $unit.FullName -Filter "*.csproj" -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch "\.Tests?\.csproj$" -and $_.DirectoryName -eq $unit.FullName } |
            Select-Object -First 1
    }

    if (-not $csproj) {
        Write-Host "  WARN  $($unit.Name) - no .csproj found" -ForegroundColor Yellow
        continue
    }

    $csprojDir = $csproj.DirectoryName
    $baseDir = Join-Path $csprojDir "Base"
    $hasLocalBase = Test-Path $baseDir

    Write-Host "  MIGRATE $($unit.Name)" -ForegroundColor White

    try {
        # Step 1: Add ProjectReference to BaseUnit
        $content = Get-Content $csproj.FullName -Raw

        # Calculate relative path from csproj to BaseUnit
        $relativePath = $baseUnitPath
        if ($csprojDir -ne (Join-Path $unit.FullName "src")) {
            # Unit has non-standard structure, adjust path
            $relativePath = "$(DotnetBaseProject)"
        }

        if ($content -notmatch 'DotnetBaseProject') {
            # Find a good place to insert ProjectReference
            # Look for existing ItemGroup with ProjectReference or create new one

            if ($content -match '(<ItemGroup>\s*<ProjectReference)') {
                # Add to existing ProjectReference ItemGroup
                $newRef = "    <ProjectReference Include=`"$relativePath`" />`n"
                $content = $content -replace '(<ItemGroup>\s*<ProjectReference)', "$newRef`$1"
            }
            elseif ($content -match '(</Project>)') {
                # Add new ItemGroup before </Project>
                $newItemGroup = @"

  <!-- Shared Base Library -->
  <ItemGroup>
    <ProjectReference Include="$relativePath" />
  </ItemGroup>

</Project>
"@
                $content = $content -replace '</Project>\s*$', $newItemGroup
            }

            if (-not $DryRun) {
                Set-Content -Path $csproj.FullName -Value $content -NoNewline
            }
            Write-Host "        + Added ProjectReference to BaseUnit" -ForegroundColor Green
        }
        else {
            Write-Host "        ~ ProjectReference already exists" -ForegroundColor DarkGray
        }

        # Step 2: Remove local Base directory
        if ($hasLocalBase) {
            $baseFiles = Get-ChildItem -Path $baseDir -Filter "*.cs" -ErrorAction SilentlyContinue
            if ($baseFiles) {
                if (-not $DryRun) {
                    Remove-Item -Path $baseDir -Recurse -Force
                }
                Write-Host "        - Removed Base/ directory ($($baseFiles.Count) files)" -ForegroundColor Yellow
            }
        }
        else {
            Write-Host "        ~ No local Base/ directory" -ForegroundColor DarkGray
        }

        $migratedCount++
    }
    catch {
        Write-Host "        ERROR: $_" -ForegroundColor Red
        $errorCount++
    }
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Migration Summary"
Write-Host "  Migrated: $migratedCount" -ForegroundColor Green
Write-Host "  Skipped:  $skippedCount" -ForegroundColor DarkGray
if ($errorCount -gt 0) {
    Write-Host "  Errors:   $errorCount" -ForegroundColor Red
}

if ($DryRun) {
    Write-Host ""
    Write-Host "This was a DRY RUN. No changes were made." -ForegroundColor Yellow
    Write-Host "Run without -DryRun to apply changes." -ForegroundColor Yellow
}
