# Načtení dat z JSON souboru
$jsonData = Get-Content -Path "connect_credentials.json" | ConvertFrom-Json

# Nastavení proměnných z JSON dat
$MacName = $jsonData.mac_mini.hostname
$User = $jsonData.mac_mini.username
$Password = $jsonData.mac_mini.password
$LocalPort = $jsonData.mac_mini.local_port
$RemotePort = $jsonData.mac_mini.remote_port

# Přidali jsme parametr -t a -N pro udržení spojení
Start-Process plink -ArgumentList "-batch -t -pw $Password -L ${LocalPort}:127.0.0.1:${RemotePort} ${User}@${MacName} -N" -WindowStyle Hidden