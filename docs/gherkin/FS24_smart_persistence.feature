Feature: FS24 - Smart Persistence Promotion (Managed Tables)
  As a platform administrator
  I want the system to detect frequently used schema mappings and propose dedicated database tables
  So that high-volume data structures get optimized storage instead of generic JSONB persistence

  # ---------------------------------------------------------------------------
  # Frequency Detection
  # ---------------------------------------------------------------------------

  Scenario: System tracks schema mapping usage count
    Given a Schema Mapping "SM-OPEX-Monthly" exists in engine-data:template
    When a file is imported and routed through the mapping 1 time
    Then the usage counter for "SM-OPEX-Monthly" is incremented to reflect the new import

  Scenario: Schema mapping reaches promotion threshold
    Given a Schema Mapping "SM-OPEX-Monthly" has been used 4 times
    And the promotion threshold is configured as 5
    When a 5th file is imported using "SM-OPEX-Monthly"
    Then the mapping is marked as "CANDIDATE_FOR_PROMOTION"
    And a notification is created for the Admin

  Scenario: Schema mapping below threshold is not flagged
    Given a Schema Mapping "SM-Quarterly-Revenue" has been used 3 times
    And the promotion threshold is configured as 5
    When the system evaluates promotion candidates
    Then "SM-Quarterly-Revenue" is not marked as a candidate

  # ---------------------------------------------------------------------------
  # Admin Proposal Notification
  # ---------------------------------------------------------------------------

  Scenario: Admin sees promotion candidate notification in UI
    Given Schema Mapping "SM-OPEX-Monthly" is marked as "CANDIDATE_FOR_PROMOTION"
    And I am authenticated as a user with role "Admin"
    When I open the engine-core:admin dashboard
    Then a notification is displayed: "This template is used frequently. Create dedicated table for higher performance?"
    And the notification links to the promotion wizard for "SM-OPEX-Monthly"

  Scenario: Admin can dismiss a promotion candidate
    Given Schema Mapping "SM-OPEX-Monthly" is marked as "CANDIDATE_FOR_PROMOTION"
    And I am authenticated as a user with role "Admin"
    When I dismiss the promotion proposal
    Then the candidate status is cleared
    And the usage counter continues tracking for future re-evaluation

  # ---------------------------------------------------------------------------
  # Conversion Assistant - SQL Schema Proposal
  # ---------------------------------------------------------------------------

  Scenario: System generates SQL schema proposal from historical data
    Given Schema Mapping "SM-OPEX-Monthly" is marked as "CANDIDATE_FOR_PROMOTION"
    And 5 historical imports exist with consistent column structures
    When the conversion assistant analyzes the historical data
    Then a SQL schema proposal is generated with column names matching the mapping fields
    And data types are optimized based on actual data (e.g., NUMERIC instead of TEXT for numeric fields)

  Scenario: SQL proposal uses optimal types instead of JSONB
    Given the conversion assistant has analyzed historical data for "SM-OPEX-Monthly"
    When the schema proposal is generated
    Then columns containing numeric values are typed as NUMERIC or DECIMAL
    And columns containing dates are typed as DATE or TIMESTAMP
    And columns containing short text are typed as VARCHAR with appropriate length
    And no column uses JSONB type

  Scenario: SQL proposal includes index recommendations
    Given the conversion assistant has analyzed historical data for "SM-OPEX-Monthly"
    When the schema proposal is generated
    Then the proposal includes index recommendations for frequently queried columns
    And a primary key column is included in the proposal

  # ---------------------------------------------------------------------------
  # Admin Approval & Modification
  # ---------------------------------------------------------------------------

  Scenario: Admin reviews and modifies the SQL schema proposal
    Given a SQL schema proposal exists for "SM-OPEX-Monthly"
    And I am authenticated as a user with role "Admin"
    When I open the schema proposal in engine-core:admin
    Then I can see all proposed columns with their names and data types
    And I can modify field lengths, data types, and index definitions

  Scenario: Admin approves the schema proposal and table is created
    Given I am authenticated as a user with role "Admin"
    And I have reviewed the SQL schema proposal for "SM-OPEX-Monthly"
    When I confirm the proposal
    Then the system physically creates the new table in PostgreSQL
    And the table structure matches the approved schema
    And the creation event is logged in the audit trail

  Scenario: Table is not created without explicit admin approval
    Given a SQL schema proposal exists for "SM-OPEX-Monthly"
    And the Admin has not yet confirmed the proposal
    When the system checks the promotion status
    Then no table is created in PostgreSQL
    And the proposal remains in "PENDING_APPROVAL" status

  Scenario: Admin adds custom indexes before approval
    Given a SQL schema proposal exists for "SM-OPEX-Monthly"
    And I am authenticated as a user with role "Admin"
    When I add an index on columns "cost_center" and "period"
    And I confirm the proposal
    Then the created table includes the custom indexes
    And the indexes are active and usable for queries

  # ---------------------------------------------------------------------------
  # Transparent Routing
  # ---------------------------------------------------------------------------

  Scenario: Future imports are auto-routed to the new dedicated table
    Given a dedicated table has been created for Schema Mapping "SM-OPEX-Monthly"
    When a new file is imported using "SM-OPEX-Monthly"
    Then engine-orchestrator routes the data to the dedicated table instead of the generic Sink
    And the import completes without any frontend changes

  Scenario: Transparent routing does not require frontend modifications
    Given a dedicated table has been created for Schema Mapping "SM-OPEX-Monthly"
    When a user uploads a file through the standard upload UI
    And the file matches "SM-OPEX-Monthly" mapping
    Then the upload flow behaves identically from the user perspective
    And the data is persisted in the dedicated table transparently

  Scenario: Existing data in generic Sink remains accessible after promotion
    Given historical data for "SM-OPEX-Monthly" exists in the generic JSONB Sink
    And a dedicated table has been created for "SM-OPEX-Monthly"
    When a user queries data for "SM-OPEX-Monthly"
    Then both historical data from the generic Sink and new data from the dedicated table are returned
    And the data source is transparent to the querying user

  Scenario: Routing fallback if dedicated table is unavailable
    Given a dedicated table has been created for Schema Mapping "SM-OPEX-Monthly"
    And the dedicated table is temporarily unavailable
    When a new file is imported using "SM-OPEX-Monthly"
    Then engine-orchestrator falls back to the generic Sink for persistence
    And an alert is sent to the Admin about the routing fallback
