# Dapr PubSub Topics

| Topic | Publisher | Subscriber(s) | Purpose |
|-------|-----------|---------------|---------|
| file-uploaded | engine-ingestor | ms-orch | Trigger file processing workflow |
| processing-completed | ms-orch | engine-reporting/notif | Notify on workflow completion |
| report.status_changed | engine-reporting/lifecycle | ms-orch, engine-reporting/notif | Report state transition |
| report.data_locked | engine-reporting/lifecycle | engine-data/sink-tbl | Lock data after approval |
| report.local_released | engine-reporting/lifecycle | engine-reporting/notif | Local scope data released |
| notify | ms-orch, engine-reporting/period | engine-reporting/notif | Generic notification trigger |
| version.created | engine-core/versioning | engine-reporting/notif | New version created |
| version.edit_on_locked | engine-core/versioning | engine-reporting/lifecycle | Edit attempt on locked entity |
| data-stored | engine-data/sink-tbl | engine-data/query | Cache invalidation on new data |
| form.response.submitted | engine-reporting/form | engine-reporting/notif | Form response submitted |
| pptx.generation_requested | ms-orch | ms-orch | Trigger PPTX generation workflow |
| pptx.generation_completed | ms-orch | engine-reporting/notif | PPTX generation done |
| snow.sync.completed | engine-integrations | engine-reporting/notif | ServiceNow sync done |
| snow.sync.failed | engine-integrations | engine-reporting/notif | ServiceNow sync failed |
| data-imported | engine-orchestrator | engine-integrations/excel-sync | Trigger Excel export flow after data import |
| promotion.candidate.detected | engine-core/admin | engine-reporting/notif | Smart persistence candidate |
| document-embedding | engine-data/sink-doc | (async processor) | Trigger embedding generation |

## Naming Conventions

- Use kebab-case for simple topics: `file-uploaded`
- Use dot-notation for domain-scoped topics: `report.status_changed`

## Dead Letter Topics

Failed messages are automatically sent to `{topic}-deadletter` by the Dapr runtime.
