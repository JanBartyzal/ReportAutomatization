Feature: FS16 - Audit & Compliance Log
  As a compliance officer
  I want an immutable, append-only audit trail of all platform actions
  So that every data access, edit, AI interaction, and state transition is traceable for security audits

  # ---------------------------------------------------------------------------
  # Immutable Logs - Append-Only
  # ---------------------------------------------------------------------------

  Scenario: Every user action is recorded in the audit log
    Given a user with role "Editor" is authenticated
    When the user performs any action on the platform
    Then an audit log entry is created with who, when, and what
    And the entry includes the user ID, timestamp, action type, and affected resource

  Scenario: Audit log table is append-only for application user
    Given the application database user connects to PostgreSQL
    When the application attempts to UPDATE an existing audit log entry
    Then the database rejects the operation
    And the original audit log entry remains unchanged

  Scenario: Audit log table prevents DELETE for application user
    Given the application database user connects to PostgreSQL
    When the application attempts to DELETE an audit log entry
    Then the database rejects the operation
    And no audit log entries are removed

  Scenario: Audit log entries are INSERT-only
    Given an Editor edits OPEX data
    When the audit system records the action
    Then a new row is inserted into the audit_log table
    And the row contains user_id, action "DATA_EDITED", resource_id, resource_type, timestamp, and details

  # ---------------------------------------------------------------------------
  # Read Access Log
  # ---------------------------------------------------------------------------

  Scenario: Viewing a sensitive report is logged
    Given a user with role "Viewer" opens a financial report
    When the report data is fetched from the API
    Then the read access log records the User ID, Document ID, client IP address, and timestamp

  Scenario: Every view of a sensitive document creates a separate log entry
    Given a user opens the same financial report 3 times
    When each view request is processed
    Then 3 separate read access log entries are created
    And each entry has a distinct timestamp

  Scenario: Read access log captures IP address
    Given a user accesses a report from IP address "10.0.1.50"
    When the report is displayed
    Then the read access log entry includes IP "10.0.1.50"

  Scenario: Read access log is scoped by organization
    Given an Admin of org_id "org-100" queries the read access log
    When the admin requests read access logs
    Then only log entries for resources within org_id "org-100" are returned

  # ---------------------------------------------------------------------------
  # AI Audit - Prompt and Response Logging
  # ---------------------------------------------------------------------------

  Scenario: Every AI prompt is logged for review
    Given a user triggers an AI query via processor-atomizers:ai
    When the AI processes the prompt
    Then the audit log records the full prompt text, user ID, and timestamp

  Scenario: Every AI response is logged for review
    Given an AI query has been processed
    When processor-atomizers:ai returns the response
    Then the audit log records the full response text, model identifier, and token count

  Scenario: AI audit log supports hallucination review
    Given an AI response was flagged by a reviewer as potentially containing a hallucination
    When the reviewer searches the AI audit log for the response
    Then the log entry contains the original prompt, the AI response, the model used, and the timestamp
    And the reviewer can add a "hallucination_flag" annotation to the entry

  Scenario: AI audit log supports data leak review
    Given an AI response is reviewed for potential data leakage
    When the reviewer queries the AI audit log filtered by org_id
    Then all AI prompts and responses for that org_id are returned
    And the reviewer can verify no cross-org data appeared in responses

  # ---------------------------------------------------------------------------
  # State Transition Auditing (FS17 Integration)
  # ---------------------------------------------------------------------------

  Scenario: Report state transition is audited
    Given a report transitions from "DRAFT" to "SUBMITTED"
    When engine-reporting:lifecycle publishes the state change event
    Then the audit log records the transition with from_state, to_state, user_id, and timestamp

  Scenario: Report rejection with comment is audited
    Given a HoldingAdmin rejects a report with comment "Incomplete IT data"
    When the report transitions from "UNDER_REVIEW" to "REJECTED"
    Then the audit log records the transition
    And the audit entry includes the rejection comment "Incomplete IT data"

  Scenario: All state transitions for a report are traceable
    Given a report has gone through states DRAFT, SUBMITTED, REJECTED, DRAFT, SUBMITTED, APPROVED
    When an auditor queries the audit log for the report
    Then 5 state transition entries are returned in chronological order
    And each entry includes the acting user and timestamp

  # ---------------------------------------------------------------------------
  # Form Action Auditing (FS19 Integration)
  # ---------------------------------------------------------------------------

  Scenario: Form field change is audited
    Given an Editor is filling out an OPEX form
    When the Editor changes the value of field "IT costs" from 500000 to 600000
    Then the audit log records the field change with old_value, new_value, field_name, and user_id

  Scenario: Comment added to form is audited
    Given an Editor adds a comment "Q1 estimate adjusted" to a form field
    When the comment is saved
    Then the audit log records the action "COMMENT_ADDED" with the comment text and field reference

  Scenario: Excel import confirmation is audited
    Given an Editor imports an Excel file into a form via POST /map/excel-to-form
    When the Editor confirms the import
    Then the audit log records the action "IMPORT_CONFIRMED"
    And the entry includes the source file name, mapped fields count, and user_id

  # ---------------------------------------------------------------------------
  # Export - CSV/JSON for Security Audit
  # ---------------------------------------------------------------------------

  Scenario: Admin exports audit log as CSV
    Given a user with role "Admin" is authenticated
    When the admin requests an audit log export in CSV format
    Then the system generates a CSV file containing all audit entries for the admin's org_id
    And the CSV includes columns: timestamp, user_id, action, resource_type, resource_id, details, ip_address

  Scenario: Admin exports audit log as JSON
    Given a user with role "Admin" is authenticated
    When the admin requests an audit log export in JSON format
    Then the system generates a JSON file containing all audit entries for the admin's org_id
    And each entry is a complete JSON object with all audit fields

  Scenario: Audit log export supports date range filtering
    Given a user with role "Admin" requests an audit log export
    When the admin specifies a date range from "2026-01-01" to "2026-03-31"
    Then the export contains only entries within Q1 2026
    And entries outside the date range are excluded

  Scenario: Audit log export supports filtering by action type
    Given a user with role "Admin" requests an audit log export
    When the admin filters by action type "DATA_EDITED"
    Then the export contains only entries with action "DATA_EDITED"

  Scenario: HoldingAdmin exports audit log across subsidiaries
    Given a user with role "HoldingAdmin" manages 3 subsidiaries
    When the HoldingAdmin requests an audit log export for the entire holding
    Then the export includes entries from all 3 subsidiaries
    And each entry identifies its source organization

  # ---------------------------------------------------------------------------
  # Security & Integrity
  # ---------------------------------------------------------------------------

  Scenario: Audit log entries include correlation ID for traceability
    Given a user performs an action that triggers multiple microservices
    When each service writes its audit log entry
    Then all entries share the same correlation ID from the original request
    And the full action chain is traceable end-to-end

  Scenario: Viewer cannot access audit logs
    Given a user with role "Viewer" is authenticated
    When the user attempts to access the audit log API
    Then the system returns HTTP status 403

  Scenario: Audit log entries are protected by RLS
    Given audit log entries exist for org_id "org-100" and org_id "org-200"
    And a user belongs to org_id "org-100"
    When the user queries the audit log
    Then only entries for org_id "org-100" are returned
    And no entries from org_id "org-200" are included
