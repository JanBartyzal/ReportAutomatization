// Azure Database for PostgreSQL Flexible Server
// Configured with pgvector extension for vector embeddings

@description('Azure region')
param location string = resourceGroup().location

@description('Environment name suffix')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'prod'

@description('Administrator login')
param adminLogin string = 'pgadmin'

@description('Administrator password')
@secure()
param adminPassword string

@description('Subnet ID for VNet integration')
param delegatedSubnetId string = ''

@description('Private DNS Zone ID for PostgreSQL')
param privateDnsZoneId string = ''

@description('PostgreSQL version')
param postgresVersion string = '16'

var serverName = 'psql-reportplatform-${environment}'
var skuName = environment == 'prod' ? 'Standard_D4ds_v5' : 'Standard_B2ms'
var storageSizeGB = environment == 'prod' ? 256 : 64

resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2023-03-01-preview' = {
  name: serverName
  location: location
  sku: {
    name: skuName
    tier: environment == 'prod' ? 'GeneralPurpose' : 'Burstable'
  }
  properties: {
    version: postgresVersion
    administratorLogin: adminLogin
    administratorLoginPassword: adminPassword
    storage: {
      storageSizeGB: storageSizeGB
    }
    backup: {
      backupRetentionDays: environment == 'prod' ? 35 : 7
      geoRedundantBackup: environment == 'prod' ? 'Enabled' : 'Disabled'
    }
    highAvailability: {
      mode: environment == 'prod' ? 'ZoneRedundant' : 'Disabled'
    }
    network: !empty(delegatedSubnetId) ? {
      delegatedSubnetResourceId: delegatedSubnetId
      privateDnsZoneArmResourceId: privateDnsZoneId
    } : {}
  }
}

// Enable pgvector extension
resource pgvectorConfig 'Microsoft.DBforPostgreSQL/flexibleServers/configurations@2023-03-01-preview' = {
  parent: postgresServer
  name: 'azure.extensions'
  properties: {
    value: 'vector,uuid-ossp'
    source: 'user-override'
  }
}

// Create reportplatform database
resource database 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2023-03-01-preview' = {
  parent: postgresServer
  name: 'reportplatform'
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

output serverId string = postgresServer.id
output serverName string = postgresServer.name
output fqdn string = postgresServer.properties.fullyQualifiedDomainName
output connectionString string = 'jdbc:postgresql://${postgresServer.properties.fullyQualifiedDomainName}:5432/reportplatform'
