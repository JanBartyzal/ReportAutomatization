# Spark Migration Guide – Table Data Storage

**Context:** RA platform currently stores all structured table data (Excel sheets, PPTX tables, CSV files, ServiceNow exports) as JSONB rows in PostgreSQL (`parsed_tables`). This guide describes how to migrate one or more data flows to an external Spark / Delta Lake pipeline on Azure Data Lake Storage Gen2 (ADLS Gen2), and how to operate both backends in parallel.

---

## Architecture Overview

```
┌────────────┐   file-uploaded event   ┌─────────────────────┐
│ engine-    │ ──────────────────────► │ engine-orchestration │
│ ingestor   │   (storageHint=SPARK)   │                      │
└────────────┘                         │  STORE step          │
                                       │  if SPARK hint:      │
                                       │    publish           │
                                       │    spark-ingest-     │
                                       │    requested         │
                                       └──────────┬──────────┘
                                                  │
                              ┌───────────────────┘
                              ▼
                  ┌───────────────────────┐
                  │ External Spark / ADF  │
                  │ Pipeline              │
                  │  - reads from Blob    │
                  │  - writes Parquet /   │
                  │    Delta to ADLS Gen2 │
                  └───────────────────────┘
```

For POSTGRES path (default):

```
  STORE step → engine-data TableSink → parsed_tables (JSONB, PostgreSQL)
```

---

## Key Components Introduced

| Component | Location | Purpose |
|---|---|---|
| `TableStorageBackend` interface | `engine-data/sink-tbl/backend/` | Common contract for all backends |
| `PostgresTableStorageBackend` | `engine-data/sink-tbl/backend/` | Default JSONB sink (unchanged behaviour) |
| `SparkTableStorageBackend` | `engine-data/sink-tbl/backend/` | Publishes `spark-ingest-requested` Dapr event |
| `StorageRoutingService` | `engine-data/sink-tbl/service/` | Resolves backend per org+sourceType; reloads every 5 min |
| `StorageRoutingConfigEntity` | `engine-data/sink-tbl/entity/` | DB-backed routing rules table |
| `StorageRoutingAdminController` | `engine-data/app/controller/` | REST admin API to manage rules |
| `storage_backend` column | `parsed_tables` | Records which backend stored each row |
| `storage_hint` field | `FileUploadedEvent` proto | Per-file hint propagated from ingestor to orchestrator |
| `ServiceNowAtomizerService` | `processor-atomizers/servicenow/` | Parses ServiceNow CSV/JSON/Excel exports |
| `spark-ingest-requested` topic | Dapr Pub/Sub | Trigger for external Spark pipeline |
| `spark-delete-requested` topic | Dapr Pub/Sub | Saga compensation for Spark-stored files |

---

## Database Migrations

| Migration | Description |
|---|---|
| `V11_0_1__sinktbl_add_storage_backend.sql` | Adds `storage_backend VARCHAR(20) DEFAULT 'POSTGRES'` to `parsed_tables` |
| `V11_0_2__sinktbl_create_storage_routing_config.sql` | Creates `storage_routing_config` table with a global POSTGRES default row |

---

## Dapr Pub/Sub Topics (New)

| Topic | Publisher | Subscriber | Payload fields |
|---|---|---|---|
| `spark-ingest-requested` | `engine-orchestration` | External Spark / ADF pipeline | `fileId`, `fileType`, `orgId`, `blobUrl`, `requestedAt` |
| `spark-delete-requested` | `engine-orchestration` | External Spark / ADF pipeline | `fileId`, `orgId`, `requestedAt` |

The external pipeline **must** acknowledge these topics via a Dapr Pub/Sub subscription or Azure Event Grid trigger. Until the pipeline is configured, routing to SPARK will result in published events that are not consumed – data will not be persisted.

---

## How to Route an Organisation to Spark

### Option A – Admin REST API (recommended)

```http
PUT /api/v1/admin/storage-routing
Content-Type: application/json

{
  "orgId": "550e8400-e29b-41d4-a716-446655440000",
  "sourceType": "EXCEL",
  "backend": "SPARK",
  "effectiveFrom": "2026-06-01T00:00:00Z",
  "createdBy": "admin@holding.com"
}
```

Changes take effect within 5 minutes (next rule cache refresh).  
Call `POST /api/v1/admin/storage-routing/refresh` to force immediate reload.

### Option B – Direct DB insert

```sql
INSERT INTO storage_routing_config (org_id, source_type, backend, effective_from, created_by)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'EXCEL',
    'SPARK',
    NOW(),
    'migration-team'
);
```

### Option C – Per-file hint (ad-hoc / testing)

Set `storageHint: "SPARK"` in the `file-uploaded` Dapr event payload.  
Useful for testing without touching the routing config table.

---

## Rule Specificity (first match wins)

| Priority | org_id | source_type | Example use |
|---|---|---|---|
| 1 (highest) | specific | specific | Migrate only EXCEL for org X |
| 2 | specific | NULL | Migrate all file types for org X |
| 3 | NULL | specific | Migrate all EXCEL globally |
| 4 (lowest) | NULL | NULL | Global default (currently POSTGRES) |

---

## Rollback Procedure

1. **Call the admin API** to set the rule back to `POSTGRES`:
   ```http
   PUT /api/v1/admin/storage-routing
   { "orgId": "...", "sourceType": "EXCEL", "backend": "POSTGRES", "createdBy": "rollback" }
   ```
2. Files processed while Spark was active are **not** in `parsed_tables`.  
   They must be re-ingested via the standard workflow, or read directly from Delta Lake.
3. If the Spark pipeline failed silently, check:
   - Dapr dead-letter queue for `spark-ingest-requested`
   - Azure Monitor / Databricks job logs

---

## Pre-Switch Checklist

Before routing any production org to SPARK:

- [ ] External Spark / ADF pipeline is deployed and subscribing to `spark-ingest-requested`
- [ ] Pipeline can read from the same Azure Blob Storage container (`file-uploads`)
- [ ] Delta Lake output location is accessible to the query layer
- [ ] `spark-delete-requested` compensation flow is implemented in the pipeline
- [ ] Monitoring for `spark-ingest-requested` dead-letter queue is configured
- [ ] At least one successful end-to-end test with a non-production org
- [ ] `storage_backend` column data verified (rows show `SPARK` after ingestion)

---

## Schema Mapping: parsed_tables → Delta Lake

| `parsed_tables` column | Delta Lake column | Notes |
|---|---|---|
| `file_id` | `file_id` STRING | Partition key candidate |
| `org_id` | `org_id` STRING | Partition key candidate |
| `source_sheet` | `source_sheet` STRING | |
| `headers` | Exploded as separate columns OR `headers` ARRAY\<STRING\> | Schema-on-read recommended initially |
| `rows` | `rows` ARRAY\<ARRAY\<STRING\>\> or exploded | Explode for BI tooling |
| `metadata` | `metadata` MAP\<STRING,STRING\> | |
| `storage_backend` | `storage_backend` STRING | Always "SPARK" |
| `created_at` | `ingested_at` TIMESTAMP | |

---

## ServiceNow Exports

ServiceNow data flows via the same pipeline after U4:

```
ServiceNow → export file (CSV / JSON / Excel)
          → uploaded to Blob Storage
          → file-uploaded event (fileType: "SERVICE_NOW", storageHint: "SPARK")
          → ServiceNowAtomizerService.ExtractAll()
          → spark-ingest-requested event (for Spark path)
             OR TableSink.BulkInsert()  (for Postgres path, source_type: "SERVICE_NOW")
```

For POSTGRES routing of ServiceNow data, `StorageRoutingService` resolves via `sourceType = "SERVICE_NOW"`.

---

## Monitoring

| Signal | Where to look |
|---|---|
| `spark-ingest-requested` published | Dapr dashboard / Zipkin traces |
| `spark-ingest-requested` consumed | Azure Event Grid / Databricks job metrics |
| Rows in `parsed_tables` with `storage_backend = 'SPARK'` | `SELECT storage_backend, COUNT(*) FROM parsed_tables GROUP BY 1` |
| Failed Spark ingestions | Azure Monitor, Databricks job runs, Dapr DLQ |
| Current routing rules | `GET /api/v1/admin/storage-routing` |
