// Azure Container Registry for Docker images

@description('Azure region')
param location string = resourceGroup().location

@description('Environment name suffix')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'prod'

var acrName = 'acrreportplatform${environment}'

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: acrName
  location: location
  sku: {
    name: environment == 'prod' ? 'Premium' : 'Standard'
  }
  properties: {
    adminUserEnabled: false
    publicNetworkAccess: environment == 'prod' ? 'Disabled' : 'Enabled'
    networkRuleBypassOptions: 'AzureServices'
  }
}

output acrId string = acr.id
output acrName string = acr.name
output acrLoginServer string = acr.properties.loginServer
