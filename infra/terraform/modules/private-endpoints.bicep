// Private Endpoints for PaaS services
// Ensures PostgreSQL, Redis, and Storage are only accessible via VNet

@description('Azure region')
param location string = resourceGroup().location

@description('Subnet ID for private endpoints')
param subnetId string

@description('Resource ID of the PostgreSQL server')
param postgresServerId string = ''

@description('Resource ID of the Redis cache')
param redisCacheId string = ''

@description('Resource ID of the Storage account')
param storageAccountId string = ''

@description('Resource ID of the Key Vault')
param keyVaultId string = ''

// PostgreSQL Private Endpoint
resource postgresEndpoint 'Microsoft.Network/privateEndpoints@2023-05-01' = if (!empty(postgresServerId)) {
  name: 'pe-postgres'
  location: location
  properties: {
    subnet: {
      id: subnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'plsc-postgres'
        properties: {
          privateLinkServiceId: postgresServerId
          groupIds: ['postgresqlServer']
        }
      }
    ]
  }
}

// Redis Private Endpoint
resource redisEndpoint 'Microsoft.Network/privateEndpoints@2023-05-01' = if (!empty(redisCacheId)) {
  name: 'pe-redis'
  location: location
  properties: {
    subnet: {
      id: subnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'plsc-redis'
        properties: {
          privateLinkServiceId: redisCacheId
          groupIds: ['redisCache']
        }
      }
    ]
  }
}

// Storage Private Endpoint
resource storageEndpoint 'Microsoft.Network/privateEndpoints@2023-05-01' = if (!empty(storageAccountId)) {
  name: 'pe-storage'
  location: location
  properties: {
    subnet: {
      id: subnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'plsc-storage'
        properties: {
          privateLinkServiceId: storageAccountId
          groupIds: ['blob']
        }
      }
    ]
  }
}

// Key Vault Private Endpoint
resource keyVaultEndpoint 'Microsoft.Network/privateEndpoints@2023-05-01' = if (!empty(keyVaultId)) {
  name: 'pe-keyvault'
  location: location
  properties: {
    subnet: {
      id: subnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'plsc-keyvault'
        properties: {
          privateLinkServiceId: keyVaultId
          groupIds: ['vault']
        }
      }
    ]
  }
}
