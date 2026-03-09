# 1. Vytvoříme dočasnou složku pro export
$exportDir = "$env:TEMP\CsprojExport"
if (Test-Path $exportDir) { Remove-Item -Recurse -Force $exportDir }
New-Item -ItemType Directory -Path $exportDir

# 2. Kopírujeme pouze .csproj soubory se zachováním struktury
Get-ChildItem -Filter *.csproj -Recurse | ForEach-Object {
    $targetFile = $_.FullName.Replace((Get-Location).Path, $exportDir)
    $targetFolder = Split-Path $targetFile
    if (!(Test-Path $targetFolder)) { New-Item -ItemType Directory -Path $targetFolder }
    Copy-Item $_.FullName -Destination $targetFile
}

# 3. Kopírujeme i soubory řešení .sln (pokud jsou potřeba)
Get-ChildItem -Filter *.sln -Recurse | ForEach-Object {
    $targetFile = $_.FullName.Replace((Get-Location).Path, $exportDir)
    Copy-Item $_.FullName -Destination $targetFile
}

# 4. Kopírujeme zdrojové soubory .cs (vylučujeme bin a obj)
Get-ChildItem -Filter *.cs -Recurse | Where-Object { 
    $_.FullName -notmatch '\\bin\\' -and $_.FullName -notmatch '\\obj\\' 
} | ForEach-Object {
    $targetFile = $_.FullName.Replace((Get-Location).Path, $exportDir)
    $targetFolder = Split-Path $targetFile
    
    # Ujistíme se, že cílová složka existuje (pro případ, že v exportu ještě není)
    if (!(Test-Path $targetFolder)) { New-Item -ItemType Directory -Path $targetFolder -Force }
    
    Copy-Item $_.FullName -Destination $targetFile
}

# 5. SCP přenos na Mac
scp -r "$exportDir\*" mimi@MIMI:/Users/mimi/CloudInfraMap/microservices/