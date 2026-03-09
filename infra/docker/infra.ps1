
param(   
    [string]$Product = "0" 
)

if ($Product -eq "0") {
    docker compose up db-platform db-business redis -d
}

if ($Product -eq "1") {
    docker compose up azurite pgadmin gateway prometheus grafana -d
}
if ($Product -eq "2") {
    docker compose up frontend -d
 
}

if ($Product -eq "3") {
 
    docker compose up cim-landing suite-landing -d
}

if ($Product -eq "4") {
 
    docker compose up pulse-landing guard-landing arch-landing -d
}

if ($Product -eq "9") {
 
    docker compose down
}

