Feature: FS05 - Sinks & Persistence
  As the orchestrator and query services
  I want dedicated sink services for structured data, documents, and logs
  So that parsed content is stored reliably with rollback support and tenant isolation

  # ---------------------------------------------------------------------------
  # Table API (engine-data:sink-tbl) - Structured Data
  # ---------------------------------------------------------------------------

  Scenario: BulkInsert stores structured table data in PostgreSQL JSONB
    Given the orchestrator has parsed table data from a file
    When the orchestrator calls gRPC BulkInsert on engine-data:sink-tbl
    Then the structured data is stored in PostgreSQL using JSONB columns
    And the record includes file_id, org_id, and the table payload

  Scenario: BulkInsert stores OPEX data in PostgreSQL JSONB
    Given the orchestrator has extracted OPEX financial data from a file
    When the orchestrator calls gRPC BulkInsert on engine-data:sink-tbl
    Then the OPEX data is stored in PostgreSQL JSONB format
    And the data is queryable via engine-data:query

  Scenario: BulkInsert stores form response data
    Given a form submission has been processed
    When the orchestrator calls gRPC BulkInsert on engine-data:sink-tbl
    Then the form data is stored in the form_responses table
    And the record includes form_id, org_id, and the response payload

  Scenario: DeleteByFileId removes all records for a file (Saga rollback)
    Given structured data for file_id "abc-123" exists in engine-data:sink-tbl
    When the orchestrator calls gRPC DeleteByFileId with file_id "abc-123"
    Then all records associated with file_id "abc-123" are deleted from PostgreSQL
    And the operation completes as part of a Saga compensating action

  Scenario: Flyway migrations manage the table schema
    Given engine-data:sink-tbl is deployed
    When the service starts
    Then Flyway migrations are applied automatically
    And the database schema is up to date

  Scenario: Row-Level Security ensures tenant isolation
    Given a user with org_id "acme-corp" queries data via engine-data:query
    When the query reaches PostgreSQL
    Then the RLS policy filters results to only org_id "acme-corp"
    And data belonging to other organizations is never returned

  Scenario: Table data is read via engine-data:query (CQRS pattern)
    Given structured data has been stored by engine-data:sink-tbl
    When a frontend client needs to read the data
    Then the request is routed to engine-data:query via the API Gateway
    And engine-data:sink-tbl is NOT called directly for reads

  # ---------------------------------------------------------------------------
  # Document API (engine-data:sink-doc) - Unstructured Data + Vectors
  # ---------------------------------------------------------------------------

  Scenario: StoreDocument saves unstructured JSON to PostgreSQL
    Given the orchestrator has extracted unstructured text content from a file
    When the orchestrator calls gRPC StoreDocument on engine-data:sink-doc
    Then the unstructured JSON is stored in PostgreSQL
    And the record includes file_id, org_id, and the document payload

  Scenario: StoreDocument triggers async vector embedding generation
    Given the orchestrator calls gRPC StoreDocument on engine-data:sink-doc
    When the document is stored successfully
    Then an async request is sent to processor-atomizers:ai for vector embedding generation
    And the embedding model used is "OpenAI text-embedding-3-small" with 1536 dimensions

  Scenario: Vector embeddings are stored in pgVector
    Given processor-atomizers:ai has generated a vector embedding for a document
    When the embedding is returned to engine-data:sink-doc
    Then the embedding is stored in PostgreSQL using the pgVector extension
    And the embedding dimension is 1536

  Scenario: DeleteByFileId removes all documents for a file (Saga rollback)
    Given document data for file_id "abc-123" exists in engine-data:sink-doc
    When the orchestrator calls gRPC DeleteByFileId with file_id "abc-123"
    Then all document records and associated vector embeddings for file_id "abc-123" are deleted
    And the operation completes as part of a Saga compensating action

  Scenario: Document data is read via engine-data:query (CQRS pattern)
    Given unstructured document data has been stored by engine-data:sink-doc
    When a frontend client needs to read or search the data
    Then the request is routed to engine-data:query via the API Gateway
    And engine-data:sink-doc is NOT called directly for reads

  # ---------------------------------------------------------------------------
  # Log API (engine-data:sink-log) - Processing Logs
  # ---------------------------------------------------------------------------

  Scenario: AppendLog writes an append-only processing log entry
    Given a workflow step has completed
    When the orchestrator calls gRPC AppendLog on engine-data:sink-log
    Then an append-only log record is created with the following fields:
      | Field         | Description                              |
      | step_name     | Name of the completed workflow step      |
      | status        | Outcome status (SUCCESS, FAILED, SKIPPED)|
      | duration_ms   | Execution time in milliseconds           |
      | error_detail  | Error message if status is FAILED        |

  Scenario: Processing logs are immutable (append-only)
    Given a log entry has been written by engine-data:sink-log
    When an attempt is made to update or delete the log entry
    Then the operation is rejected
    And the original log entry remains unchanged

  Scenario: Log data is read via engine-data:query (CQRS pattern)
    Given processing logs have been stored by engine-data:sink-log
    When a frontend client needs to view processing history
    Then the request is routed to engine-data:query via the API Gateway
    And engine-data:sink-log is NOT called directly for reads

  Scenario: AppendLog records are associated with file_id and workflow_id
    Given the orchestrator processes a file through multiple workflow steps
    When each step calls gRPC AppendLog on engine-data:sink-log
    Then all log entries are linked by file_id and workflow_id
    And the full processing timeline can be reconstructed via engine-data:query

  # ---------------------------------------------------------------------------
  # Database Infrastructure
  # ---------------------------------------------------------------------------

  Scenario: PostgreSQL 16 is used as the primary data store
    Given the platform infrastructure is deployed
    Then engine-data:sink-tbl, engine-data:sink-doc, and engine-data:sink-log all connect to PostgreSQL 16
    And each service uses its own schema or database as configured

  Scenario: Redis caches query results with 5-minute TTL
    Given engine-data:query receives a query for structured data
    When the query result is returned
    Then the result is cached in Redis with a TTL of 5 minutes
    And subsequent identical queries within the TTL are served from Redis cache

  Scenario: Redis cache is invalidated when new data is written
    Given a cached query result exists in Redis for a specific org_id and file_id
    When engine-data:sink-tbl writes new data for the same org_id
    Then the relevant Redis cache entries are invalidated
    And the next query fetches fresh data from PostgreSQL

  Scenario: Blob Storage is used for binary artifacts
    Given an atomizer generates a binary artifact (e.g., slide PNG image)
    When the artifact is stored
    Then it is written to Azure Blob Storage
    And only the artifact_url is stored in PostgreSQL
