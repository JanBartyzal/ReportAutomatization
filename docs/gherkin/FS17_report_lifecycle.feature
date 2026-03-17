Feature: FS17 - OPEX Report Lifecycle & Submission Workflow
  As a HoldingAdmin or Editor
  I want a structured report lifecycle with state transitions, audit trail, and bulk actions
  So that OPEX reports flow through a controlled review-approval process with full traceability

  # ---------------------------------------------------------------------------
  # Report Entity & Uniqueness
  # ---------------------------------------------------------------------------

  Scenario: System enforces one report per organization per period
    Given an organization "SUB-001" and a period "Q1-2025"
    And a report of type "OPEX" already exists for "SUB-001" in "Q1-2025"
    When an Editor attempts to create another "OPEX" report for "SUB-001" in "Q1-2025"
    Then the system returns HTTP status 409
    And the response body contains "report already exists for this organization and period"

  Scenario: Report is created in DRAFT state
    Given an authenticated Editor for organization "SUB-001"
    And period "Q1-2025" is OPEN
    When the Editor creates a new OPEX report for period "Q1-2025"
    Then the report is persisted with state "DRAFT"
    And the report is bound to org_id "SUB-001", period_id "Q1-2025", and report_type "OPEX"
    And an audit log entry is created with action "CREATED" and state "DRAFT"

  # ---------------------------------------------------------------------------
  # State Transitions - Happy Path
  # ---------------------------------------------------------------------------

  Scenario: Editor submits a complete report (DRAFT -> SUBMITTED)
    Given an OPEX report in state "DRAFT" for organization "SUB-001"
    And all required fields are filled
    And all required sheets are uploaded
    And data passes all validation rules
    When the Editor transitions the report to "SUBMITTED"
    Then the report state changes to "SUBMITTED"
    And an audit log entry is created with from_state "DRAFT", to_state "SUBMITTED", user_id, and timestamp
    And a Dapr PubSub event "report.status_changed" is published by engine-reporting:lifecycle

  Scenario: HoldingAdmin accepts report for review (SUBMITTED -> UNDER_REVIEW)
    Given an OPEX report in state "SUBMITTED" for organization "SUB-001"
    And the authenticated user has role "HoldingAdmin"
    When the HoldingAdmin transitions the report to "UNDER_REVIEW"
    Then the report state changes to "UNDER_REVIEW"
    And an audit log entry is created with from_state "SUBMITTED", to_state "UNDER_REVIEW", user_id, and timestamp
    And a Dapr PubSub event "report.status_changed" is published

  Scenario: HoldingAdmin approves the report (UNDER_REVIEW -> APPROVED)
    Given an OPEX report in state "UNDER_REVIEW" for organization "SUB-001"
    And the authenticated user has role "HoldingAdmin"
    When the HoldingAdmin transitions the report to "APPROVED"
    Then the report state changes to "APPROVED"
    And the report data is included in central reporting
    And an audit log entry is created with from_state "UNDER_REVIEW", to_state "APPROVED", user_id, and timestamp
    And a Dapr PubSub event "report.status_changed" is published

  Scenario: HoldingAdmin rejects the report with mandatory comment (UNDER_REVIEW -> REJECTED)
    Given an OPEX report in state "UNDER_REVIEW" for organization "SUB-001"
    And the authenticated user has role "HoldingAdmin"
    When the HoldingAdmin transitions the report to "REJECTED" with comment "Missing Q1 depreciation breakdown"
    Then the report state changes to "REJECTED"
    And the rejection comment "Missing Q1 depreciation breakdown" is stored and visible to the Editor
    And an audit log entry is created with from_state "UNDER_REVIEW", to_state "REJECTED", user_id, timestamp, and comment
    And a Dapr PubSub event "report.status_changed" is published
    And engine-orchestrator sends a notification to the Editor via FS13 containing the rejection comment

  Scenario: Rejected report returns to DRAFT for resubmission (REJECTED -> DRAFT)
    Given an OPEX report in state "REJECTED" for organization "SUB-001"
    When the Editor opens the rejected report for correction
    Then the report state transitions to "DRAFT"
    And the Editor can edit the report data
    And the rejection comment from the previous review remains visible
    And an audit log entry is created with from_state "REJECTED", to_state "DRAFT", user_id, and timestamp

  # ---------------------------------------------------------------------------
  # Submission Checklist & Validation
  # ---------------------------------------------------------------------------

  Scenario: Editor cannot submit report with missing required fields
    Given an OPEX report in state "DRAFT" for organization "SUB-001"
    And the field "total_opex_czk" is empty
    And the field "total_opex_czk" is marked as required
    When the Editor attempts to transition the report to "SUBMITTED"
    Then the transition is denied
    And the response contains a checklist with "total_opex_czk" marked as incomplete
    And the report remains in state "DRAFT"

  Scenario: Editor cannot submit report with missing sheet uploads
    Given an OPEX report in state "DRAFT" for organization "SUB-001"
    And the required sheet "personnel_costs" has not been uploaded
    When the Editor attempts to transition the report to "SUBMITTED"
    Then the transition is denied
    And the response contains a checklist indicating "personnel_costs" sheet is missing
    And the report remains in state "DRAFT"

  Scenario: Editor cannot submit report failing validation rules
    Given an OPEX report in state "DRAFT" for organization "SUB-001"
    And the field "total_opex_czk" has value -500
    And the validation rule requires "total_opex_czk" to be >= 0
    When the Editor attempts to transition the report to "SUBMITTED"
    Then the transition is denied
    And the response contains validation error "total_opex_czk must be >= 0"
    And the report remains in state "DRAFT"

  Scenario: Submission checklist reports 100% completeness before allowing transition
    Given an OPEX report in state "DRAFT" for organization "SUB-001"
    When the Editor requests the submission checklist
    Then the checklist includes status for all required fields
    And the checklist includes status for all required sheet uploads
    And the checklist includes status for all validation rules
    And the checklist shows an overall completeness percentage
    And the report can only transition to "SUBMITTED" when completeness is 100%

  # ---------------------------------------------------------------------------
  # Invalid State Transitions
  # ---------------------------------------------------------------------------

  Scenario Outline: System rejects invalid state transitions
    Given an OPEX report in state "<current_state>"
    When a user attempts to transition the report to "<target_state>"
    Then the transition is denied with HTTP status 422
    And the response body contains "invalid state transition from <current_state> to <target_state>"

    Examples:
      | current_state | target_state  |
      | DRAFT         | APPROVED      |
      | DRAFT         | UNDER_REVIEW  |
      | DRAFT         | REJECTED      |
      | SUBMITTED     | APPROVED      |
      | SUBMITTED     | DRAFT         |
      | UNDER_REVIEW  | SUBMITTED     |
      | APPROVED      | SUBMITTED     |
      | APPROVED      | UNDER_REVIEW  |
      | APPROVED      | REJECTED      |

  # ---------------------------------------------------------------------------
  # Rejection Requires Comment
  # ---------------------------------------------------------------------------

  Scenario: Rejection without comment is denied
    Given an OPEX report in state "UNDER_REVIEW"
    And the authenticated user has role "HoldingAdmin"
    When the HoldingAdmin transitions the report to "REJECTED" without a comment
    Then the transition is denied
    And the response body contains "rejection comment is mandatory"

  # ---------------------------------------------------------------------------
  # Permission Enforcement
  # ---------------------------------------------------------------------------

  Scenario: Editor cannot approve a report
    Given an OPEX report in state "UNDER_REVIEW"
    And the authenticated user has role "Editor"
    When the Editor attempts to transition the report to "APPROVED"
    Then the transition is denied with HTTP status 403
    And the response body contains "insufficient permissions"

  Scenario: Editor from different organization cannot submit report
    Given an OPEX report in state "DRAFT" for organization "SUB-001"
    And the authenticated user is an Editor for organization "SUB-002"
    When the Editor attempts to transition the report to "SUBMITTED"
    Then the transition is denied with HTTP status 403
    And the response body contains "insufficient permissions"

  Scenario: Viewer cannot perform any state transitions
    Given an OPEX report in state "DRAFT"
    And the authenticated user has role "Viewer"
    When the Viewer attempts to transition the report to "SUBMITTED"
    Then the transition is denied with HTTP status 403

  # ---------------------------------------------------------------------------
  # Audit Trail
  # ---------------------------------------------------------------------------

  Scenario: Every state transition is logged in audit
    Given an OPEX report that has transitioned through DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED
    When a user requests the audit history for the report
    Then the audit log contains 4 entries including the initial creation
    And each entry contains user_id, from_state, to_state, timestamp, and optional comment
    And entries are ordered chronologically

  Scenario: Full state transition history is viewable as timeline in UI
    Given an OPEX report with multiple state transitions
    When a user views the report detail page
    Then a timeline view displays all state transitions
    And each timeline entry shows the user name, transition, timestamp, and comment if present

  # ---------------------------------------------------------------------------
  # Data Lock After Approval
  # ---------------------------------------------------------------------------

  Scenario: Approved report data becomes read-only
    Given an OPEX report in state "APPROVED" for organization "SUB-001"
    When an Editor attempts to modify a data field in the report
    Then the modification is denied
    And the response body contains "report is locked in APPROVED state"

  Scenario: Unlocking approved report creates a new version via FS14
    Given an OPEX report in state "APPROVED" for organization "SUB-001"
    And the authenticated user has role "HoldingAdmin"
    When the HoldingAdmin initiates a correction on the approved report
    Then a new version of the report is created via FS14 versioning
    And the new version starts in state "DRAFT"
    And the previous approved version remains read-only and archived
    And an audit log entry records the version creation

  # ---------------------------------------------------------------------------
  # HoldingAdmin Dashboard
  # ---------------------------------------------------------------------------

  Scenario: HoldingAdmin views report status matrix
    Given reports exist for companies "SUB-001", "SUB-002", "SUB-003" in period "Q1-2025"
    And "SUB-001" report is in state "APPROVED"
    And "SUB-002" report is in state "SUBMITTED"
    And "SUB-003" report is in state "DRAFT"
    When the HoldingAdmin opens the period dashboard for "Q1-2025"
    Then a matrix displays rows for each company and columns for each period
    And "SUB-001" shows status "APPROVED"
    And "SUB-002" shows status "SUBMITTED"
    And "SUB-003" shows status "DRAFT"

  # ---------------------------------------------------------------------------
  # Bulk Actions
  # ---------------------------------------------------------------------------

  Scenario: HoldingAdmin bulk-approves multiple reports
    Given 3 OPEX reports in state "UNDER_REVIEW" for period "Q1-2025"
    And the authenticated user has role "HoldingAdmin"
    When the HoldingAdmin selects all 3 reports and triggers bulk "APPROVE"
    Then all 3 reports transition to state "APPROVED"
    And 3 audit log entries are created, one per report
    And 3 Dapr PubSub events "report.status_changed" are published
    And all 3 reports are included in central reporting

  Scenario: HoldingAdmin bulk-rejects multiple reports with comment
    Given 2 OPEX reports in state "UNDER_REVIEW" for period "Q1-2025"
    And the authenticated user has role "HoldingAdmin"
    When the HoldingAdmin selects both reports and triggers bulk "REJECT" with comment "Incomplete cost allocation"
    Then both reports transition to state "REJECTED"
    And the rejection comment is stored for both reports
    And notifications are sent to both Editors via FS13

  Scenario: Bulk action skips reports not eligible for transition
    Given 3 OPEX reports for period "Q1-2025"
    And report 1 is in state "UNDER_REVIEW"
    And report 2 is in state "DRAFT"
    And report 3 is in state "UNDER_REVIEW"
    When the HoldingAdmin selects all 3 and triggers bulk "APPROVE"
    Then report 1 and report 3 transition to "APPROVED"
    And report 2 is skipped with reason "invalid state transition"
    And the response includes a summary of successful and skipped transitions

  # ---------------------------------------------------------------------------
  # engine-orchestrator Event Handling
  # ---------------------------------------------------------------------------

  Scenario: engine-orchestrator triggers notification on rejection
    Given an OPEX report transitions from "UNDER_REVIEW" to "REJECTED"
    And engine-reporting:lifecycle publishes event "report.status_changed" via Dapr PubSub
    When engine-orchestrator receives the event
    Then engine-orchestrator sends a notification to the responsible Editor via FS13
    And the notification includes the rejection comment and report identifier

  Scenario: engine-orchestrator triggers PPTX generation on approval
    Given an OPEX report transitions from "UNDER_REVIEW" to "APPROVED"
    And engine-reporting:lifecycle publishes event "report.status_changed" via Dapr PubSub
    When engine-orchestrator receives the event
    Then engine-orchestrator triggers automatic inclusion in central reporting
    And engine-orchestrator may trigger PPTX generation if a template is assigned

  Scenario: engine-orchestrator triggers auto-checks on submission
    Given an OPEX report transitions from "DRAFT" to "SUBMITTED"
    And engine-reporting:lifecycle publishes event "report.status_changed" via Dapr PubSub
    When engine-orchestrator receives the event
    Then engine-orchestrator orchestrates automated validation checks
    And engine-orchestrator sends a notification to HoldingAdmin that a new report is awaiting review
