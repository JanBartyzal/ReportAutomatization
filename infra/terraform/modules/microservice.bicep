// Reusable Container App module for ReportPlatform microservices.
// Parameterized for Java (Spring Boot) and Python (FastAPI) services
// with Dapr sidecar, health probes, autoscaling, and OTEL tracing.

@description('Name of the microservice (e.g. ms-auth, ms-atm-pptx)')
param serviceName string

@description('Resource ID of the Container Apps Environment')
param environmentId string

@description('Azure region')
param location string = resourceGroup().location

@description('Container image tag')
param imageTag string = 'latest'

@description('Azure Container Registry login server')
param acrLoginServer string

@description('CPU allocation (in cores)')
param cpu string = '0.5'

@description('Memory allocation')
param memory string = '1Gi'

@description('Minimum replicas (0 = scale-to-zero)')
param minReplicas int = 0

@description('Maximum replicas')
param maxReplicas int = 10

@description('Container port')
param targetPort int = 8080

@description('Ingress type')
@allowed(['external', 'internal', 'none'])
param ingressType string = 'internal'

@description('Dapr app protocol')
@allowed(['http', 'grpc'])
param daprAppProtocol string = 'http'

@description('Liveness probe path')
param livenessPath string = '/actuator/health'

@description('Readiness probe path')
param readinessPath string = '/actuator/health'

@description('Environment variables (array of {name, value} or {name, secretRef})')
param envVars array = []

@description('Secrets (array of {name, value} or {name, keyVaultUrl, identity})')
param secrets array = []

@description('HTTP concurrency for autoscaling')
param httpConcurrency int = 100

@description('CPU utilization % for autoscaling')
param cpuUtilization int = 70

// ─── Variables ───────────────────────────────────────────────────────────────

var daprAppId = serviceName
var containerName = replace(serviceName, 'ms-', '')

// OTEL env vars injected into every service
var otelEnvVars = [
  { name: 'OTEL_EXPORTER_OTLP_ENDPOINT', value: 'http://otel-collector:4317' }
  { name: 'OTEL_SERVICE_NAME', value: serviceName }
  { name: 'OTEL_RESOURCE_ATTRIBUTES', value: 'service.namespace=reportplatform' }
]

var allEnvVars = concat(otelEnvVars, envVars)

// ─── Container App ───────────────────────────────────────────────────────────

resource containerApp 'Microsoft.App/containerApps@2023-05-01' = {
  name: serviceName
  location: location
  properties: {
    managedEnvironmentId: environmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: ingressType != 'none' ? {
        external: ingressType == 'external'
        targetPort: targetPort
        transport: daprAppProtocol
        traffic: [
          { latestRevision: true, weight: 100 }
        ]
      } : null
      dapr: {
        enabled: true
        appId: daprAppId
        appPort: targetPort
        appProtocol: daprAppProtocol
      }
      secrets: secrets
    }
    template: {
      containers: [
        {
          name: containerName
          image: '${acrLoginServer}/${serviceName}:${imageTag}'
          resources: {
            cpu: json(cpu)
            memory: memory
          }
          env: allEnvVars
          probes: [
            {
              type: 'Liveness'
              httpGet: { path: livenessPath, port: targetPort }
              initialDelaySeconds: 15
              periodSeconds: 10
              failureThreshold: 3
            }
            {
              type: 'Readiness'
              httpGet: { path: readinessPath, port: targetPort }
              initialDelaySeconds: 5
              periodSeconds: 5
              failureThreshold: 3
            }
          ]
        }
      ]
      scale: {
        minReplicas: minReplicas
        maxReplicas: maxReplicas
        rules: [
          {
            name: 'http-rule'
            http: {
              metadata: {
                concurrentRequests: string(httpConcurrency)
              }
            }
          }
          {
            name: 'cpu-rule'
            custom: {
              type: 'cpu'
              metadata: {
                type: 'Utilization'
                value: string(cpuUtilization)
              }
            }
          }
        ]
      }
    }
  }
}

// ─── Outputs ─────────────────────────────────────────────────────────────────

@description('Name of the Container App')
output name string = containerApp.name

@description('FQDN of the Container App ingress (empty if no ingress)')
output fqdn string = ingressType != 'none' ? containerApp.properties.configuration.ingress.fqdn : ''
