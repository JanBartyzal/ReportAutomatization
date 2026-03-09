// TASK-WG-ACA-001: Materialized View Refresh Job (Scheduled)
// Container Apps Job that runs every 5 minutes to refresh materialized views.
// Scale-to-zero between executions — only billed for actual compute time.
// Cost: ~$2/month (288 executions/day × ~30s each).

@description('Resource ID of the Container Apps Environment')
param environmentId string

@description('Azure region for the resources')
param location string = resourceGroup().location

@description('Container image tag')
param imageTag string = 'latest'

@description('PostgreSQL connection string')
@secure()
param postgresConnectionString string

@description('Cron expression for the schedule (default: every 5 minutes)')
param cronExpression string = '*/5 * * * *'

@description('Maximum execution time in seconds')
param replicaTimeout int = 300

@description('Number of retries on failure')
param replicaRetryLimit int = 2

// ─── Container Apps Job ──────────────────────────────────────────────────────

resource mvRefreshJob 'Microsoft.App/jobs@2023-05-01' = {
  name: 'mv-refresh-job'
  location: location
  properties: {
    managedEnvironmentId: environmentId
    configuration: {
      triggerType: 'Schedule'
      scheduleTriggerConfig: {
        cronExpression: cronExpression
        parallelism: 1
        replicaCompletionCount: 1
      }
      replicaTimeout: replicaTimeout
      replicaRetryLimit: replicaRetryLimit
      secrets: [
        { name: 'postgres-connection', value: postgresConnectionString }
      ]
    }
    template: {
      containers: [
        {
          name: 'mv-refresh'
          image: 'cloudinframap.azurecr.io/unit-mv-refresh-job:${imageTag}'
          resources: {
            cpu: json('0.25')
            memory: '256Mi'
          }
          env: [
            { name: 'ConnectionStrings__PortfolioDb', secretRef: 'postgres-connection' }
            { name: 'ASPNETCORE_ENVIRONMENT', value: 'Production' }
          ]
          probes: [
            {
              type: 'Startup'
              httpGet: { path: '/health', port: 8080 }
              initialDelaySeconds: 5
              periodSeconds: 3
              failureThreshold: 10
            }
          ]
        }
      ]
    }
  }
}

// ─── Outputs ─────────────────────────────────────────────────────────────────

@description('Name of the MV Refresh Job')
output name string = mvRefreshJob.name

@description('Resource ID of the MV Refresh Job')
output id string = mvRefreshJob.id
