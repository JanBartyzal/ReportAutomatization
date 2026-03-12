// TASK-WG-ACA-001: Azure Container Apps Environment
// Provides the managed environment for all Container Apps with:
// - Log Analytics integration
// - Dapr components (pub/sub + state store via Redis)
// - Zone redundancy for production

@description('Azure region for the resources')
param location string = resourceGroup().location

@description('Environment name suffix (e.g. dev, staging, prod)')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'prod'

@description('Resource ID of the Log Analytics workspace')
param logAnalyticsWorkspaceId string

@description('Resource ID of the Application Insights instance')
param appInsightsId string

@description('Name of the Azure Cache for Redis instance')
param redisName string

@description('Redis primary access key')
@secure()
param redisPrimaryKey string

@description('Enable zone redundancy (recommended for prod)')
param zoneRedundant bool = true

// ─── Variables ───────────────────────────────────────────────────────────────

var environmentName = 'cim-${environment}'
var redisHost = '${redisName}.redis.cache.windows.net:6380'

// ─── Log Analytics reference ─────────────────────────────────────────────────

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2021-06-01' existing = {
  name: last(split(logAnalyticsWorkspaceId, '/'))
}

// ─── Application Insights reference ─────────────────────────────────────────

resource appInsights 'Microsoft.Insights/components@2020-02-02' existing = {
  name: last(split(appInsightsId, '/'))
}

// ─── Container Apps Environment ──────────────────────────────────────────────

resource containerAppsEnv 'Microsoft.App/managedEnvironments@2023-05-01' = {
  name: environmentName
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalytics.properties.customerId
        sharedKey: logAnalytics.listKeys().primarySharedKey
      }
    }
    daprAIConnectionString: appInsights.properties.ConnectionString
    zoneRedundant: zoneRedundant
  }
}

// ─── Dapr Pub/Sub Component (Redis) ─────────────────────────────────────────

resource daprPubSub 'Microsoft.App/managedEnvironments/daprComponents@2023-05-01' = {
  parent: containerAppsEnv
  name: 'pubsub'
  properties: {
    componentType: 'pubsub.redis'
    version: 'v1'
    metadata: [
      { name: 'redisHost', value: redisHost }
      { name: 'redisPassword', secretRef: 'redis-password' }
      { name: 'enableTLS', value: 'true' }
    ]
    secrets: [
      { name: 'redis-password', value: redisPrimaryKey }
    ]
    scopes: [
      'unit-cache-invalidator'
      'unit-web-gateway'
      'ms-auth'
      'ms-ing'
      'ms-orch'
      'ms-scan'
      'ms-sink-tbl'
      'ms-sink-doc'
      'ms-sink-log'
      'ms-qry'
      'ms-tmpl'
      'ms-dash'
      'ms-admin'
      'ms-batch'
      'ms-lifecycle'
      'ms-period'
      'ms-notif'
      'ms-form'
      'ms-ver'
      'ms-audit'
      'ms-srch'
      'ms-tmpl-pptx'
      'ms-atm-pptx'
      'ms-atm-xls'
      'ms-atm-pdf'
      'ms-atm-csv'
      'ms-atm-ai'
      'ms-mcp'
      'ms-gen-pptx'
    ]
  }
}

// ─── Dapr State Store Component (Redis) ──────────────────────────────────────

resource daprStateStore 'Microsoft.App/managedEnvironments/daprComponents@2023-05-01' = {
  parent: containerAppsEnv
  name: 'statestore'
  properties: {
    componentType: 'state.redis'
    version: 'v1'
    metadata: [
      { name: 'redisHost', value: redisHost }
      { name: 'redisPassword', secretRef: 'redis-password' }
      { name: 'enableTLS', value: 'true' }
    ]
    secrets: [
      { name: 'redis-password', value: redisPrimaryKey }
    ]
  }
}

// ─── Outputs ─────────────────────────────────────────────────────────────────

@description('Resource ID of the Container Apps Environment')
output environmentId string = containerAppsEnv.id

@description('Default domain of the Container Apps Environment')
output defaultDomain string = containerAppsEnv.properties.defaultDomain

@description('Name of the Container Apps Environment')
output environmentName string = containerAppsEnv.name
