Feature: FS08 – Batch Management
  As a HoldingAdmin or subsidiary user
  I want to organize uploaded files into batches and reporting periods with strict tenant isolation
  So that the holding can see consolidated reports and data never leaks across organizations

  Background:
    Given the user is authenticated via Azure Entra ID
    And the user has a valid JWT containing org_id, holding_id, and company_id claims
    And Row-Level Security is enforced at the PostgreSQL level

  # ---------------------------------------------------------------------------
  # Organization Metadata
  # ---------------------------------------------------------------------------

  @engine-core:admin @org-metadata
  Scenario: Every uploaded file is tagged with organization metadata
    Given a user from company "company-A" in holding "holding-001" uploads a file
    When the file is ingested by engine-ingestor
    Then the file record includes holding_id "holding-001"
    And the file record includes company_id "company-A"
    And the file record includes uploaded_by with the authenticated user's identity

  @engine-core:admin @org-metadata
  Scenario: Organization metadata is immutable after upload
    Given a file was uploaded by user "user-010" from company "company-A"
    When any user attempts to modify the holding_id or company_id of the file
    Then the request is rejected with status 403
    And the original metadata remains unchanged

  # ---------------------------------------------------------------------------
  # Batch Grouping
  # ---------------------------------------------------------------------------

  @engine-core:batch @batch-creation
  Scenario: HoldingAdmin creates a new batch for a reporting period
    Given a HoldingAdmin for holding "holding-001"
    When the HoldingAdmin sends a POST to "/api/v1/batches" with body:
      """
      {
        "name": "Q2/2025",
        "holding_id": "holding-001",
        "period_start": "2025-04-01",
        "period_end": "2025-06-30"
      }
      """
    Then the response status is 201
    And a batch with name "Q2/2025" is created for holding "holding-001"
    And the batch is assigned a unique batch_id

  @engine-core:batch @batch-upload
  Scenario: Subsidiary user uploads a file tagged with a batch
    Given a batch "Q2/2025" with batch_id "batch-100" exists for holding "holding-001"
    And a user from company "company-B" in holding "holding-001"
    When the user uploads a file with batch_tag "batch-100"
    Then the file is associated with batch "batch-100"
    And the file inherits the organization metadata from the user's JWT

  @engine-core:batch @batch-consolidation
  Scenario: HoldingAdmin views a consolidated report for a batch
    Given batch "Q2/2025" contains files from "company-A", "company-B", and "company-C"
    And all files have been parsed and stored
    When the HoldingAdmin accesses "/api/v1/batches/batch-100/report"
    Then the response status is 200
    And the consolidated report includes data from all three companies
    And data is grouped by company_id

  @engine-core:batch @batch-status
  Scenario: Batch tracks delivery status per subsidiary
    Given batch "Q2/2025" is assigned to companies "company-A", "company-B", and "company-C"
    And only "company-A" and "company-C" have uploaded files
    When the HoldingAdmin views the batch status
    Then "company-A" is marked as "DELIVERED"
    And "company-B" is marked as "PENDING"
    And "company-C" is marked as "DELIVERED"

  # ---------------------------------------------------------------------------
  # Batch maps to Reporting Period (FS20)
  # ---------------------------------------------------------------------------

  @engine-core:batch @reporting-period
  Scenario: Batch is mapped to a reporting period with period_id
    Given a batch "Q2/2025" exists with batch_id "batch-100"
    When the system associates the batch with a reporting period
    Then the batch record includes a period_id replacing the generic batch_id for OPEX reporting
    And all queries using period_id return the same data as batch_id "batch-100"

  @engine-core:batch @reporting-period
  Scenario: Period-based queries return data from the corresponding batch
    Given a reporting period with period_id "period-Q2-2025" is linked to batch "batch-100"
    When the frontend queries "/api/v1/periods/period-Q2-2025/data"
    Then the response includes all parsed records from batch "batch-100"
    And the response is scoped to the user's org_id from JWT

  # ---------------------------------------------------------------------------
  # Row-Level Security (RLS)
  # ---------------------------------------------------------------------------

  @engine-core:batch @RLS @security
  Scenario: Every SQL query is filtered by org_id from JWT
    Given a user with org_id "org-001" sends a query request
    When the database executes the query
    Then the PostgreSQL RLS policy automatically appends a WHERE clause for org_id = "org-001"
    And no rows from other organizations are returned

  @engine-core:batch @RLS @security
  Scenario: Cross-tenant data access is architecturally impossible
    Given user "user-A" belongs to org_id "org-001"
    And user "user-B" belongs to org_id "org-002"
    When user "user-A" attempts to query data with an explicit org_id = "org-002" filter
    Then the RLS policy overrides the filter with org_id from the JWT
    And only data for org_id "org-001" is returned

  @engine-core:batch @RLS @security
  Scenario: RLS is enforced even for direct SQL queries from services
    Given a microservice sets the session variable "app.current_org_id" from the JWT before executing a query
    When the query is executed against any table with RLS enabled
    Then only rows matching the session org_id are visible
    And attempting to INSERT a row with a different org_id is blocked by the RLS policy

  @engine-core:batch @RLS @security
  Scenario: HoldingAdmin can access data across subsidiaries within their holding
    Given a HoldingAdmin with holding_id "holding-001"
    And holding "holding-001" contains companies "company-A" and "company-B"
    When the HoldingAdmin queries batch data
    Then data from both "company-A" and "company-B" is returned
    But data from companies outside "holding-001" is not returned
