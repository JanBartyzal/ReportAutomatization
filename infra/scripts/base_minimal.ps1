# base_minimal.ps1
param(
    [switch]$OverwriteDockerfiles = $false
)
# Skript pro generovani Helm chart souboru a Dockerfile do vsech mikrosluzeb v /units

# Definice cest
$microservicesDir = $PSScriptRoot
$unitsDir = Join-Path $microservicesDir "units"

# Kontrola existence slozky units
if (-not (Test-Path $unitsDir)) {
    Write-Error "Slozka units nebyla nalezena: $unitsDir"
    exit 1
}

# Funkce pro detekci typu projektu
function Get-ProjectType {
    param([string]$UnitPath)
    
    $srcPath = Join-Path $UnitPath "src"
    
    # Kontrola na Python projekt (main.py)
    if (Test-Path (Join-Path $srcPath "main.py")) {
        return "Python"
    }
    
    # Kontrola na Node.js projekt (package.json)
    if (Test-Path (Join-Path $srcPath "package.json")) {
        return "NodeJS"
    }
    
    # Kontrola na .NET projekt (*.csproj)
    $csproj = Get-ChildItem -Path $srcPath -Filter "*.csproj" -ErrorAction SilentlyContinue | 
    Where-Object { $_.Name -notmatch "\.Tests?\.csproj$" } | 
    Select-Object -First 1
    if ($csproj) {
        return "DotNet"
    }
    
    return "Unknown"
}

# Funkce pro generovani .NET Dockerfile
function New-DotNetDockerfile {
    param([string]$UnitName, [string]$FilePath)
    
    $content = @"
# $UnitName - .NET 10 Microservice

FROM mcr.microsoft.com/dotnet/sdk:10.0 AS builder

WORKDIR /src

# Copy proto files
COPY packages/protos/ /src/protos/

# Copy shared Base library
COPY packages/dotnet-base/ /src/Base/

# Copy project file and code
COPY microservices/units/$UnitName/src/*.csproj ./
COPY microservices/units/$UnitName/src/ ./

# Build and publish in one go (more robust for gRPC tools)
RUN dotnet publish -c Release -o /app -r linux-musl-x64 --no-self-contained

# Runtime stage
FROM mcr.microsoft.com/dotnet/aspnet:10.0-alpine

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /app .

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

USER appuser

ENV ASPNETCORE_URLS=http://+:8080
ENV ASPNETCORE_ENVIRONMENT=Production

EXPOSE 8080

ENTRYPOINT ["dotnet", "$UnitName.dll"]
"@
    
    $content | Out-File -FilePath $FilePath -Encoding UTF8 -NoNewline
}

# Funkce pro generovani Python Dockerfile
function New-PythonDockerfile {
    param([string]$UnitName, [string]$FilePath)
    
    $content = @"
# $UnitName - Python Microservice

FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Copy shared package
COPY microservices/shared/python/cloudinframap_focus /tmp/cloudinframap_focus
RUN pip install /tmp/cloudinframap_focus

# Copy generated protos
COPY packages/protos /tmp/protos
RUN pip install grpcio-tools==1.62.0

# Create generated directory
RUN mkdir -p /app/src/generated

# Generate protos
RUN python -m grpc_tools.protoc \
    -I /tmp/protos \
    --python_out=/app/src/generated \
    --grpc_python_out=/app/src/generated \
    /tmp/protos/common/*.proto \
    /tmp/protos/parser/*.proto

# Touch __init__.py in generated directories
RUN touch /app/src/generated/__init__.py

# Copy application code
COPY microservices/units/$UnitName/src /app/src

# Install app dependencies
COPY microservices/units/$UnitName/src/requirements.txt /app/requirements.txt
RUN pip install -r /app/requirements.txt

# Set Python path
ENV PYTHONPATH=/app/src:/app/src/generated

# Run service
CMD ["python", "src/main.py"]
"@
    
    $content | Out-File -FilePath $FilePath -Encoding UTF8 -NoNewline
}

# Funkce pro generovani Node.js Dockerfile
function New-NodeJSDockerfile {
    param([string]$UnitName, [string]$FilePath)
    
    $content = @"
# $UnitName - Node.js/React Frontend

FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies
COPY microservices/units/$UnitName/src/package*.json ./
RUN npm ci

# Copy source and build
COPY microservices/units/$UnitName/src/ ./

# Build arguments for environment
ARG VITE_API_BASE_URL=/api
ARG VITE_WS_URL=/ws
ARG VITE_APP_VERSION=1.0.0

ENV VITE_API_BASE_URL=`$VITE_API_BASE_URL
ENV VITE_WS_URL=`$VITE_WS_URL
ENV VITE_APP_VERSION=`$VITE_APP_VERSION

RUN npm run build

# Production stage
FROM nginx:1.25-alpine

# Security: Run as non-root
RUN addgroup -g 1001 -S nginx-app && \
    adduser -u 1001 -S nginx-app -G nginx-app && \
    chown -R nginx-app:nginx-app /var/cache/nginx && \
    chown -R nginx-app:nginx-app /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R nginx-app:nginx-app /var/run/nginx.pid

# Copy built app
COPY --from=builder /app/dist /usr/share/nginx/html

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

USER nginx-app

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
"@
    
    $content | Out-File -FilePath $FilePath -Encoding UTF8 -NoNewline
}

# Ziskani vsech podslozek v /units
$unitFolders = Get-ChildItem -Path $unitsDir -Directory

Write-Host "Nalezeno $($unitFolders.Count) mikrosluzeb v $unitsDir" -ForegroundColor Cyan
Write-Host ""

$successCount = 0
$errorCount = 0

foreach ($unitFolder in $unitFolders) {
    $unitName = $unitFolder.Name
    Write-Host "Zpracovavam: $unitName" -ForegroundColor Yellow

    try {
        # Detekce typu projektu
        $projectType = Get-ProjectType -UnitPath $unitFolder.FullName
        Write-Host "  INFO Typ projektu: $projectType" -ForegroundColor Cyan

        # Kontrola a generovani Dockerfile
        $dockerfilePath = Join-Path $unitFolder.FullName "Dockerfile"
        if (-not (Test-Path $dockerfilePath) -or $OverwriteDockerfiles) {
            switch ($projectType) {
                "DotNet" {
                    New-DotNetDockerfile -UnitName $unitName -FilePath $dockerfilePath
                    Write-Host "  OK Vygenerovan: Dockerfile (.NET)" -ForegroundColor Green
                }
                "Python" {
                    New-PythonDockerfile -UnitName $unitName -FilePath $dockerfilePath
                    Write-Host "  OK Vygenerovan: Dockerfile (Python)" -ForegroundColor Green
                }
                "NodeJS" {
                    New-NodeJSDockerfile -UnitName $unitName -FilePath $dockerfilePath
                    Write-Host "  OK Vygenerovan: Dockerfile (Node.js)" -ForegroundColor Green
                }
                default {
                    Write-Host "  SKIP Dockerfile - neznamy typ projektu" -ForegroundColor DarkGray
                }
            }
        }
        else {
            Write-Host "  INFO Dockerfile jiz existuje (pouzijte -OverwriteDockerfiles pro prepsani)" -ForegroundColor Gray
        }

        # Vytvoreni slozky helm, pokud neexistuje
        $helmDir = Join-Path $unitFolder.FullName "helm"
        if (-not (Test-Path $helmDir)) {
            New-Item -Path $helmDir -ItemType Directory -Force | Out-Null
            Write-Host "  OK Vytvorena slozka: helm" -ForegroundColor Green
        }
        else {
            Write-Host "  INFO Slozka helm jiz existuje" -ForegroundColor Gray
        }
        
        # Vytvoreni slozky helm/templates, pokud neexistuje
        $templatesDir = Join-Path $helmDir "templates"
        if (-not (Test-Path $templatesDir)) {
            New-Item -Path $templatesDir -ItemType Directory -Force | Out-Null
            Write-Host "  OK Vytvorena slozka: helm/templates" -ForegroundColor Green
        }
        else {
            Write-Host "  INFO Slozka helm/templates jiz existuje" -ForegroundColor Gray
        }

        # Generate Helm Chart.yaml with base chart dependency
        $chartContent = @"
apiVersion: v2
name: $unitName
description: Helm chart for $unitName microservice
version: 0.1.0
appVersion: "1.0.0"
dependencies:
  - name: base-service
    version: "1.0.0"
    repository: "file://../../../charts/base-service"
"@
        $chartContent | Out-File -FilePath "$helmDir/Chart.yaml" -Encoding UTF8 -NoNewline
        Write-Host "  OK Vygenerovan: Chart.yaml (s dependency)" -ForegroundColor Green

        # Generate service-specific values.yaml
        $appId = $unitName -replace "^unit-", ""
        
        # Base values common to all services
        $valuesContent = @"
# Service-specific values for $unitName
# These override the base-service chart defaults

unitName: $unitName
unitId: "U-XXX-000"  # TODO: Set correct unit ID

image:
  repository: cloudinframap/$unitName
  tag: latest
  pullPolicy: IfNotPresent

dapr:
  enabled: true
  appId: $appId
  appPort: 8080
"@

        # Add type-specific configuration
        switch ($projectType) {
            "DotNet" {
                $valuesContent += @"

  protocol: grpc

# .NET specific resource limits
resources:
  requests:
    cpu: 50m
    memory: 128Mi
  limits:
    cpu: 500m
    memory: 512Mi

env:
  - name: ASPNETCORE_ENVIRONMENT
    value: "Production"
  - name: Unit__Name
    value: "$unitName"
"@
            }
            "Python" {
                $valuesContent += @"

  protocol: grpc

# Python specific resource limits (lighter than .NET)
resources:
  requests:
    cpu: 50m
    memory: 64Mi
  limits:
    cpu: 200m
    memory: 256Mi

env:
  - name: PYTHONUNBUFFERED
    value: "1"
  - name: Unit__Name
    value: "$unitName"
"@
            }
            "NodeJS" {
                $valuesContent += @"

  enabled: false  # Frontend services typically don't need Dapr

# Node.js/Frontend specific resource limits
resources:
  requests:
    cpu: 50m
    memory: 128Mi
  limits:
    cpu: 300m
    memory: 512Mi

service:
  type: ClusterIP
  port: 80
  targetPort: 8080

env:
  - name: NODE_ENV
    value: "production"
"@
            }
            default {
                $valuesContent += @"

  protocol: grpc

# Default resource limits
resources:
  requests:
    cpu: 50m
    memory: 64Mi
  limits:
    cpu: 200m
    memory: 256Mi
"@
            }
        }
        
        $valuesContent | Out-File -FilePath "$helmDir/values.yaml" -Encoding UTF8 -NoNewline
        Write-Host "  OK Vygenerovan: values.yaml ($projectType)" -ForegroundColor Green

        # Remove old deployment.yaml from templates (no longer needed with base chart dependency)
        $oldDeploymentPath = Join-Path $templatesDir "deployment.yaml"
        if (Test-Path $oldDeploymentPath) {
            Remove-Item $oldDeploymentPath -Force
            Write-Host "  OK Odstranen: templates/deployment.yaml (pouziva se base chart)" -ForegroundColor Green
        }
        
        $successCount++
        Write-Host ""
        
    }
    catch {
        Write-Error "  CHYBA pri zpracovani $unitName : $_"
        $errorCount++
        Write-Host ""
    }
}

# Souhrn
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SOUHRN" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Uspesne zpracovano: $successCount mikrosluzeb" -ForegroundColor Green
if ($errorCount -gt 0) {
    Write-Host "Chyby: $errorCount mikrosluzeb" -ForegroundColor Red
}
Write-Host ""
Write-Host "Hotovo!" -ForegroundColor Cyan
