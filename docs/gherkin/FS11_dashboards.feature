Feature: FS11 - Dashboards & SQL Reporting
  As a platform user
  I want interactive dashboards with configurable SQL-based data sources
  So that I can visualize and analyze OPEX data from JSONB tables without writing raw SQL

  # ---------------------------------------------------------------------------
  # Dashboard Creation & Visibility
  # ---------------------------------------------------------------------------

  Scenario: Admin creates a new dashboard
    Given a user with role "Admin" is authenticated
    When the user creates a new dashboard with name "Q1 OPEX Overview"
    Then the dashboard is saved with status "draft"
    And the dashboard owner is set to the authenticated user

  Scenario: Editor creates a new dashboard
    Given a user with role "Editor" is authenticated
    When the user creates a new dashboard with name "IT Cost Breakdown"
    Then the dashboard is saved with status "draft"
    And the dashboard owner is set to the authenticated user

  Scenario: Viewer cannot create a dashboard
    Given a user with role "Viewer" is authenticated
    When the user attempts to create a new dashboard
    Then the system returns HTTP status 403

  Scenario: Viewer sees only public dashboards
    Given the following dashboards exist:
      | name              | is_public | owner_role |
      | Public Finance    | true      | Admin      |
      | Internal Ops      | false     | Admin      |
      | Public HR         | true      | Editor     |
    And a user with role "Viewer" is authenticated
    When the user requests the list of dashboards
    Then the response contains 2 dashboards
    And all returned dashboards have is_public set to true

  Scenario: Admin sees all dashboards within their organization
    Given the following dashboards exist:
      | name              | is_public | owner_role |
      | Public Finance    | true      | Admin      |
      | Internal Ops      | false     | Admin      |
      | Draft HR          | false     | Editor     |
    And a user with role "Admin" is authenticated
    When the user requests the list of dashboards
    Then the response contains 3 dashboards

  Scenario: Admin toggles dashboard visibility to public
    Given a user with role "Admin" is authenticated
    And a dashboard "Internal Ops" exists with is_public set to false
    When the user sets is_public to true for dashboard "Internal Ops"
    Then the dashboard "Internal Ops" has is_public set to true
    And Viewer users can now see the dashboard

  # ---------------------------------------------------------------------------
  # Data Source - SQL Queries over JSONB
  # ---------------------------------------------------------------------------

  Scenario: Dashboard widget queries JSONB table using PostgreSQL JSON functions
    Given a dashboard widget is configured with a SQL query
    And the query uses PostgreSQL JSON functions over JSONB columns
    When engine-data:dashboard executes the query
    Then the result set is returned as structured JSON
    And the response includes column names and typed values

  Scenario: JSONB columns are accessible as virtual SQL tables
    Given OPEX data is stored in a JSONB column in PostgreSQL
    When a dashboard query references the JSONB column with jsonb_to_recordset
    Then the data is returned as if it were a regular relational table

  # ---------------------------------------------------------------------------
  # UI Configuration - No SQL Knowledge Required
  # ---------------------------------------------------------------------------

  Scenario: User configures GROUP BY via UI dropdown
    Given a user with role "Editor" is editing a dashboard widget
    When the user selects "cost_center" as the GROUP BY field from the UI dropdown
    Then the widget query is updated with GROUP BY cost_center
    And the widget preview refreshes with grouped data

  Scenario: User configures ORDER BY via UI dropdown
    Given a user with role "Editor" is editing a dashboard widget
    When the user selects "amount_czk" as the ORDER BY field and "DESC" as direction
    Then the widget query is updated with ORDER BY amount_czk DESC
    And the widget preview shows data sorted by amount descending

  Scenario: User applies date filter via UI date picker
    Given a user with role "Editor" is editing a dashboard widget
    When the user sets the date filter from "2026-01-01" to "2026-03-31"
    Then the widget query includes a WHERE clause filtering by the date range
    And only data within Q1 2026 is displayed

  Scenario: User applies organization filter via UI selector
    Given a user with role "Editor" is editing a dashboard widget
    And the user's organization has 3 subsidiaries
    When the user selects subsidiary "Alpha Corp" from the org filter
    Then the widget query includes a WHERE clause filtering by org_id of "Alpha Corp"
    And only data for "Alpha Corp" is displayed

  Scenario: Combined filters generate correct query
    Given a user with role "Editor" is editing a dashboard widget
    When the user selects GROUP BY "cost_center", ORDER BY "amount_czk DESC"
    And the user sets date filter "2026-01-01" to "2026-03-31"
    And the user selects org filter "Alpha Corp"
    Then the generated query includes all selected filters
    And the widget preview displays correctly filtered and grouped data

  # ---------------------------------------------------------------------------
  # Advanced - Direct SQL Editor
  # ---------------------------------------------------------------------------

  Scenario: Advanced user writes a custom SQL query in the SQL editor
    Given a user with role "Admin" is editing a dashboard widget
    When the user switches to the direct SQL editor mode
    And enters a valid SQL query with JOIN and aggregation functions
    Then the query is validated for syntax correctness
    And the widget preview displays the query results

  Scenario: SQL editor rejects dangerous SQL statements
    Given a user with role "Admin" is using the direct SQL editor
    When the user enters a query containing "DROP TABLE"
    Then the system rejects the query
    And the response contains "prohibited SQL statement"

  Scenario: SQL editor rejects queries outside user's org scope
    Given a user with role "Admin" is using the direct SQL editor
    And the user belongs to org_id "org-123"
    When the user enters a query that does not include org_id filtering
    Then the system automatically appends the org_id filter
    And the query results are scoped to "org-123" only

  Scenario: SQL editor validates query before execution
    Given a user with role "Admin" is using the direct SQL editor
    When the user enters an invalid SQL query with syntax errors
    Then the system returns a validation error with the syntax issue description
    And the query is not executed

  # ---------------------------------------------------------------------------
  # RLS & Data Isolation
  # ---------------------------------------------------------------------------

  Scenario: Dashboard data respects Row-Level Security
    Given a user with role "Editor" belongs to org_id "org-456"
    When the user views a public dashboard with cross-org data
    Then only data for org_id "org-456" is visible
    And no data from other organizations is returned

  Scenario: HoldingAdmin sees aggregated data across subsidiaries
    Given a user with role "HoldingAdmin" manages a holding with 3 subsidiaries
    When the user views a dashboard with aggregation across subsidiaries
    Then data from all 3 subsidiaries is included in the aggregation
    And each row identifies its source organization
