// Azure Key Vault for ReportPlatform secrets management
// Stores database passwords, Redis keys, storage connections, etc.

@description('Azure region')
param location string = resourceGroup().location

@description('Environment name suffix')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'prod'

@description('Tenant ID for Azure AD')
param tenantId string = subscription().tenantId

@description('Principal ID of the ACA managed identity for secret access')
param acaManagedIdentityPrincipalId string = ''

var keyVaultName = 'kv-rptplat-${environment}'

resource keyVault 'Microsoft.KeyVault/vaults@2023-02-01' = {
  name: keyVaultName
  location: location
  properties: {
    sku: {
      family: 'A'
      name: 'standard'
    }
    tenantId: tenantId
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 90
    enablePurgeProtection: environment == 'prod'
    networkAcls: {
      defaultAction: 'Deny'
      bypass: 'AzureServices'
    }
  }
}

// Grant ACA managed identity Secret User role
resource secretUserRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (!empty(acaManagedIdentityPrincipalId)) {
  name: guid(keyVault.id, acaManagedIdentityPrincipalId, '4633458b-17de-408a-b874-0445c86b69e6')
  scope: keyVault
  properties: {
    principalId: acaManagedIdentityPrincipalId
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4633458b-17de-408a-b874-0445c86b69e6')
    principalType: 'ServicePrincipal'
  }
}

output keyVaultId string = keyVault.id
output keyVaultName string = keyVault.name
output keyVaultUri string = keyVault.properties.vaultUri
