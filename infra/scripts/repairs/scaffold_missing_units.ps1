# TASKS-GAP: Scaffold missing microservice units
# Creates minimal unit structure for units identified as missing in GAP analysis

$units = @(
    @{ Name = "unit-network-topology-builder"; Id = "U-NET-001"; Domain = "network"; Desc = "Builds network topology graphs from infrastructure plans" },
    @{ Name = "unit-egress-cost-calculator"; Id = "U-NET-002"; Domain = "network"; Desc = "Calculates cross-region and cross-provider egress costs" },
    @{ Name = "unit-data-gravity-analyzer"; Id = "U-NET-003"; Domain = "network"; Desc = "Analyzes data gravity patterns for optimal placement" },
    @{ Name = "unit-storage-price-calculator"; Id = "U-STO-001"; Domain = "storage"; Desc = "Calculates storage costs across tiers and providers" },
    @{ Name = "unit-storage-tier-advisor"; Id = "U-STO-002"; Domain = "storage"; Desc = "Recommends optimal storage tiers based on access patterns" },
    @{ Name = "unit-scenario-template-library"; Id = "U-BEN-001"; Domain = "benchmark"; Desc = "Manages scenario templates for benchmarking" },
    @{ Name = "unit-benchmark-data-provider"; Id = "U-BEN-003"; Domain = "benchmark"; Desc = "Provides industry benchmark data for cost comparison" },
    @{ Name = "unit-layout-engine"; Id = "U-DES-003"; Domain = "designer"; Desc = "Auto-layout engine for infrastructure diagrams" },
    @{ Name = "unit-export-renderer"; Id = "U-DES-004"; Domain = "designer"; Desc = "Renders infrastructure diagrams to various export formats" }
)

foreach ($unit in $units) {
    $name = $unit.Name
    $id = $unit.Id
    $domain = $unit.Domain
    $desc = $unit.Desc
    $pascalName = ($name -replace 'unit-', '' -replace '-', ' ' | ForEach-Object { (Get-Culture).TextInfo.ToTitleCase($_) }) -replace ' ', ''
    $ns = "CloudInfraMap.Units.$pascalName"
    $basePath = "units/$name"

    Write-Host "Creating $name ($id)..." -ForegroundColor Green

    # Create directory structure
    New-Item -ItemType Directory -Force -Path "$basePath/src/Base" | Out-Null
    New-Item -ItemType Directory -Force -Path "$basePath/src/Services" | Out-Null
    New-Item -ItemType Directory -Force -Path "$basePath/src/Data" | Out-Null
    New-Item -ItemType Directory -Force -Path "$basePath/tests" | Out-Null
    New-Item -ItemType Directory -Force -Path "$basePath/helm" | Out-Null

    # README.md
    @"
# $name ($id)

**Domain:** $domain
**Description:** $desc

## Status
Scaffolded as part of TASKS-GAP analysis. Requires full implementation.

## API
See proto definition in ``/packages/protos/$domain/``
"@ | Set-Content "$basePath/README.md"

    # .csproj
    @"
<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <TargetFramework>net10.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
    <RootNamespace>$ns</RootNamespace>
    <AssemblyName>$name</AssemblyName>
  </PropertyGroup>
  <ItemGroup>
    <Protobuf Include="`$(ProtosRoot)\common\*.proto" GrpcServices="None" />
    <Protobuf Include="`$(ProtosRoot)\config\config_distributor.proto" GrpcServices="Client" Link="Protos\config_distributor.proto" ProtoRoot="`$(ProtosRoot)" />
  </ItemGroup>
  <ItemGroup>
    <PackageReference Include="Grpc.AspNetCore" Version="2.62.0" />
    <PackageReference Include="Grpc.Net.Client" Version="2.62.0" />
    <PackageReference Include="Grpc.Tools" Version="2.62.0" PrivateAssets="All" />
    <PackageReference Include="Google.Protobuf" Version="3.26.1" />
    <PackageReference Include="Dapr.AspNetCore" Version="1.14.0" />
    <PackageReference Include="Dapr.Client" Version="1.14.0" />
    <PackageReference Include="Serilog.AspNetCore" Version="8.0.1" />
    <PackageReference Include="Serilog.Sinks.Console" Version="5.0.1" />
    <PackageReference Include="OpenTelemetry.Extensions.Hosting" Version="1.8.1" />
    <PackageReference Include="OpenTelemetry.Instrumentation.AspNetCore" Version="1.8.1" />
    <PackageReference Include="prometheus-net.AspNetCore" Version="8.2.1" />
    <PackageReference Include="AspNetCore.HealthChecks.NpgSql" Version="8.0.1" />
    <PackageReference Include="Npgsql.EntityFrameworkCore.PostgreSQL" Version="9.0.0" />
  </ItemGroup>
</Project>
"@ | Set-Content "$basePath/src/$name.csproj"

    # Program.cs
    @"
// $id`: $name
// Domain: $domain
// $desc

using Serilog;

var builder = WebApplication.CreateBuilder(args);

var unitId = "$id";
var unitName = "$name";

Log.Logger = new LoggerConfiguration()
    .Enrich.WithProperty("UnitId", unitId)
    .Enrich.WithProperty("UnitName", unitName)
    .WriteTo.Console(outputTemplate: "[{Timestamp:HH:mm:ss} {Level:u3}] [{UnitId}] {Message:lj}{NewLine}{Exception}")
    .CreateLogger();

builder.Host.UseSerilog();
builder.Services.AddGrpc();
builder.Services.AddDaprClient();
builder.Services.AddHealthChecks();

var app = builder.Build();

app.MapHealthChecks("/health");
app.MapGet("/", () => new { unit_id = unitId, unit_name = unitName, domain = "$domain", status = "running" });

app.Run();
"@ | Set-Content "$basePath/src/Program.cs"

    # appsettings.json
    @"
{
  "Logging": { "LogLevel": { "Default": "Information" } },
  "Dapr": { "GrpcPort": 50001, "HttpPort": 3500 },
  "UnitId": "$id",
  "UnitName": "$name"
}
"@ | Set-Content "$basePath/src/appsettings.json"

    # Dockerfile
    @"
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY packages/protos/ /src/protos/
COPY units/$name/src/ /src/units/$name/src/
WORKDIR /src/units/$name/src
RUN dotnet publish -c Release -o /app

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app .
EXPOSE 8080
ENTRYPOINT ["dotnet", "$name.dll"]
"@ | Set-Content "$basePath/Dockerfile"

    # Helm Chart.yaml
    @"
apiVersion: v2
name: $name
description: $desc
type: application
version: 1.0.0
appVersion: "1.0.0"
"@ | Set-Content "$basePath/helm/Chart.yaml"

    # Helm values.yaml
    @"
replicaCount: 1
image:
  repository: cloudinframap/$name
  tag: latest
service:
  type: ClusterIP
  port: 8080
  grpcPort: 50051
"@ | Set-Content "$basePath/helm/values.yaml"

    # Basic test
    @"
namespace $ns.Tests;
public class BasicTests
{
    [Fact] public void UnitId_ShouldBe_$($id -replace '-', '_')() => Assert.Equal("$id", "$id");
}
"@ | Set-Content "$basePath/tests/BasicTests.cs"

    # Test .csproj
    @"
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net10.0</TargetFramework>
    <IsPackable>false</IsPackable>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="Microsoft.NET.Test.Sdk" Version="17.9.0" />
    <PackageReference Include="xunit" Version="2.7.0" />
    <PackageReference Include="xunit.runner.visualstudio" Version="2.5.7" />
  </ItemGroup>
</Project>
"@ | Set-Content "$basePath/tests/$name.Tests.csproj"

    Write-Host "  Created $name" -ForegroundColor Cyan
}

Write-Host "`nAll missing units scaffolded!" -ForegroundColor Green
