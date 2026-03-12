// ReportPlatform - Main Bicep deployment for Azure Container Apps
// Orchestrates all infrastructure and microservice modules:
//   - Container Apps Environment with Dapr
//   - Infrastructure: VNet, KeyVault, PostgreSQL, Redis, Storage, ACR, Front Door
//   - Java microservices (20 services)
//   - Python microservices (8 services)
//   - Frontend (1 service)
//   - Observability: OTEL Collector
//   - Legacy: Web Gateway, GraphQL Gateway, MV Refresh Job, Cache Invalidator

targetScope = 'resourceGroup'

// ─── Parameters ──────────────────────────────────────────────────────────────

@description('Azure region for all resources')
param location string = resourceGroup().location

@description('Deployment environment')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'prod'

@description('Container image tag for all services')
param imageTag string = 'latest'

@description('Resource ID of the Log Analytics workspace')
param logAnalyticsWorkspaceId string

@description('Resource ID of the Application Insights instance')
param appInsightsId string

@description('Name of the Azure Cache for Redis instance')
param redisName string

@description('Redis primary access key')
@secure()
param redisPrimaryKey string

@description('Redis host address (host:port)')
param redisHost string = '${redisName}.redis.cache.windows.net:6380'

@description('PostgreSQL connection string')
@secure()
param postgresConnectionString string

@description('Redis connection string')
@secure()
param redisConnectionString string

@description('Azure Container Registry login server')
param acrLoginServer string = 'acrreportplatform.azurecr.io'

@description('Azure Storage connection string')
@secure()
param storageConnectionString string = ''

@description('Azure Entra ID tenant ID')
param azureTenantId string = ''

@description('Azure Entra ID client ID')
param azureClientId string = ''

// ─── Container Apps Environment ──────────────────────────────────────────────

module containerEnv 'modules/container-apps-env.bicep' = {
  name: 'container-env-${environment}'
  params: {
    location: location
    environment: environment
    logAnalyticsWorkspaceId: logAnalyticsWorkspaceId
    appInsightsId: appInsightsId
    redisName: redisName
    redisPrimaryKey: redisPrimaryKey
    zoneRedundant: environment == 'prod'
  }
}

// ─── Legacy modules (existing) ───────────────────────────────────────────────

module webGateway 'modules/web-gateway.bicep' = {
  name: 'web-gateway-${environment}'
  params: {
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    environmentDomain: containerEnv.outputs.defaultDomain
    postgresConnectionString: postgresConnectionString
    redisConnectionString: redisConnectionString
  }
}

module graphqlGateway 'modules/graphql-gateway.bicep' = {
  name: 'graphql-gateway-${environment}'
  params: {
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    redisConnectionString: redisConnectionString
  }
}

module mvRefreshJob 'modules/mv-refresh-job.bicep' = {
  name: 'mv-refresh-job-${environment}'
  params: {
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    postgresConnectionString: postgresConnectionString
  }
}

module cacheInvalidator 'modules/cache-invalidator.bicep' = {
  name: 'cache-invalidator-${environment}'
  params: {
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    redisConnectionString: redisConnectionString
    redisHost: redisHost
  }
}

// ─── Java Microservices ──────────────────────────────────────────────────────

// Common DB env vars for Java services
var javaDbEnvBase = [
  { name: 'SPRING_PROFILES_ACTIVE', value: environment }
  { name: 'DB_HOST', value: 'psql-reportplatform-${environment}.postgres.database.azure.com' }
  { name: 'DB_PORT', value: '5432' }
  { name: 'DAPR_HOST', value: 'localhost' }
  { name: 'DAPR_HTTP_PORT', value: '3500' }
  { name: 'DAPR_GRPC_PORT', value: '50001' }
]

// MS-AUTH - Authentication (always-on, external)
module msAuth 'modules/microservice.bicep' = {
  name: 'ms-auth-${environment}'
  params: {
    serviceName: 'ms-auth'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    cpu: '0.5'
    memory: '1Gi'
    minReplicas: 1
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [
      { name: 'DB_NAME', value: 'auth_db' }
      { name: 'AZURE_TENANT_ID', value: azureTenantId }
      { name: 'AZURE_CLIENT_ID', value: azureClientId }
    ])
  }
}

// MS-ING - File Ingestion (always-on)
module msIng 'modules/microservice.bicep' = {
  name: 'ms-ing-${environment}'
  params: {
    serviceName: 'ms-ing'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    cpu: '0.5'
    memory: '1Gi'
    minReplicas: 1
    maxReplicas: 10
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [
      { name: 'DB_NAME', value: 'ms_ing' }
      { name: 'AZURE_STORAGE_CONNECTION_STRING', value: storageConnectionString }
      { name: 'AZURE_STORAGE_CONTAINER', value: 'file-uploads' }
    ])
  }
}

// MS-ORCH - Workflow Orchestration (always-on, critical)
module msOrch 'modules/microservice.bicep' = {
  name: 'ms-orch-${environment}'
  params: {
    serviceName: 'ms-orch'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    cpu: '1'
    memory: '2Gi'
    minReplicas: 1
    maxReplicas: 10
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [
      { name: 'DB_NAME', value: 'orch_db' }
      { name: 'DAPR_PUBSUB_NAME', value: 'reportplatform-pubsub' }
      { name: 'DAPR_STATESTORE_NAME', value: 'reportplatform-statestore' }
      { name: 'REDIS_HOST', value: redisHost }
    ])
  }
}

// MS-SCAN - Security Scanner
module msScan 'modules/microservice.bicep' = {
  name: 'ms-scan-${environment}'
  params: {
    serviceName: 'ms-scan'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    cpu: '0.5'
    memory: '1Gi'
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [
      { name: 'AZURE_STORAGE_CONNECTION_STRING', value: storageConnectionString }
    ])
  }
}

// MS-SINK-TBL, MS-SINK-DOC, MS-SINK-LOG - Data Sinks (scale-to-zero)
module msSinkTbl 'modules/microservice.bicep' = {
  name: 'ms-sink-tbl-${environment}'
  params: {
    serviceName: 'ms-sink-tbl'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'ms_sink_tbl' }])
  }
}

module msSinkDoc 'modules/microservice.bicep' = {
  name: 'ms-sink-doc-${environment}'
  params: {
    serviceName: 'ms-sink-doc'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'ms_sink_doc' }])
  }
}

module msSinkLog 'modules/microservice.bicep' = {
  name: 'ms-sink-log-${environment}'
  params: {
    serviceName: 'ms-sink-log'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'ms_sink_log' }])
  }
}

// MS-QRY - Query API (CQRS Read Model)
module msQry 'modules/microservice.bicep' = {
  name: 'ms-qry-${environment}'
  params: {
    serviceName: 'ms-qry'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 1
    maxReplicas: 10
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [
      { name: 'DB_NAME', value: 'reportplatform' }
      { name: 'REDIS_HOST', value: redisHost }
    ])
  }
}

// MS-TMPL - Template Registry
module msTmpl 'modules/microservice.bicep' = {
  name: 'ms-tmpl-${environment}'
  params: {
    serviceName: 'ms-tmpl'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'reportplatform' }])
  }
}

// MS-DASH - Dashboard Aggregation
module msDash 'modules/microservice.bicep' = {
  name: 'ms-dash-${environment}'
  params: {
    serviceName: 'ms-dash'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'reportplatform' }])
  }
}

// MS-ADMIN - Admin Service
module msAdmin 'modules/microservice.bicep' = {
  name: 'ms-admin-${environment}'
  params: {
    serviceName: 'ms-admin'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 3
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'admin_db' }])
  }
}

// MS-BATCH - Batch Management
module msBatch 'modules/microservice.bicep' = {
  name: 'ms-batch-${environment}'
  params: {
    serviceName: 'ms-batch'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'batch_db' }])
  }
}

// MS-LIFECYCLE - Report Lifecycle
module msLifecycle 'modules/microservice.bicep' = {
  name: 'ms-lifecycle-${environment}'
  params: {
    serviceName: 'ms-lifecycle'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [
      { name: 'DB_NAME', value: 'reportplatform' }
      { name: 'DAPR_PUBSUB_NAME', value: 'reportplatform-pubsub' }
      { name: 'DAPR_STATESTORE_NAME', value: 'reportplatform-statestore' }
    ])
  }
}

// MS-PERIOD - Period Manager
module msPeriod 'modules/microservice.bicep' = {
  name: 'ms-period-${environment}'
  params: {
    serviceName: 'ms-period'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 3
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'reportplatform' }])
  }
}

// MS-NOTIF - Notifications
module msNotif 'modules/microservice.bicep' = {
  name: 'ms-notif-${environment}'
  params: {
    serviceName: 'ms-notif'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [
      { name: 'DB_NAME', value: 'reportplatform' }
      { name: 'DAPR_PUBSUB_NAME', value: 'reportplatform-pubsub' }
    ])
  }
}

// MS-FORM - Form Builder
module msForm 'modules/microservice.bicep' = {
  name: 'ms-form-${environment}'
  params: {
    serviceName: 'ms-form'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'reportplatform' }])
  }
}

// MS-VER - Versioning
module msVer 'modules/microservice.bicep' = {
  name: 'ms-ver-${environment}'
  params: {
    serviceName: 'ms-ver'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 3
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'reportplatform' }])
  }
}

// MS-AUDIT - Audit & Compliance
module msAudit 'modules/microservice.bicep' = {
  name: 'ms-audit-${environment}'
  params: {
    serviceName: 'ms-audit'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 3
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'reportplatform' }])
  }
}

// MS-SRCH - Search Service
module msSrch 'modules/microservice.bicep' = {
  name: 'ms-srch-${environment}'
  params: {
    serviceName: 'ms-srch'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 5
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [{ name: 'DB_NAME', value: 'reportplatform' }])
  }
}

// MS-TMPL-PPTX - PPTX Template Manager
module msTmplPptx 'modules/microservice.bicep' = {
  name: 'ms-tmpl-pptx-${environment}'
  params: {
    serviceName: 'ms-tmpl-pptx'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 3
    ingressType: 'internal'
    envVars: concat(javaDbEnvBase, [
      { name: 'DB_NAME', value: 'reportplatform' }
      { name: 'AZURE_STORAGE_CONNECTION_STRING', value: storageConnectionString }
      { name: 'AZURE_STORAGE_CONTAINER', value: 'templates' }
    ])
  }
}

// ─── Python Microservices ────────────────────────────────────────────────────

var pythonBaseEnv = [
  { name: 'PYTHONUNBUFFERED', value: '1' }
  { name: 'DAPR_HOST', value: 'localhost' }
  { name: 'DAPR_HTTP_PORT', value: '3500' }
  { name: 'DAPR_GRPC_PORT', value: '50001' }
  { name: 'DAPR_PUBSUB_NAME', value: 'reportplatform-pubsub' }
  { name: 'DAPR_STATESTORE_NAME', value: 'reportplatform-statestore' }
  { name: 'AZURE_STORAGE_CONNECTION_STRING', value: storageConnectionString }
]

// MS-ATM-PPTX - PPTX Atomizer (scale-to-zero, gRPC)
module msAtmPptx 'modules/microservice.bicep' = {
  name: 'ms-atm-pptx-${environment}'
  params: {
    serviceName: 'ms-atm-pptx'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    cpu: '0.5'
    memory: '1Gi'
    minReplicas: 0
    maxReplicas: 10
    targetPort: 8000
    ingressType: 'internal'
    daprAppProtocol: 'http'
    livenessPath: '/health'
    readinessPath: '/health'
    envVars: concat(pythonBaseEnv, [
      { name: 'AZURE_STORAGE_CONTAINER', value: 'file-uploads' }
    ])
  }
}

// MS-ATM-XLS - Excel Atomizer
module msAtmXls 'modules/microservice.bicep' = {
  name: 'ms-atm-xls-${environment}'
  params: {
    serviceName: 'ms-atm-xls'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    cpu: '0.5'
    memory: '1Gi'
    minReplicas: 0
    maxReplicas: 10
    targetPort: 8000
    ingressType: 'internal'
    livenessPath: '/health'
    readinessPath: '/health'
    envVars: concat(pythonBaseEnv, [
      { name: 'AZURE_STORAGE_CONTAINER', value: 'file-uploads' }
    ])
  }
}

// MS-ATM-PDF - PDF/OCR Atomizer (gRPC)
module msAtmPdf 'modules/microservice.bicep' = {
  name: 'ms-atm-pdf-${environment}'
  params: {
    serviceName: 'ms-atm-pdf'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    cpu: '1'
    memory: '2Gi'
    minReplicas: 0
    maxReplicas: 10
    targetPort: 50051
    ingressType: 'internal'
    daprAppProtocol: 'grpc'
    livenessPath: '/health'
    readinessPath: '/health'
    envVars: concat(pythonBaseEnv, [
      { name: 'AZURE_STORAGE_CONTAINER', value: 'file-uploads' }
    ])
  }
}

// MS-ATM-CSV - CSV Atomizer (gRPC)
module msAtmCsv 'modules/microservice.bicep' = {
  name: 'ms-atm-csv-${environment}'
  params: {
    serviceName: 'ms-atm-csv'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 10
    targetPort: 50051
    ingressType: 'internal'
    daprAppProtocol: 'grpc'
    livenessPath: '/health'
    readinessPath: '/health'
    envVars: pythonBaseEnv
  }
}

// MS-ATM-AI - AI Gateway (gRPC)
module msAtmAi 'modules/microservice.bicep' = {
  name: 'ms-atm-ai-${environment}'
  params: {
    serviceName: 'ms-atm-ai'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    cpu: '0.5'
    memory: '1Gi'
    minReplicas: 0
    maxReplicas: 5
    targetPort: 50051
    ingressType: 'internal'
    daprAppProtocol: 'grpc'
    livenessPath: '/health'
    readinessPath: '/health'
    envVars: concat(pythonBaseEnv, [
      { name: 'LITELLM_BASE_URL', value: 'http://litellm:4000' }
    ])
  }
}

// MS-MCP - MCP Server (AI Agent Interface)
module msMcp 'modules/microservice.bicep' = {
  name: 'ms-mcp-${environment}'
  params: {
    serviceName: 'ms-mcp'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    minReplicas: 0
    maxReplicas: 3
    targetPort: 8000
    ingressType: 'internal'
    livenessPath: '/health'
    readinessPath: '/health'
    envVars: concat(pythonBaseEnv, [
      { name: 'DB_HOST', value: 'psql-reportplatform-${environment}.postgres.database.azure.com' }
      { name: 'DB_PORT', value: '5432' }
      { name: 'DB_NAME', value: 'reportplatform' }
    ])
  }
}

// MS-GEN-PPTX - PPTX Generator (gRPC)
module msGenPptx 'modules/microservice.bicep' = {
  name: 'ms-gen-pptx-${environment}'
  params: {
    serviceName: 'ms-gen-pptx'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    cpu: '1'
    memory: '2Gi'
    minReplicas: 0
    maxReplicas: 5
    targetPort: 50051
    ingressType: 'internal'
    daprAppProtocol: 'grpc'
    livenessPath: '/health'
    readinessPath: '/health'
    envVars: concat(pythonBaseEnv, [
      { name: 'TEMPLATES_CONTAINER', value: 'templates' }
      { name: 'GENERATED_CONTAINER', value: 'generated-reports' }
    ])
  }
}

// ─── Frontend ────────────────────────────────────────────────────────────────

module msFe 'modules/microservice.bicep' = {
  name: 'ms-fe-${environment}'
  params: {
    serviceName: 'ms-fe'
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    acrLoginServer: acrLoginServer
    cpu: '0.25'
    memory: '512Mi'
    minReplicas: 1
    maxReplicas: 5
    targetPort: 80
    ingressType: 'external'
    daprAppProtocol: 'http'
    livenessPath: '/health'
    readinessPath: '/health'
    envVars: [
      { name: 'VITE_API_URL', value: 'https://${webGateway.outputs.fqdn}' }
      { name: 'VITE_AZURE_CLIENT_ID', value: azureClientId }
      { name: 'VITE_AZURE_TENANT_ID', value: azureTenantId }
    ]
  }
}

// ─── Outputs ─────────────────────────────────────────────────────────────────

@description('FQDN of the Web Gateway (external)')
output webGatewayFqdn string = webGateway.outputs.fqdn

@description('FQDN of the GraphQL Gateway (internal)')
output graphqlGatewayFqdn string = graphqlGateway.outputs.fqdn

@description('Default domain of the Container Apps Environment')
output environmentDomain string = containerEnv.outputs.defaultDomain

@description('Name of the Container Apps Environment')
output environmentName string = containerEnv.outputs.environmentName

@description('FQDN of the Frontend (external)')
output frontendFqdn string = msFe.outputs.fqdn
