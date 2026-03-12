# Dapr Configuration for Consolidated Services (P8)

This directory contains Dapr component configurations for the 8 consolidated deployment units.

## Consolidated Services and Their Dapr Requirements

| Service | App ID | Components Needed | PubSub Topics |
|---------|--------|-------------------|---------------|
| engine-core | engine-core | pubsub, statestore, secrets | - |
| engine-ingestor | engine-ingestor | pubsub | file.uploaded |
| engine-orchestrator | engine-orchestrator | pubsub, statestore | workflow.* |
| engine-data | engine-data | pubsub, statestore | data.processed |
| engine-reporting | engine-reporting | pubsub, statestore | report.*, notify, deadline.*, form.* |
| engine-integrations | engine-integrations | pubsub, statestore | integration.* |
| processor-atomizers | processor-atomizers | pubsub, statestore | process.* |
| processor-generators | processor-generators | pubsub, statestore | generate.* |

## Directory Structure

```
infra/docker/dapr/
├── components/
│   ├── pubsub.yaml              # Shared pubsub component
│   └── statestore.yaml          # Shared statestore component
├── config/
│   └── dapr.yaml                # Dapr runtime configuration
└── services/
    ├── engine-core/
    │   ├── components/
    │   │   ├── pubsub.yaml
    │   │   └── statestore.yaml
    │   └── config.yaml
    ├── engine-ingestor/
    │   └── components/
    │       └── pubsub.yaml
    ├── engine-orchestrator/
    │   └── components/
    │       ├── pubsub.yaml
    │       └── statestore.yaml
    ├── engine-data/
    │   └── components/
    │       ├── pubsub.yaml
    │       └── statestore.yaml
    ├── engine-reporting/
    │   └── components/
    │       ├── pubsub.yaml
    │       └── statestore.yaml
    ├── engine-integrations/
    │   └── components/
    │       ├── pubsub.yaml
    │       └── statestore.yaml
    ├── processor-atomizers/
    │   └── components/
    │       └── pubsub.yaml
    └── processor-generators/
        └── components/
            ├── pubsub.yaml
            └── statestore.yaml
```

## Usage

Dapr sidecars are automatically configured when using Docker Compose:

```bash
# Start consolidated stack with Dapr
docker compose -f docker-compose.consolidated.yml up -d

# View Dapr sidecar logs
docker logs engine-core-dapr
```

## App-ID Mapping

| Old Service | New Consolidated App ID |
|-------------|------------------------|
| ms-auth | engine-core |
| ms-admin | engine-core |
| ms-batch | engine-core |
| ms-ver | engine-core |
| ms-audit | engine-core |
| ms-ing | engine-ingestor |
| ms-orch | engine-orchestrator |
| ms-sink-tbl | engine-data |
| ms-sink-doc | engine-data |
| ms-sink-log | engine-data |
| ms-qry | engine-data |
| ms-dash | engine-data |
| ms-srch | engine-data |
| ms-tmpl | engine-data |
| ms-lifecycle | engine-reporting |
| ms-period | engine-reporting |
| ms-form | engine-reporting |
| ms-tmpl-pptx | engine-reporting |
| ms-notif | engine-reporting |
| ms-ext-snow | engine-integrations |
| ms-atm-pptx | processor-atomizers |
| ms-atm-xls | processor-atomizers |
| ms-atm-pdf | processor-atomizers |
| ms-atm-csv | processor-atomizers |
| ms-atm-ai | processor-atomizers |
| ms-atm-cln | processor-atomizers |
| ms-gen-pptx | processor-generators |
| ms-gen-xls | processor-generators |
| ms-mcp | processor-generators |
