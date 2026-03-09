// TASK-WG-ACA-001: Cache Invalidator Container App (Event-Driven Scale-to-Zero)
// Scales to zero when no events in the Redis pub/sub queue.
// Activated by cache-invalidation-queue messages via KEDA Redis scaler.
// Cost: ~$1/month (event-driven, minimal compute).

@description('Resource ID of the Container Apps Environment')
param environmentId string

@description('Azure region for the resources')
param location string = resourceGroup().location

@description('Container image tag')
param imageTag string = 'latest'

@description('Redis connection string')
@secure()
param redisConnectionString string

@description('Redis host address (host:port)')
param redisHost string

// ─── Container App ───────────────────────────────────────────────────────────

resource cacheInvalidator 'Microsoft.App/containerApps@2023-05-01' = {
  name: 'unit-cache-invalidator'
  location: location
  properties: {
    managedEnvironmentId: environmentId
    configuration: {
      ingress: {
        external: false   // Internal only — event-driven
        targetPort: 8080
        transport: 'http'
      }
      dapr: {
        enabled: true
        appId: 'unit-cache-invalidator'
        appPort: 8080
        appProtocol: 'http'
      }
      secrets: [
        { name: 'redis-connection', value: redisConnectionString }
      ]
    }
    template: {
      containers: [
        {
          name: 'cache-invalidator'
          image: 'cloudinframap.azurecr.io/unit-cache-invalidator:${imageTag}'
          resources: {
            cpu: json('0.1')
            memory: '128Mi'
          }
          env: [
            { name: 'Redis__ConnectionString', secretRef: 'redis-connection' }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: { path: '/live', port: 8080 }
              initialDelaySeconds: 5
              periodSeconds: 10
            }
          ]
        }
      ]
      scale: {
        minReplicas: 0   // Scale to zero — activated by Redis queue events
        maxReplicas: 3
        rules: [
          {
            // Scale based on Redis pub/sub queue depth
            name: 'redis-pubsub'
            custom: {
              type: 'redis'
              metadata: {
                address: redisHost
                listName: 'cache-invalidation-queue'
                listLength: '5'
              }
              auth: [
                { secretRef: 'redis-connection', triggerParameter: 'password' }
              ]
            }
          }
        ]
      }
    }
  }
}

// ─── Outputs ─────────────────────────────────────────────────────────────────

@description('Name of the Cache Invalidator Container App')
output name string = cacheInvalidator.name
