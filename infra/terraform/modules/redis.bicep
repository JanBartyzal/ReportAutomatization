// Azure Cache for Redis
// Used for Dapr state store, pub/sub, and application caching

@description('Azure region')
param location string = resourceGroup().location

@description('Environment name suffix')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'prod'

var redisCacheName = 'redis-reportplatform-${environment}'
var skuName = environment == 'prod' ? 'Premium' : 'Standard'
var skuFamily = environment == 'prod' ? 'P' : 'C'
var skuCapacity = environment == 'prod' ? 1 : 1

resource redisCache 'Microsoft.Cache/redis@2023-08-01' = {
  name: redisCacheName
  location: location
  properties: {
    sku: {
      name: skuName
      family: skuFamily
      capacity: skuCapacity
    }
    enableNonSslPort: false
    minimumTlsVersion: '1.2'
    redisConfiguration: {
      'maxmemory-policy': 'allkeys-lru'
    }
    publicNetworkAccess: environment == 'prod' ? 'Disabled' : 'Enabled'
  }
}

output redisId string = redisCache.id
output redisName string = redisCache.name
output redisHostName string = redisCache.properties.hostName
output redisPort int = redisCache.properties.sslPort
output redisPrimaryKey string = redisCache.listKeys().primaryKey
