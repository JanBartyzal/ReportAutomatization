# 1. Definujeme cesty
$remotePath = "mimi@MIMI:/Users/mimi/CloudInfraMap/microservices/units/"
$localPath = "$((Get-Location).Path)\microservices\units\"
$tempPath = "$env:TEMP\MacTestsDownload"

# 2. Vytvoření/vyčištění dočasné složky
if (Test-Path $tempPath) { Remove-Item -Recurse -Force $tempPath }
New-Item -ItemType Directory -Path $tempPath | Out-Null

Write-Host "Stahuji všechny testy z Macu do dočasné složky..." -ForegroundColor Cyan

# 3. Stažení celého adresáře units z Macu, ale POUZE tests/ složek
# K tomu použijeme scp, ale na Macu nejdřív provedeme find a tar pro efektivní přenos jen testů
$tarCommand = "cd /Users/mimi/CloudInfraMap/microservices/units && find . -type d -name 'tests' -o -type f -path '*/tests/*' | tar -cf tests_archive.tar -T -"
ssh mimi@MIMI $tarCommand

Write-Host "Přenáším archiv přes SCP..." -ForegroundColor Cyan
scp "mimi@MIMI:/Users/mimi/CloudInfraMap/microservices/units/tests_archive.tar" "$tempPath\tests_archive.tar"

# Úklid na Macu
ssh mimi@MIMI "rm /Users/mimi/CloudInfraMap/microservices/units/tests_archive.tar"

Write-Host "Rozbaluji testy lokálně..." -ForegroundColor Cyan
Set-Location $tempPath
tar -xf tests_archive.tar

# 4. Inkrementální kopírování z TEMP do cílového pracovního adresáře PC
$skippedUnits = @()

$tempUnitsDir = $tempPath
$extractedTestFiles = Get-ChildItem -Path $tempUnitsDir -Recurse -File | Where-Object { $_.Name -ne "tests_archive.tar" }

Write-Host "Porovnávám a kopíruji testy..." -ForegroundColor Cyan

foreach ($tempFile in $extractedTestFiles) {
    # Relativní cesta souboru (např. "unit-3pao-prep\tests\MyTest.cs")
    $relativePath = $tempFile.FullName.Substring($tempUnitsDir.Length + 1)
    $targetFile = Join-Path $localPath $relativePath
    $targetFolder = Split-Path $targetFile
    
    # Rozpoznání názvu unity (první složka v cestě)
    $unitName = ($relativePath -split '\\|/')[0]

    # Zajištění existence cílové složky
    if (!(Test-Path $targetFolder)) {
        New-Item -ItemType Directory -Path $targetFolder -Force | Out-Null
    }

    # Zjistit, jestli má smysl přepisovat
    if (Test-Path $targetFile) {
        $localFile = Get-Item $targetFile
        # Přepíšeme pouze, pokud je soubor z Macu novější
        if ($tempFile.LastWriteTime -gt $localFile.LastWriteTime) {
            Copy-Item $tempFile.FullName -Destination $targetFile -Force
            Write-Host "Aktualizováno (novější z Macu): $relativePath" -ForegroundColor Green
        }
        else {
            if ($skippedUnits -notcontains $unitName) {
                $skippedUnits += $unitName
            }
        }
    }
    else {
        # Lokálně tento test ještě neexistuje, zkopírovat vždy
        Copy-Item $tempFile.FullName -Destination $targetFile
        Write-Host "Přidáno (nové z Macu): $relativePath" -ForegroundColor Yellow
    }
}

# 5. Vypsání celkového infa
Write-Host "`n============== SOUHRN ==============" -ForegroundColor Cyan
if ($skippedUnits.Count -gt 0) {
    Write-Host "Tyto unity obsahovaly testy, které JSOU NA PC NOVEJŠÍ (a tedy přeskočeny):" -ForegroundColor DarkGray
    foreach ($su in $skippedUnits | Sort-Object) {
        Write-Host " - $su" -ForegroundColor DarkGray
    }
}
else {
    Write-Host "Všechny stažené testy byly nakopírovány." -ForegroundColor Green
}

# Úklid
Remove-Item -Recurse -Force $tempPath
Write-Host "Dokončeno." -ForegroundColor Green
