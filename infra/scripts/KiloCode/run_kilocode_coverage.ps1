param(
    [string]$Model = "kilo/z-ai/glm-5:free",
    [string]$StateFile = "processed_units.txt"
)

$scriptPath = $MyInvocation.MyCommand.Path
$scriptDir = Split-Path $scriptPath
$unitsDir = Join-Path $scriptDir "units"
$stateFilePath = Join-Path $scriptDir $StateFile

$processedUnits = @()
if (Test-Path $stateFilePath) {
    $processedUnits = Get-Content -Path $stateFilePath
}

$units = Get-ChildItem -Path $unitsDir -Directory -Filter "unit-*" | Sort-Object Name

foreach ($unit in $units) {
    $unitName = $unit.Name
    
    if ($processedUnits -contains $unitName) {
        Write-Host "Přeskakuji $unitName (již zpracováno)." -ForegroundColor DarkGray
        continue
    }

    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Processing $unitName..." -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    
    $prompt = "Aktuální testy v units/$unitName mají kriticky nízkou coverage (často pod 2%). Cílem je dosáhnout test coverage >90%. Vytvoř nebo doplň chybějící unit testy pro $unitName (s využitím xUnit a Moq), které pokryjí maximální možné množství logiky. Vygeneruj skutečný funkční kód testů a ujisti se, že projdou."
    
    # Spuštění kilocode
    Write-Host "Executing kilocode for $unitName" -ForegroundColor Yellow
    kilocode run -m $Model "$prompt"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "kilocode command returned non-zero exit code for $unitName."
    }
    
    # Uložení informace o zpracování dané unity do txt souboru
    Add-Content -Path $stateFilePath -Value $unitName
}

Write-Host "Dokončeno." -ForegroundColor Green
