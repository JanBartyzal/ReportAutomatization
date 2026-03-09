# Protocol Buffer Definitions

Shared `.proto` files for the PPTX Analyzer & Automation Platform.

## Structure

```
packages/protos/
├── buf.yaml              # buf v2 module config
├── buf.gen.yaml          # codegen config (Java + Python)
├── common/v1/            # shared types (RequestContext, Pagination, etc.)
├── orchestrator/v1/      # MS-ORCH workflow engine
├── atomizer/v1/          # MS-ATM-* file extractors (PPTX, Excel, PDF, CSV, AI)
├── sink/v1/              # MS-SINK-* data persistence (Table, Document, Log)
├── scanner/v1/           # MS-SCAN antivirus & sanitization
├── template/v1/          # MS-TMPL schema mapping
├── lifecycle/v1/         # MS-LIFECYCLE report state machine events
├── notification/v1/      # MS-NOTIF notification events
├── generator/v1/         # MS-GEN-PPTX report generation
├── form/v1/              # MS-FORM (reserved for future use)
└── period/v1/            # MS-PERIOD (reserved for future use)
```

## Usage

```bash
# Generate Java + Python stubs
./scripts/proto-gen.sh
```

Requires [buf CLI](https://buf.build/docs/installation).

## Conventions

- All services import `common/v1/common.proto` for shared types
- Java package: `com.reportplatform.proto.{service}.v1`
- Package naming: `{service}.v1`
- Binary data is never inlined – always use `BlobReference`
