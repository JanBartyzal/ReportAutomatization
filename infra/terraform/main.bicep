// TASK-WG-ACA-001: Main Bicep deployment for Azure Container Apps
// Orchestrates all ACA modules for CloudInfraMap:
//   - Container Apps Environment with Dapr
//   - Core BFF (always-on, min 1 replica)
//   - GraphQL Gateway (scale-to-zero on HTTP)
//   - MV Refresh Job (scheduled every 5 min)
//   - Cache Invalidator (event-driven scale-to-zero)
//
// Monthly cost estimate (low traffic): ~$58 vs ~$150 all always-on

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

// ─── Core BFF — always-on ────────────────────────────────────────────────────

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

// ─── GraphQL Gateway — scale-to-zero ─────────────────────────────────────────

module graphqlGateway 'modules/graphql-gateway.bicep' = {
  name: 'graphql-gateway-${environment}'
  params: {
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    redisConnectionString: redisConnectionString
  }
}

// ─── MV Refresh Job — scheduled ──────────────────────────────────────────────

module mvRefreshJob 'modules/mv-refresh-job.bicep' = {
  name: 'mv-refresh-job-${environment}'
  params: {
    environmentId: containerEnv.outputs.environmentId
    location: location
    imageTag: imageTag
    postgresConnectionString: postgresConnectionString
  }
}

// ─── Cache Invalidator — event-driven scale-to-zero ──────────────────────────

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

// ─── Outputs ─────────────────────────────────────────────────────────────────

@description('FQDN of the Web Gateway (external)')
output webGatewayFqdn string = webGateway.outputs.fqdn

@description('FQDN of the GraphQL Gateway (internal)')
output graphqlGatewayFqdn string = graphqlGateway.outputs.fqdn

@description('Default domain of the Container Apps Environment')
output environmentDomain string = containerEnv.outputs.defaultDomain

@description('Name of the Container Apps Environment')
output environmentName string = containerEnv.outputs.environmentName
