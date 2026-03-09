// TASK-WG-ACA-001: GraphQL Gateway Container App (Scale-to-Zero)
// Internal-only service that scales to zero when idle.
// Receives traffic from Core BFF proxy and scales on HTTP concurrency.
// Cost: ~$5/month at low traffic vs ~$50/month always-on.

@description('Resource ID of the Container Apps Environment')
param environmentId string

@description('Azure region for the resources')
param location string = resourceGroup().location

@description('Container image tag')
param imageTag string = 'latest'

@description('Redis connection string')
@secure()
param redisConnectionString string

// ─── Container App ───────────────────────────────────────────────────────────

resource graphqlGateway 'Microsoft.App/containerApps@2023-05-01' = {
  name: 'unit-graphql-gateway'
  location: location
  properties: {
    managedEnvironmentId: environmentId
    configuration: {
      ingress: {
        external: false   // Internal only — accessed via Core BFF proxy
        targetPort: 8080
        transport: 'http'
      }
      dapr: {
        enabled: true
        appId: 'unit-graphql-gateway'
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
          name: 'graphql-gateway'
          image: 'cloudinframap.azurecr.io/unit-graphql-gateway:${imageTag}'
          resources: {
            cpu: json('0.25')
            memory: '512Mi'
          }
          env: [
            { name: 'Modules__GraphQL__Enabled', value: 'true' }
            { name: 'Modules__GraphQL__PlaygroundEnabled', value: 'false' }
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
        minReplicas: 0   // Scale to zero — activated by HTTP traffic
        maxReplicas: 5
        rules: [
          {
            name: 'http-rule'
            http: {
              metadata: {
                concurrentRequests: '50'
              }
            }
          }
        ]
      }
    }
  }
}

// ─── Outputs ─────────────────────────────────────────────────────────────────

@description('FQDN of the GraphQL Gateway (internal)')
output fqdn string = graphqlGateway.properties.configuration.ingress.fqdn

@description('Name of the GraphQL Gateway Container App')
output name string = graphqlGateway.name
