#Requires -Version 5.1
# Fix BaseUnit.csproj paths in all unit .csproj files

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$unitsRoot = Join-Path $scriptRoot "units"

$fixedCount = 0

Get-ChildItem -Path $unitsRoot -Recurse -Filter "*.csproj" | Where-Object { $_.Name -notmatch "\.Tests?\.csproj$" } | ForEach-Object {
    $content = Get-Content $_.FullName -Raw

    if ($content -match 'DotnetBaseProject') {
        $correctPath = '$(DotnetBaseProject)'

        # Replace any hardcoded relative paths that should use the MSBuild property
        $newContent = $content -replace 'Include="[^"]*DotnetBaseProject[^"]*"', "Include=`"$correctPath`""

        if ($newContent -ne $content) {
            Set-Content -Path $_.FullName -Value $newContent -NoNewline
            Write-Host "FIXED: $($_.FullName) -> $correctPath"
            $fixedCount++
        }
    }
}

Write-Host ""
Write-Host "Fixed $fixedCount files"
