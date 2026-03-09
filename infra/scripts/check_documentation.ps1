# check_documentation.ps1
# Checks all units in /units folder for documentation and extracts Helm values

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$unitsPath = Join-Path $scriptPath "units"
$outputPath = Join-Path $scriptPath "docs_overview.md"

# Initialize output
$output = @()
$output += "# Documentation Overview"
$output += ""
$output += "| Unit Name | Documentation | Helm-UnitId | Helm-Env-Name |"
$output += "|-----------|---------------|-------------|---------------|"

# Get all unit folders
$unitFolders = Get-ChildItem -Path $unitsPath -Directory | Where-Object { $_.Name -like "unit-*" } | Sort-Object Name

foreach ($unit in $unitFolders) {
    $unitName = $unit.Name

    # Check for README.md
    $readmePath = Join-Path $unit.FullName "README.md"
    $hasDocumentation = Test-Path $readmePath
    $documentationStatus = if ($hasDocumentation) { "true" } else { "false" }

    # Read Helm values
    $helmValuesPath = Join-Path $unit.FullName "helm\values.yaml"
    $unitId = "N/A"
    $envName = "N/A"

    if (Test-Path $helmValuesPath) {
        $content = Get-Content $helmValuesPath -Raw -Encoding UTF8

        # Extract unitId
        if ($content -match 'unitId:\s*"?([^"\r\n]+)"?') {
            $unitId = $Matches[1].Trim()
        }

        # Extract Unit__Name from env section
        if ($content -match 'name:\s*Unit__Name\s*[\r\n]+\s*value:\s*"?([^"\r\n]+)"?') {
            $envName = $Matches[1].Trim()
        }
    }

    $output += "| $unitName | $documentationStatus | $unitId | $envName |"
}

# Add summary
$totalUnits = $unitFolders.Count
$documentedUnits = ($unitFolders | Where-Object { Test-Path (Join-Path $_.FullName "README.md") }).Count
$undocumentedUnits = $totalUnits - $documentedUnits

$output += ""
$output += "---"
$output += ""
$output += "## Summary"
$output += ""
$output += "- **Total Units:** $totalUnits"
$output += "- **Documented:** $documentedUnits"
$output += "- **Missing Documentation:** $undocumentedUnits"
$output += "- **Documentation Coverage:** $([math]::Round(($documentedUnits / $totalUnits) * 100, 1))%"
$output += ""
$output += "*Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')*"

# Write output file
$output | Out-File -FilePath $outputPath -Encoding UTF8

Write-Host "Documentation overview generated: $outputPath"
Write-Host "Total units: $totalUnits, Documented: $documentedUnits, Missing: $undocumentedUnits"
