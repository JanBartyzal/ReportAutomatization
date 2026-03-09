$ErrorActionPreference = "Continue"

$unitsRoot = "i:\Codes\work\CloudInfraMap\microservices\units"
$units = Get-ChildItem -Path $unitsRoot -Directory -Filter "unit-*"
$missingTests = [System.Collections.Generic.List[string]]::new()
$lowCoverage = [System.Collections.Generic.List[PSCustomObject]]::new()

Write-Host "Checking unit tests and coverage for all units in $unitsRoot..." -ForegroundColor Cyan

foreach ($unit in $units) {
    # Skip python or node.js units just like test_units.ps1
    $unitSrcDir = Join-Path $unit.FullName "src"
    $unitIsPython = (Test-Path (Join-Path $unitSrcDir "main.py")) -or
    ($null -ne (Get-ChildItem -Path $unit.FullName -Filter "main.py" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1))
    $unitIsNodeJs = (Test-Path (Join-Path $unitSrcDir "package.json")) -or
    ($null -ne (Get-ChildItem -Path $unit.FullName -Filter "package.json" -Recurse -Depth 3 -ErrorAction SilentlyContinue |
        Where-Object { $_.DirectoryName -notmatch "[/\\]node_modules$" } | Select-Object -First 1))

    if ($unitIsPython -or $unitIsNodeJs) {
        continue
    }

    # Find test project
    $testCsproj = $null
    $testsDir = Join-Path $unit.FullName "tests"
    if (Test-Path $testsDir) {
        $testCsproj = Get-ChildItem -Path $testsDir -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "\.Tests?\.csproj$" -or $_.DirectoryName -match "[/\\]tests?$" } |
        Select-Object -First 1
        if (-not $testCsproj) {
            $testCsproj = Get-ChildItem -Path $testsDir -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1
        }
    }
    if (-not $testCsproj) {
        $testCsproj = Get-ChildItem -Path $unit.FullName -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "\.Tests?\.csproj$" } |
        Select-Object -First 1
    }

    if (-not $testCsproj) {
        $missingTests.Add($unit.Name)
        Write-Host "$($unit.Name) -> Missing Tests" -ForegroundColor Red
        continue
    }

    # Run tests with coverage
    $testResultsDir = Join-Path $testCsproj.DirectoryName "TestResults"
    if (Test-Path $testResultsDir) { Remove-Item -Path $testResultsDir -Recurse -Force -ErrorAction SilentlyContinue }

    # Run dotnet test in the project directory
    $process = Start-Process -FilePath "dotnet" -ArgumentList "test", "`"$($testCsproj.FullName)`"", "--collect:`"XPlat Code Coverage`"", "--results-directory", "`"$testResultsDir`"", "--logger:console;verbosity=quiet" -Wait -NoNewWindow -PassThru -WorkingDirectory $testCsproj.DirectoryName
    
    $covFiles = Get-ChildItem -Path $testResultsDir -Filter "coverage.cobertura.xml" -Recurse -ErrorAction SilentlyContinue
    if (-not $covFiles -or $covFiles.Count -eq 0) {
        # Possibly a test project without coverlet added, meaning coverage couldn't be collected, counts as 0%
        Write-Host "$($unit.Name) -> No coverage file generated (Considered 0%)" -ForegroundColor Yellow
        $lowCoverage.Add([PSCustomObject]@{ UnitName = $unit.Name; Coverage = 0.0 })
        continue
    }

    $covFile = $covFiles[0]
    try {
        [xml]$xml = Get-Content $covFile.FullName
        $lineRateStr = $xml.coverage.'line-rate'
        if ([string]::IsNullOrWhiteSpace($lineRateStr)) {
            Write-Host "$($unit.Name) -> Invalid coverage file (Considered 0%)" -ForegroundColor Yellow
            $lowCoverage.Add([PSCustomObject]@{ UnitName = $unit.Name; Coverage = 0.0 })
            continue
        }
        # Parse double neutrally using invariant culture
        $lineRate = [double]::Parse($lineRateStr, [System.Globalization.CultureInfo]::InvariantCulture)
        $pct = [Math]::Round($lineRate * 100, 2)

        if ($pct -lt 50) {
            Write-Host "$($unit.Name) -> Coverage: $pct%" -ForegroundColor Yellow
            $lowCoverage.Add([PSCustomObject]@{ UnitName = $unit.Name; Coverage = $pct })
        }
        else {
            Write-Host "$($unit.Name) -> Coverage: $pct%" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "$($unit.Name) -> Error parsing config ($_) (Considered 0%)" -ForegroundColor Yellow
        $lowCoverage.Add([PSCustomObject]@{ UnitName = $unit.Name; Coverage = 0.0 })
    }
}

Write-Host "`n================================================" -ForegroundColor Cyan
Write-Host "UNIT SERVICES WITHOUT TESTS" -ForegroundColor Red
Write-Host "================================================" -ForegroundColor Cyan
foreach ($m in $missingTests) {
    Write-Host "- $m"
}

Write-Host "`n================================================" -ForegroundColor Cyan
Write-Host "UNIT SERVICES WITH COVERAGE < 50%" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
foreach ($l in $lowCoverage | Sort-Object Coverage) {
    Write-Host "- $($l.UnitName) ($($l.Coverage)%)"
}

# Output as markdown text to a file so that the assistant can read it directly.
$mdReportPath = "i:\Codes\work\CloudInfraMap\microservices\reports\coverage_report.md"
$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine("# Microservices Test Coverage Report")
[void]$sb.AppendLine("## Units Without Tests")
foreach ($m in $missingTests) {
    [void]$sb.AppendLine("- $m")
}
[void]$sb.AppendLine("## Units With Coverage < 50%")
foreach ($l in $lowCoverage | Sort-Object Coverage) {
    [void]$sb.AppendLine("- $($l.UnitName) (Coverage: $($l.Coverage)%)")
}
Out-File -FilePath $mdReportPath -InputObject $sb.ToString() -Encoding UTF8
Write-Host "Report saved to $mdReportPath" -ForegroundColor Green
