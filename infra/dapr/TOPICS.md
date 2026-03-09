# Dapr Pub/Sub Topic Registry

## Platform Topics

| Topic | Publisher | Subscriber(s) | Event Proto | Description |
|-------|-----------|---------------|-------------|-------------|
| `file-uploaded` | ms-ing | ms-orch | `orchestrator.v1.FileUploadedEvent` | Triggered after file is uploaded and scanned |
| `processing-completed` | ms-orch | ms-notif | `orchestrator.v1.ProcessingCompletedEvent` | Triggered when file processing workflow completes |
| `report.status_changed` | ms-lifecycle | ms-orch, ms-notif | `lifecycle.v1.ReportStatusChangedEvent` | Triggered on every report status transition |
| `report.data_locked` | ms-lifecycle | ms-sink-tbl | `lifecycle.v1.ReportDataLockedEvent` | Triggered when report is approved (data becomes read-only) |
| `notify` | ms-orch | ms-notif | `notification.v1.NotificationEvent` | Generic notification dispatch |

## Dead Letter Topics

Failed messages are automatically sent to `{topic}-deadletter` by the Dapr runtime.

## Naming Conventions

- Use kebab-case for simple topics: `file-uploaded`
- Use dot-notation for domain-scoped topics: `report.status_changed`
