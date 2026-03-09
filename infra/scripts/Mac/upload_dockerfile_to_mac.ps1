# 1. Vytvoříme dočasnou složku pro export
$exportDir = "$env:TEMP\DockerfileExport"
if (Test-Path $exportDir) { Remove-Item -Recurse -Force $exportDir }
New-Item -ItemType Directory -Path $exportDir


Get-ChildItem -Filter Dockerfile -Recurse | ForEach-Object {
    $targetFile = $_.FullName.Replace((Get-Location).Path, $exportDir)
    $targetFolder = Split-Path $targetFile
    if (!(Test-Path $targetFolder)) { New-Item -ItemType Directory -Path $targetFolder }
    Copy-Item $_.FullName -Destination $targetFile
}

# 5. SCP přenos na Mac
scp -r "$exportDir\*" mimi@MIMI:/Users/mimi/CloudInfraMap/microservices