$ErrorActionPreference = "Continue"

$unitsRoot = "i:\Codes\work\CloudInfraMap\microservices\units"
$units = Get-ChildItem -Path $unitsRoot -Directory -Filter "unit-*"
$missingTests = [System.Collections.Generic.List[string]]::new()
$completedResults = [System.Collections.Generic.List[PSCustomObject]]::new()

Write-Host "Checking unit tests and coverage for all units in $unitsRoot in parallel..." -ForegroundColor Cyan

# Prepare pool
$pool = [runspacefactory]::CreateRunspacePool(1, 12)
$pool.Open()
$runspaces = [System.Collections.Generic.List[object]]::new()

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

    $ps = [powershell]::Create().AddScript({
            param($unitName, $testCsprojPath)
            $testResultsDir = Join-Path (Split-Path $testCsprojPath) "TestResults"
            if (Test-Path $testResultsDir) { Remove-Item -Path $testResultsDir -Recurse -Force -ErrorAction SilentlyContinue }
        
            # We try without build first to speed up, but that requires compiled DLLs. Let's just run dotnet test normally but capture all parallel
            $process = Start-Process -FilePath "dotnet" -ArgumentList "test", "`"$testCsprojPath`"", "--collect:`"XPlat Code Coverage`"", "--results-directory", "`"$testResultsDir`"", "--logger:console;verbosity=quiet" -Wait -NoNewWindow -PassThru -WorkingDirectory (Split-Path $testCsprojPath)
        
            $covFiles = Get-ChildItem -Path $testResultsDir -Filter "coverage.cobertura.xml" -Recurse -ErrorAction SilentlyContinue
            if (-not $covFiles -or $covFiles.Count -eq 0) {
                return [PSCustomObject]@{ Unit = $unitName; Coverage = 0.0; Status = "No coverage generated" }
            }
        
            try {
                [xml]$xml = Get-Content $covFiles[0].FullName
                $lineRateStr = $xml.coverage.'line-rate'
                if ([string]::IsNullOrWhiteSpace($lineRateStr)) {
                    return [PSCustomObject]@{ Unit = $unitName; Coverage = 0.0; Status = "Invalid XML" }
                }
                $lineRate = [double]::Parse($lineRateStr, [System.Globalization.CultureInfo]::InvariantCulture)
                return [PSCustomObject]@{ Unit = $unitName; Coverage = [Math]::Round($lineRate * 100, 2); Status = "OK" }
            }
            catch {
                return [PSCustomObject]@{ Unit = $unitName; Coverage = 0.0; Status = "Parse Error" }
            }
        }).AddArgument($unit.Name).AddArgument($testCsproj.FullName)
    
    $ps.RunspacePool = $pool
    $runspaces.Add([PSCustomObject]@{ UnitName = $unit.Name; Pipe = $ps; Handle = $ps.BeginInvoke() })
}

# Monitor progress
Write-Host "Started $($runspaces.Count) parallel test jobs."
$total = $runspaces.Count
$done = 0

while ($done -lt $total) {
    Start-Sleep -Seconds 5
    $currentDone = ($runspaces | Where-Object { $_.Handle.IsCompleted }).Count
    if ($currentDone -gt $done) {
        $done = $currentDone
        Write-Host "Progress: $done / $total completed..."
    }
}

$lowCoverage = [System.Collections.Generic.List[PSCustomObject]]::new()
foreach ($rs in $runspaces) {
    try {
        $result = $rs.Pipe.EndInvoke($rs.Handle)
        if ($result -and $result.Count -gt 0) {
            $res = $result[0]
            if ($res.Coverage -lt 50) {
                $lowCoverage.Add([PSCustomObject]@{ UnitName = $res.Unit; Coverage = $res.Coverage })
                Write-Host "$($res.Unit) -> Coverage: $($res.Coverage)% ($($res.Status))" -ForegroundColor Yellow
            }
            else {
                Write-Host "$($res.Unit) -> Coverage: $($res.Coverage)%" -ForegroundColor Green
            }
        }
    }
    catch {
        Write-Host "Error retrieving result for $($rs.UnitName): $_" -ForegroundColor Red
    }
    finally {
        $rs.Pipe.Dispose()
    }
}

$pool.Dispose()

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
Write-Host "Report saved to $mdReportPath" -ForegroundColor Green
