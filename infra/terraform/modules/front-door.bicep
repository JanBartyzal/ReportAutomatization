// Azure Front Door with WAF for external traffic management
// Provides SSL offloading, CDN, and Web Application Firewall

@description('Environment name suffix')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'prod'

@description('FQDN of the backend ACA Web Gateway')
param backendFqdn string

var frontDoorName = 'fd-reportplatform-${environment}'
var wafPolicyName = 'wafReportplatform${environment}'

// WAF Policy
resource wafPolicy 'Microsoft.Network/FrontDoorWebApplicationFirewallPolicies@2022-05-01' = {
  name: wafPolicyName
  location: 'global'
  sku: {
    name: 'Premium_AzureFrontDoor'
  }
  properties: {
    policySettings: {
      enabledState: 'Enabled'
      mode: environment == 'prod' ? 'Prevention' : 'Detection'
      requestBodyCheck: 'Enabled'
    }
    managedRules: {
      managedRuleSets: [
        {
          ruleSetType: 'Microsoft_DefaultRuleSet'
          ruleSetVersion: '2.1'
          ruleSetAction: 'Block'
        }
        {
          ruleSetType: 'Microsoft_BotManagerRuleSet'
          ruleSetVersion: '1.0'
          ruleSetAction: 'Block'
        }
      ]
    }
  }
}

// Front Door Profile
resource frontDoor 'Microsoft.Cdn/profiles@2023-05-01' = {
  name: frontDoorName
  location: 'global'
  sku: {
    name: 'Premium_AzureFrontDoor'
  }
  properties: {}
}

// Endpoint
resource endpoint 'Microsoft.Cdn/profiles/afdEndpoints@2023-05-01' = {
  parent: frontDoor
  name: 'ep-reportplatform-${environment}'
  location: 'global'
  properties: {
    enabledState: 'Enabled'
  }
}

// Origin Group
resource originGroup 'Microsoft.Cdn/profiles/originGroups@2023-05-01' = {
  parent: frontDoor
  name: 'og-aca'
  properties: {
    healthProbeSettings: {
      probePath: '/health'
      probeRequestType: 'HEAD'
      probeProtocol: 'Https'
      probeIntervalInSeconds: 30
    }
    loadBalancingSettings: {
      sampleSize: 4
      successfulSamplesRequired: 3
    }
  }
}

// Origin (ACA Web Gateway)
resource origin 'Microsoft.Cdn/profiles/originGroups/origins@2023-05-01' = {
  parent: originGroup
  name: 'origin-aca-gw'
  properties: {
    hostName: backendFqdn
    httpPort: 80
    httpsPort: 443
    originHostHeader: backendFqdn
    priority: 1
    weight: 1000
  }
}

output frontDoorId string = frontDoor.id
output endpointHostName string = endpoint.properties.hostName
