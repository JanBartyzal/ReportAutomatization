// Virtual Network for ReportPlatform ACA deployment
// Provides network isolation with subnets for:
// - Container Apps Environment
// - Private Endpoints (PostgreSQL, Redis, Storage)

@description('Azure region')
param location string = resourceGroup().location

@description('Environment name suffix')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'prod'

var vnetName = 'vnet-reportplatform-${environment}'
var addressPrefix = '10.0.0.0/16'

resource vnet 'Microsoft.Network/virtualNetworks@2023-05-01' = {
  name: vnetName
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [addressPrefix]
    }
    subnets: [
      {
        name: 'snet-aca'
        properties: {
          addressPrefix: '10.0.0.0/21'
          delegations: [
            {
              name: 'aca-delegation'
              properties: {
                serviceName: 'Microsoft.App/environments'
              }
            }
          ]
        }
      }
      {
        name: 'snet-private-endpoints'
        properties: {
          addressPrefix: '10.0.8.0/24'
          privateEndpointNetworkPolicies: 'Disabled'
        }
      }
      {
        name: 'snet-management'
        properties: {
          addressPrefix: '10.0.9.0/24'
        }
      }
    ]
  }
}

output vnetId string = vnet.id
output vnetName string = vnet.name
output acaSubnetId string = vnet.properties.subnets[0].id
output privateEndpointSubnetId string = vnet.properties.subnets[1].id
