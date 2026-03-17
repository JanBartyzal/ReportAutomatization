Feature: FS06 – Analytics & Query
  As a platform user
  I want to query, search, and visualize parsed data through optimized read APIs
  So that I can gain insights from uploaded files and form submissions via dashboards and search

  Background:
    Given the user is authenticated via Azure Entra ID
    And the user has a valid JWT containing org_id and role claims
    And the API Gateway routes requests to the appropriate backend service

  # ---------------------------------------------------------------------------
  # engine-data:query – CQRS Read API
  # ---------------------------------------------------------------------------

  @engine-data:query @CQRS @REST
  Scenario: Query parsed data via CQRS read endpoint
    Given parsed JSONB records exist in the materialized view for org_id "org-001"
    When the frontend sends a GET request to "/api/v1/query/records" with filter parameters
    Then the response status is 200
    And the response body contains only records belonging to org_id "org-001"
    And the response is served from the materialized view, not the write model

  @engine-data:query @caching
  Scenario: Query results are cached in Redis with TTL
    Given a query for "/api/v1/query/records?type=OPEX" has been executed once
    When the same query is executed again within the cache TTL window
    Then the response is served from Redis cache
    And the response header includes "X-Cache: HIT"

  @engine-data:query @caching
  Scenario: Cache is invalidated after TTL expiration
    Given a cached query result exists in Redis with a TTL of 60 seconds
    When the TTL expires
    And the frontend sends the same query again
    Then the response is fetched from the materialized view
    And the fresh result is stored in Redis with a new TTL

  @engine-data:query @materialized-view
  Scenario: Materialized views are refreshed after new data ingestion
    Given a new file has been parsed and stored by engine-data:sink-tbl
    When the materialized view refresh job runs
    Then the materialized view contains the newly ingested records
    And subsequent queries via engine-data:query return the updated data

  # ---------------------------------------------------------------------------
  # engine-data:dashboard – Dashboard Aggregation
  # ---------------------------------------------------------------------------

  @engine-data:dashboard @aggregation @REST
  Scenario: Retrieve aggregated data for a dashboard chart
    Given JSONB records exist for org_id "org-001" across multiple companies
    When the frontend sends a POST request to "/api/v1/dashboard/aggregate" with body:
      """
      {
        "group_by": ["company_id", "cost_center"],
        "order_by": "total_amount DESC",
        "metrics": ["SUM(total_amount)", "COUNT(*)"]
      }
      """
    Then the response status is 200
    And the response contains aggregated rows grouped by company_id and cost_center
    And the rows are ordered by total_amount descending

  @engine-data:dashboard @aggregation
  Scenario: Dashboard aggregation supports UI-configurable GROUP BY and ORDER BY
    Given the UI configuration specifies grouping by "period" and ordering by "revenue ASC"
    When the aggregation endpoint receives the configuration
    Then it generates a SQL query with GROUP BY period and ORDER BY revenue ASC
    And the result set matches the requested grouping and ordering

  @engine-data:dashboard @source-type
  Scenario: Dashboard displays data from both file uploads and form submissions
    Given JSONB records exist with source_type "FILE" from uploaded Excel files
    And JSONB records exist with source_type "FORM" from form submissions
    When the frontend requests the dashboard aggregation for org_id "org-001"
    Then the response includes records from both source types
    And each record includes a "source_type" field with value "FILE" or "FORM"

  @engine-data:dashboard @charts
  Scenario: Dashboard endpoint provides data compatible with Recharts and Nivo
    Given aggregated data is available for the requested period
    When the frontend sends a GET request to "/api/v1/dashboard/chart-data"
    Then the response contains a JSON array suitable for Recharts series format
    And each data point includes "name", "value", and optional "category" fields

  @engine-data:dashboard @SQL-JSONB
  Scenario: Aggregation queries operate over JSONB columns
    Given parsed data is stored as JSONB in the structured_data table
    When the dashboard service builds an aggregation query
    Then the SQL uses JSONB operators to extract fields for GROUP BY and ORDER BY
    And the query executes within the acceptable performance threshold

  # ---------------------------------------------------------------------------
  # engine-data:search – Search Service
  # ---------------------------------------------------------------------------

  @engine-data:search @full-text-search @REST
  Scenario: Full-text search returns matching records
    Given parsed documents contain the text "quarterly revenue forecast"
    When the frontend sends a GET request to "/api/v1/search?q=quarterly+revenue"
    Then the response status is 200
    And the results include documents containing "quarterly revenue"
    And results are ranked by relevance score

  @engine-data:search @full-text-search
  Scenario: Full-text search respects tenant isolation
    Given org_id "org-001" has documents containing "budget"
    And org_id "org-002" has documents containing "budget"
    When a user from org_id "org-001" searches for "budget"
    Then only documents belonging to org_id "org-001" are returned
    And no documents from org_id "org-002" appear in the results

  @engine-data:search @vector-search
  Scenario: Vector search returns semantically similar records via pgVector
    Given document embeddings are stored in pgVector for org_id "org-001"
    When the frontend sends a POST request to "/api/v1/search/semantic" with body:
      """
      {
        "query": "operating expenses by department",
        "top_k": 10
      }
      """
    Then the response status is 200
    And the results contain up to 10 semantically similar records
    And each result includes a similarity score

  @engine-data:search @vector-search
  Scenario: Vector search falls back gracefully when no embeddings exist
    Given no vector embeddings exist for org_id "org-003"
    When a user from org_id "org-003" performs a semantic search
    Then the response status is 200
    And the results array is empty
    And the response includes a message "No semantic index available for this organization"
