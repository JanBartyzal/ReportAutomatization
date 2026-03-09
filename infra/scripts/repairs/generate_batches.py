import os
import json

units_dir = r'i:\Codes\work\CloudInfraMap\microservices\units'
all_units = [d for d in os.listdir(units_dir) if d.startswith('unit-') and os.path.isdir(os.path.join(units_dir, d))]
all_units.sort()

# Zabezpečení přihlášení, web-gateway pro přístup k resources a napojení na secrets a db
core_units = [
    'unit-web-gateway',
    'unit-auth-service',
    'unit-user-registry',
    'unit-organization-manager',
    'unit-vault-connector',
    'unit-rbac-enforcer',
    'unit-token-issuer',
    'unit-audit-logger',
    'unit-api-key-manager',
    'unit-secret-discovery-engine',
    'unit-secret-health-monitor'
]

core_units = [u for u in core_units if u in all_units]
remaining_units = [u for u in all_units if u not in core_units]

groups = {
    '1_Admin_Config_Identity': [],
    '2_Designer_Pricing_ITBM': [],
    '3_Dashboard_AI_Analytics': [],
    '4_Compliance_Security': [],
    '5_Integration_Workflow_ERP': []
}

for u in remaining_units:
    name = u.replace('unit-', '')
    
    # 1. Admin, Config, Onboarding, shell
    if any(k in name for k in ['admin', 'config', 'environment', 'shell', 'sandbox', 'onboarding', 'context', 'tour', 'local-account', 'mfa', 'oidc', 'social-login', 'scim', 'profile', 'tenant', 'branding']):
        groups['1_Admin_Config_Identity'].append(u)
        
    # 2. Designer, Plans, Marketplace, Pricing, ITBM, Network, Storage
    elif any(k in name for k in ['design', 'canvas', 'component', 'blueprint', 'pattern', 'hierarchy', 'plan', 'scenario', 'marketplace', 'import', 'parser', 'price', 'pricing', 'currency', 'tco', 'asset', 'labor', 'license', 'datacenter', 'server', 'power', 'storage', 'egress', 'peering', 'network', 'sku', 'multiplan', 'bicep', 'cloudformation', 'terraform', 'geoarbitrage']):
        groups['2_Designer_Pricing_ITBM'].append(u)
        
    # 3. Dashboard, Executive, AI, Analytics, Telemetry, Web
    elif any(k in name for k in ['dashboard', 'executive', 'cfo', 'portfolio', 'quickwin', 'analytics', 'analyze', 'analyzer', 'anomaly', 'ai', 'nl', 'conversational', 'intent', 'autonomous', 'automation', 'remediation', 'rightsizing', 'forecast', 'burnrate', 'savings', 'telemetry', 'prometheus', 'cloudwatch', 'azure-monitor', 'utilization', 'metric', 'efficiency', 'waste', 'overprovision', 'recommend', 'ml', 'search', 'budget', 'web', 'frontend', 'landing', 'capacity', 'precision']):
        groups['3_Dashboard_AI_Analytics'].append(u)
        
    # 4. Compliance, Security, Export, Document
    elif any(k in name for k in ['compliance', 'gdpr', 'hipaa', 'soc2', 'nis2', 'fedramp', 'pcidss', 'aoc', 'saq', 'cde', 'poam', 'conmon', 'baa', 'phi', 'security', 'guardrail', 'policy', 'approval', 'governance', 'backup', 'restore', 'anonymization', 'retention', 'export', 'csv', 'svg', 'focus', 'evidence', 'preventive', 'root-cause', 'report']):
        groups['4_Compliance_Security'].append(u)
        
    # 5. Integration, ERP, Billing, Job, remaining
    else:
         groups['5_Integration_Workflow_ERP'].append(u)

# Ensure no group + core exceeds 60
max_size = 60 - len(core_units)

final_batches = []
batch_index = 1

for name, units in groups.items():
    # Split if too large
    for i in range(0, len(units), max_size):
        chunk = units[i:i + max_size]
        final_batches.append({
            'name': f'{name}_Part_{i//max_size + 1}' if len(units) > max_size else name,
            'core': core_units,
            'specific': chunk
        })
        batch_index += 1

with open('batches.json', 'w', encoding='utf-8') as f:
    json.dump(final_batches, f, indent=2)

print(f'Total Core Units: {len(core_units)}')
print(f'Created {len(final_batches)} batches.')
for b in final_batches:
    print(f"-- {b['name']}: {len(b['specific'])} specific + {len(b['core'])} core = {len(b['specific']) + len(b['core'])}")
