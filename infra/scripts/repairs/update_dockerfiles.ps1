#Requires -Version 5.1
<#
.SYNOPSIS
    Update Dockerfiles to use BaseUnit as ProjectReference

.DESCRIPTION
    Updates all unit Dockerfiles to:
    1. Copy base-unit as a project reference instead of just Base/ files
    2. Maintain correct directory structure for relative paths
#>
param(
    [switch]$DryRun
)

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$unitsRoot = Join-Path $scriptRoot "units"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  Update Dockerfiles for BaseUnit" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Mode:   $(if ($DryRun) { 'DRY RUN' } else { 'LIVE' })"
Write-Host ""

$updatedCount = 0

Get-ChildItem -Path $unitsRoot -Directory -Filter "unit-*" | ForEach-Object {
    $unitName = $_.Name
    $dockerfile = Join-Path $_.FullName "Dockerfile"

    if (-not (Test-Path $dockerfile)) {
        return
    }

    $content = Get-Content $dockerfile -Raw

    # Check if it uses the old Base/ copy pattern
    if ($content -match 'COPY packages/dotnet-base/') {
        Write-Host "  UPDATE $unitName" -ForegroundColor White

        # Replace old pattern with new project-based structure
        $newContent = $content

        # Update WORKDIR and COPY commands for proper project structure
        # Old: WORKDIR /src, copy Base/ to /src/Base/
        # New: WORKDIR /src/units/unit-xxx/src, copy base-unit/ to /src/base-unit/

        # Replace base-unit/Base/ copy with packages/dotnet-base/ project copy
        $newContent = $newContent -replace `
            'COPY packages/dotnet-base/ /src/Base/', `
            "COPY packages/dotnet-base/ /src/dotnet-base/"

        # Update protos path to match new structure
        $newContent = $newContent -replace `
            'COPY packages/protos/ /protos/', `
            "COPY packages/protos/ /src/protos/"

        # Update unit copy to maintain proper relative paths
        # The csproj has: Include="$(DotnetBaseProject)"
        # So from /src/units/unit-xxx/src/ we need /src/dotnet-base/
        $newContent = $newContent -replace `
            "COPY packages/units/$unitName/src/\*\.csproj \./", `
            "COPY packages/units/$unitName/src/*.csproj /src/units/$unitName/src/"

        $newContent = $newContent -replace `
            "COPY packages/units/$unitName/src/ \./", `
            "COPY packages/units/$unitName/src/ /src/units/$unitName/src/"

        # Update WORKDIR
        $newContent = $newContent -replace `
            'WORKDIR /src\r?\n', `
            "WORKDIR /src/units/$unitName/src`n"

        if (-not $DryRun) {
            Set-Content -Path $dockerfile -Value $newContent -NoNewline
        }

        Write-Host "        Updated Dockerfile" -ForegroundColor Green
        $updatedCount++
    }
    else {
        Write-Host "  SKIP  $unitName (already updated or different pattern)" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "Updated $updatedCount Dockerfiles"

if ($DryRun) {
    Write-Host ""
    Write-Host "This was a DRY RUN. No changes were made." -ForegroundColor Yellow
}
