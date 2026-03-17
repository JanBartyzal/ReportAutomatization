Feature: FS04 - Custom Orchestrator
  As the platform
  I want a workflow engine that orchestrates file processing pipelines
  So that uploaded files are routed, parsed, mapped, and stored reliably with saga-based error handling

  # ---------------------------------------------------------------------------
  # Workflow Trigger
  # ---------------------------------------------------------------------------

  Scenario: New file upload triggers workflow automatically
    Given a Dapr PubSub event "new_file" is published by engine-ingestor
    When engine-orchestrator receives the event
    Then a new workflow instance is created
    And the workflow begins with the "Get Metadata" step

  Scenario: Workflow definition is loaded from versioned JSON
    Given a JSON workflow definition exists in the Git repository
    When engine-orchestrator initializes
    Then the workflow definitions are loaded and registered in the Spring State Machine engine

  # ---------------------------------------------------------------------------
  # Pipeline Steps
  # ---------------------------------------------------------------------------

  Scenario: Pipeline routes PPTX file to PPTX Atomizer
    Given a workflow is triggered for a file with mime_type "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    When the Router step evaluates the file type
    Then the workflow invokes processor-atomizers:pptx via Dapr gRPC

  Scenario: Pipeline routes XLSX file to Excel Atomizer
    Given a workflow is triggered for a file with mime_type "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    When the Router step evaluates the file type
    Then the workflow invokes processor-atomizers:xls via Dapr gRPC

  Scenario: Pipeline routes PDF file to PDF Atomizer
    Given a workflow is triggered for a file with mime_type "application/pdf"
    When the Router step evaluates the file type
    Then the workflow invokes processor-atomizers:pdf via Dapr gRPC

  Scenario: Pipeline routes CSV file to CSV Atomizer
    Given a workflow is triggered for a file with mime_type "text/csv"
    When the Router step evaluates the file type
    Then the workflow invokes processor-atomizers:csv via Dapr gRPC

  Scenario: Pipeline applies Schema Mapping after atomizer extraction
    Given an atomizer has returned structured data for a file
    When the workflow proceeds to the Schema Mapping step
    Then the workflow invokes engine-data:template via Dapr gRPC to apply column normalization

  Scenario: Pipeline stores table data via engine-data:sink-tbl
    Given the atomizer output contains table-type data
    When the Filter Logic step classifies the data as "table"
    Then the workflow invokes engine-data:sink-tbl via Dapr gRPC to store the data

  Scenario: Pipeline stores text data via engine-data:sink-doc
    Given the atomizer output contains text-type data
    When the Filter Logic step classifies the data as "text"
    Then the workflow invokes engine-data:sink-doc via Dapr gRPC to store the data

  # ---------------------------------------------------------------------------
  # Type-Safe Contracts
  # ---------------------------------------------------------------------------

  Scenario: Orchestrator uses type-safe Java interfaces and DTOs
    Given engine-orchestrator defines a gRPC service call to an atomizer
    When the request is constructed
    Then it uses a Java interface-defined DTO
    And the response is deserialized into a type-safe Java DTO

  # ---------------------------------------------------------------------------
  # Saga Pattern & Compensating Actions
  # ---------------------------------------------------------------------------

  Scenario: Saga compensates on sink storage failure
    Given a workflow has successfully parsed a file via an atomizer
    And the Schema Mapping step has completed
    When engine-data:sink-tbl returns an error during storage
    Then the orchestrator executes the compensating action for the storage step
    And the workflow state transitions to "COMPENSATING"
    And the compensation result is logged

  Scenario: Saga compensates on atomizer failure after partial processing
    Given a workflow has completed 3 of 5 slide extractions
    When the 4th extraction fails with a ParsingException
    Then the orchestrator executes compensating actions for the completed steps
    And the workflow transitions to "FAILED" with details of the failure

  # ---------------------------------------------------------------------------
  # Async Worker - Parallel Slide Extractions
  # ---------------------------------------------------------------------------

  Scenario: Orchestrator processes slide extractions in parallel
    Given a PPTX file with 30 slides is being processed
    When the orchestrator dispatches slide extraction tasks via Dapr Pub/Sub
    Then between 20 and 50 extraction tasks run in parallel
    And the orchestrator aggregates all results before proceeding to the next step

  # ---------------------------------------------------------------------------
  # Error Handling - Exponential Backoff
  # ---------------------------------------------------------------------------

  Scenario: Atomizer transient failure triggers exponential backoff retry
    Given the orchestrator calls processor-atomizers:pptx via Dapr gRPC
    And processor-atomizers:pptx returns HTTP status 500
    When the retry policy is applied
    Then the orchestrator retries after 1 second
    And if still failing, retries after 5 seconds
    And if still failing, retries after 30 seconds
    And if all 3 retries fail, the job is written to the failed_jobs table

  Scenario: ParsingException is handled and recorded
    Given an atomizer throws a ParsingException
    When the orchestrator catches the exception
    Then the error is recorded in the failed_jobs table with exception type "ParsingException"

  Scenario: StorageException is handled and recorded
    Given a sink service throws a StorageException
    When the orchestrator catches the exception
    Then the error is recorded in the failed_jobs table with exception type "StorageException"

  Scenario: VirusDetectedException halts the workflow immediately
    Given engine-ingestor:scanner returns a VirusDetectedException
    When the orchestrator catches the exception
    Then the workflow halts immediately without retry
    And the error is recorded in the failed_jobs table with exception type "VirusDetectedException"

  # ---------------------------------------------------------------------------
  # Idempotence
  # ---------------------------------------------------------------------------

  Scenario: Re-running workflow for same file does not create duplicate records
    Given a file with file_id "abc-123" has already been processed successfully
    When the same workflow is triggered again for file_id "abc-123"
    Then the orchestrator checks Redis for the file_id + step_hash
    And detects the steps have already been completed
    And does NOT create duplicate records in the database

  Scenario: Redis stores idempotency keys for running workflows
    Given a workflow step completes successfully
    When the orchestrator records the completion
    Then a Redis key is created with the combination of file_id and step_hash
    And subsequent requests with the same key are treated as no-ops

  # ---------------------------------------------------------------------------
  # State Management
  # ---------------------------------------------------------------------------

  Scenario: Running workflow state is stored in Redis
    Given a workflow is actively processing a file
    When the workflow state is queried
    Then the current state is retrieved from Redis
    And includes the current step, progress, and timestamp

  Scenario: Paused or waiting workflow state is stored in PostgreSQL
    Given a workflow is paused waiting for manual approval
    When the workflow state is persisted
    Then the state is stored in PostgreSQL
    And includes the reason for pause and the expected next action

  # ---------------------------------------------------------------------------
  # Dead Letter Queue (DLQ)
  # ---------------------------------------------------------------------------

  Scenario: Failed job is stored in failed_jobs table
    Given a workflow step has failed after all retries
    When the orchestrator writes to the DLQ
    Then a record is created in the failed_jobs table
    And the record contains file_id, step_name, error_type, error_detail, and timestamp

  Scenario: Failed job can be reprocessed from the DLQ UI
    Given a failed job exists in the failed_jobs table
    When an Admin triggers reprocessing via the DLQ UI
    Then the orchestrator creates a new workflow instance for the failed step
    And the original failed_jobs record is updated with reprocess_timestamp

  Scenario: Atomizer failure data is preserved and not lost
    Given an atomizer returns HTTP status 500 after all retries
    When the orchestrator writes the failure to failed_jobs
    Then all available context (file_id, blob_url, step_name, error_detail) is preserved
    And the data is available for manual review and reprocessing
