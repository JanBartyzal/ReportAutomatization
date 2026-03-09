# 1. Vytvoříme dočasnou složku pro export
$exportDir = "$env:TEMP\CsprojExport"
if (Test-Path $exportDir) { Remove-Item -Recurse -Force $exportDir }
New-Item -ItemType Directory -Path $exportDir

$baseDir = (Get-Location).Path

# 2. Definujeme konkrétní přípony souborů, které chceme primárně přenášet napříč projektem
$allowedExtensions = @('.csproj', '.sln', '.cs', '.json', '.yaml', '.yml', '.ps1', '.sh', '.md', '.proto', '.txt')

# Funkce pro bezpečné kopírování souboru se zachováním stromové struktury
function Copy-ProjectFile {
    param([string]$SourcePath)
    
    # Zjistíme cílovou cestu nahrazením základu umístěním exportu
    $targetFile = $SourcePath.Replace($baseDir, $exportDir)
    $targetFolder = Split-Path $targetFile
    
    if (!(Test-Path $targetFolder)) { 
        New-Item -ItemType Directory -Path $targetFolder -Force | Out-Null
    }
    
    Copy-Item $SourcePath -Destination $targetFile
}

# 3. Kopírujeme soubory podle povolených přípon, vyloučíme 'bin' a 'obj'
Get-ChildItem -Path $baseDir -Recurse -File | Where-Object { 
    $ext = $_.Extension.ToLower()
    $allowedExtensions -contains $ext -and 
    $_.FullName -notmatch '\\bin\\' -and 
    $_.FullName -notmatch '\\obj\\' -and
    $_.FullName -notmatch '\\\.git\\'
} | ForEach-Object {
    Copy-ProjectFile -SourcePath $_.FullName
}

# 4. SCP přenos na Mac do kořenového repozitáře CloudInfraMap
Write-Host "Kopíruji soubory na Mac..." -ForegroundColor Cyan
scp -r "$exportDir\*" mimi@MIMI:/Users/mimi/CloudInfraMap/