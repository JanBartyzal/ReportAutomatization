$unitsRoot = "i:\Codes\work\CloudInfraMap\microservices\units"
$units = Get-ChildItem -Path $unitsRoot -Directory -Filter "unit-*"
$missingTests = [System.Collections.Generic.List[string]]::new()
$lowCoverage = [System.Collections.Generic.List[PSCustomObject]]::new()

foreach ($unit in $units) {
    $unitSrcDir = Join-Path $unit.FullName "src"
    $unitIsPython = (Test-Path (Join-Path $unitSrcDir "main.py")) -or ($null -ne (Get-ChildItem -Path $unit.FullName -Filter "main.py" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1))
    $unitIsNodeJs = (Test-Path (Join-Path $unitSrcDir "package.json")) -or ($null -ne (Get-ChildItem -Path $unit.FullName -Filter "package.json" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Where-Object { $_.DirectoryName -notmatch "[/\\]node_modules$" } | Select-Object -First 1))

    if ($unitIsPython -or $unitIsNodeJs) { continue }

    $testCsproj = $null
    $testsDir = Join-Path $unit.FullName "tests"
    if (Test-Path $testsDir) {
        $testCsproj = Get-ChildItem -Path $testsDir -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.Name -match "\.Tests?\.csproj$" -or $_.DirectoryName -match "[/\\]tests?$" } | Select-Object -First 1
        if (-not $testCsproj) { $testCsproj = Get-ChildItem -Path $testsDir -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 }
    }
    if (-not $testCsproj) {
        $testCsproj = Get-ChildItem -Path $unit.FullName -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.Name -match "\.Tests?\.csproj$" } | Select-Object -First 1
    }

    if (-not $testCsproj) {
        $missingTests.Add($unit.Name)
        continue
    }

    $testResultsDir = Join-Path (Split-Path $testCsproj.FullName) "TestResults"
    $covFiles = Get-ChildItem -Path $testResultsDir -Filter "coverage.cobertura.xml" -Recurse -ErrorAction SilentlyContinue

    if (-not $covFiles -or $covFiles.Count -eq 0) {
        $lowCoverage.Add([PSCustomObject]@{ UnitName = $unit.Name; Coverage = 0.0 })
        continue
    }

    try {
        [xml]$xml = Get-Content $covFiles[0].FullName -ErrorAction Ignore
        $lineRateStr = $xml.coverage.'line-rate'
        if ([string]::IsNullOrWhiteSpace($lineRateStr)) {
            $lowCoverage.Add([PSCustomObject]@{ UnitName = $unit.Name; Coverage = 0.0 })
            continue
        }
        $lineRate = [double]::Parse($lineRateStr, [System.Globalization.CultureInfo]::InvariantCulture)
        $pct = [Math]::Round($lineRate * 100, 2)
        if ($pct -lt 50) {
            $lowCoverage.Add([PSCustomObject]@{ UnitName = $unit.Name; Coverage = $pct })
        }
    }
    catch {
        $lowCoverage.Add([PSCustomObject]@{ UnitName = $unit.Name; Coverage = 0.0 })
    }
}

$mdReportPath = "i:\Codes\work\CloudInfraMap\microservices\reports\coverage_report.md"
$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine("# Microservices Test Coverage Report")
[void]$sb.AppendLine("## Units Without Tests")
foreach ($m in $missingTests | Sort-Object) {
    [void]$sb.AppendLine("- $m")
}
[void]$sb.AppendLine("## Units With Coverage < 50%")
foreach ($l in $lowCoverage | Sort-Object Coverage) {
    [void]$sb.AppendLine("- $($l.UnitName) (Coverage: $($l.Coverage)%)")
}
Out-File -FilePath $mdReportPath -InputObject $sb.ToString() -Encoding UTF8
Write-Host "Done"
