// TASK-WG-ACA-001: Core BFF (Web Gateway) Container App
// Always-on service (minReplicas: 1) with HTTP + CPU autoscaling.
// Handles REST/CQRS, proxies GraphQL to standalone gateway.
// CORS and rate limiting delegated to Azure APIM layer.

@description('Resource ID of the Container Apps Environment')
param environmentId string

@description('Azure region for the resources')
param location string = resourceGroup().location

@description('Container image tag')
param imageTag string = 'latest'

@description('Default domain of the Container Apps Environment')
param environmentDomain string

@description('PostgreSQL connection string')
@secure()
param postgresConnectionString string

@description('Redis connection string')
@secure()
param redisConnectionString string

// ─── Container App ───────────────────────────────────────────────────────────

resource webGateway 'Microsoft.App/containerApps@2023-05-01' = {
  name: 'unit-web-gateway'
  location: location
  properties: {
    managedEnvironmentId: environmentId
    configuration: {
      activeRevisionsMode: 'Multiple'
      ingress: {
        external: true
        targetPort: 8080
        transport: 'http'
        traffic: [
          { latestRevision: true, weight: 100 }
        ]
      }
      dapr: {
        enabled: true
        appId: 'unit-web-gateway'
        appPort: 8080
        appProtocol: 'http'
      }
      secrets: [
        { name: 'postgres-connection', value: postgresConnectionString }
        { name: 'redis-connection', value: redisConnectionString }
      ]
    }
    template: {
      containers: [
        {
          name: 'web-gateway'
          image: 'cloudinframap.azurecr.io/unit-web-gateway:${imageTag}'
          resources: {
            cpu: json('0.5')
            memory: '1Gi'
          }
          env: [
            // ── APIM Layer configuration ──
            // APIM handles rate limiting, CORS, JWT validation, and Swagger
            { name: 'ApimLayer__Enabled', value: 'true' }
            { name: 'ApimLayer__RateLimitingEnabled', value: 'false' }
            { name: 'ApimLayer__CorsEnabled', value: 'false' }
            { name: 'ApimLayer__JwtMode', value: 'headers' }
            { name: 'ApimLayer__SwaggerEnabled', value: 'false' }

            // ── Module configuration ──
            // GraphQL runs as standalone Container App (scale-to-zero)
            { name: 'Modules__GraphQL__Enabled', value: 'false' }
            { name: 'Modules__GraphQL__ProxyUrl', value: 'https://unit-graphql-gateway.internal.${environmentDomain}' }
            { name: 'Modules__CQRS__Enabled', value: 'true' }
            // MV Refresh handled by scheduled Container Apps Job
            { name: 'Modules__CQRS__MvRefreshEnabled', value: 'false' }
            // Cache invalidation via Dapr pub/sub events
            { name: 'Modules__CacheInvalidation__Mode', value: 'EventDriven' }

            // ── Connection strings ──
            { name: 'ConnectionStrings__PortfolioDb', secretRef: 'postgres-connection' }
            { name: 'Redis__ConnectionString', secretRef: 'redis-connection' }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: { path: '/live', port: 8080 }
              initialDelaySeconds: 10
              periodSeconds: 10
            }
            {
              type: 'Readiness'
              httpGet: { path: '/ready', port: 8080 }
              initialDelaySeconds: 5
              periodSeconds: 5
            }
          ]
        }
      ]
      scale: {
        minReplicas: 1   // Always-on — critical service
        maxReplicas: 10
        rules: [
          {
            name: 'http-rule'
            http: {
              metadata: {
                concurrentRequests: '100'
              }
            }
          }
          {
            name: 'cpu-rule'
            custom: {
              type: 'cpu'
              metadata: {
                type: 'Utilization'
                value: '70'
              }
            }
          }
        ]
      }
    }
  }
}

// ─── Outputs ─────────────────────────────────────────────────────────────────

@description('FQDN of the Web Gateway ingress')
output fqdn string = webGateway.properties.configuration.ingress.fqdn

@description('Name of the Web Gateway Container App')
output name string = webGateway.name
