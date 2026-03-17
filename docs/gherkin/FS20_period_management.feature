Feature: FS20 - Reporting Period & Deadline Management
  As a HoldingAdmin
  I want to create reporting periods with deadlines, track completion, and compare historical data
  So that the reporting cycle is structured, automated, and all subsidiaries submit on time

  # ---------------------------------------------------------------------------
  # Period Creation & Configuration
  # ---------------------------------------------------------------------------

  Scenario: HoldingAdmin creates a new reporting period
    Given an authenticated user with role "HoldingAdmin"
    When the HoldingAdmin creates a period with:
      | field               | value        |
      | name                | Q1 2025      |
      | type                | QUARTERLY    |
      | start_date          | 2025-01-01   |
      | submission_deadline | 2025-04-15   |
      | review_deadline     | 2025-04-30   |
      | period_code         | Q1-2025      |
    Then the period is created with state "OPEN"
    And the period is assigned to the holding
    And the period is visible to all subsidiaries

  Scenario Outline: HoldingAdmin creates periods of different types
    Given an authenticated user with role "HoldingAdmin"
    When the HoldingAdmin creates a period with type "<period_type>"
    Then the period is created successfully with type "<period_type>"

    Examples:
      | period_type |
      | MONTHLY     |
      | QUARTERLY   |
      | ANNUAL      |

  Scenario: Period code must be unique within the holding
    Given a period with code "Q1-2025" already exists
    When the HoldingAdmin attempts to create another period with code "Q1-2025"
    Then the system returns HTTP status 409
    And the response body contains "period code already exists"

  # ---------------------------------------------------------------------------
  # Period States
  # ---------------------------------------------------------------------------

  Scenario: Period transitions from OPEN to COLLECTING
    Given a period "Q1-2025" in state "OPEN"
    When the HoldingAdmin transitions the period to "COLLECTING"
    Then the period state changes to "COLLECTING"
    And assigned forms become fillable by Editors

  Scenario: Period transitions from COLLECTING to REVIEWING
    Given a period "Q1-2025" in state "COLLECTING"
    When the HoldingAdmin transitions the period to "REVIEWING"
    Then the period state changes to "REVIEWING"
    And HoldingAdmin can begin reviewing submitted reports

  Scenario: Period transitions from REVIEWING to CLOSED
    Given a period "Q1-2025" in state "REVIEWING"
    When the HoldingAdmin transitions the period to "CLOSED"
    Then the period state changes to "CLOSED"
    And the period is archived for historical comparison
    And no further submissions or modifications are allowed

  Scenario Outline: Invalid period state transitions are rejected
    Given a period in state "<current_state>"
    When the HoldingAdmin attempts to transition the period to "<target_state>"
    Then the transition is denied with HTTP status 422

    Examples:
      | current_state | target_state |
      | OPEN          | REVIEWING    |
      | OPEN          | CLOSED       |
      | COLLECTING    | OPEN         |
      | COLLECTING    | CLOSED       |
      | REVIEWING     | OPEN         |
      | REVIEWING     | COLLECTING   |
      | CLOSED        | OPEN         |
      | CLOSED        | COLLECTING   |
      | CLOSED        | REVIEWING    |

  # ---------------------------------------------------------------------------
  # Clone from Previous Period
  # ---------------------------------------------------------------------------

  Scenario: Clone period from previous period
    Given a closed period "Q4-2024" with assigned forms and PPTX templates
    When the HoldingAdmin clones "Q4-2024" to create "Q1-2025"
    Then a new period "Q1-2025" is created in state "OPEN"
    And all form assignments from "Q4-2024" are carried over to "Q1-2025"
    And all PPTX template assignments from "Q4-2024" are carried over
    And the HoldingAdmin can adjust deadlines and dates for "Q1-2025"

  Scenario: Clone period completes within performance target
    Given a period "Q4-2024" with 20 form assignments and 5 template assignments
    When the HoldingAdmin clones the period
    Then the clone operation completes in less than 2 minutes

  Scenario: Cloned period does not copy submitted data
    Given a closed period "Q4-2024" with submitted report data
    When the HoldingAdmin clones "Q4-2024" to create "Q1-2025"
    Then the new period "Q1-2025" has no submitted data
    And only structural assignments (forms, templates, company assignments) are copied

  # ---------------------------------------------------------------------------
  # Submission Deadline Management
  # ---------------------------------------------------------------------------

  Scenario: Forms auto-close after submission deadline
    Given a period "Q1-2025" with submission_deadline "2025-04-15"
    And form "FORM-001" is assigned to the period
    And the current date passes "2025-04-15"
    When the auto-close job runs
    Then form "FORM-001" state changes to "CLOSED"
    And Editors can no longer submit data through the form
    And no manual intervention is required

  Scenario: Late submission requires explicit HoldingAdmin override
    Given a period "Q1-2025" with submission_deadline "2025-04-15"
    And the deadline has passed
    And company "SUB-003" has not submitted
    When "SUB-003" Editor attempts to submit
    Then the submission is rejected with "submission deadline has passed"
    And the Editor sees a message to contact HoldingAdmin for override

  Scenario: HoldingAdmin grants late submission override
    Given a period "Q1-2025" with expired submission_deadline
    And company "SUB-003" has not submitted
    When the HoldingAdmin grants a submission override for "SUB-003"
    Then "SUB-003" Editor can submit their report
    And the submission is marked as "LATE" in the audit trail

  # ---------------------------------------------------------------------------
  # Review Deadline
  # ---------------------------------------------------------------------------

  Scenario: Review deadline is tracked for HoldingAdmin
    Given a period "Q1-2025" with review_deadline "2025-04-30"
    And 5 reports are in state "SUBMITTED" or "UNDER_REVIEW"
    When the current date is within 7 days of "2025-04-30"
    Then the HoldingAdmin receives a reminder notification
    And the notification lists the 5 reports still pending review

  # ---------------------------------------------------------------------------
  # Auto Notifications Before Deadline
  # ---------------------------------------------------------------------------

  Scenario: Notification sent 7 days before submission deadline
    Given a period "Q1-2025" with submission_deadline "2025-04-15"
    And companies "SUB-001" and "SUB-003" have not submitted (status DRAFT or no form filled)
    When the date reaches 7 days before "2025-04-15" (i.e. 2025-04-08)
    Then notifications are sent to Editors of "SUB-001" and "SUB-003"
    And the notification warns of the approaching deadline
    And companies that have already submitted do not receive the notification

  Scenario: Notification sent 3 days before submission deadline
    Given a period "Q1-2025" with submission_deadline "2025-04-15"
    And company "SUB-003" still has not submitted
    When the date reaches 3 days before "2025-04-15" (i.e. 2025-04-12)
    Then a notification is sent to the Editor of "SUB-003"
    And the notification indicates 3 days remaining

  Scenario: Notification sent 1 day before submission deadline
    Given a period "Q1-2025" with submission_deadline "2025-04-15"
    And company "SUB-003" still has not submitted
    When the date reaches 1 day before "2025-04-15" (i.e. 2025-04-14)
    Then an urgent notification is sent to the Editor of "SUB-003"
    And the notification indicates 1 day remaining

  Scenario: Notification days are configurable
    Given a period "Q1-2025" with submission_deadline "2025-04-15"
    When the HoldingAdmin configures notification days to 14, 7, 3, 1
    Then notifications are sent at 14, 7, 3, and 1 days before the deadline
    And the default configuration remains 7, 3, 1 if not customized

  Scenario: Submitted companies do not receive deadline notifications
    Given a period "Q1-2025" with submission_deadline "2025-04-15"
    And company "SUB-001" has submitted their report (state SUBMITTED or later)
    When the 7-day notification job runs
    Then "SUB-001" does not receive a deadline notification

  # ---------------------------------------------------------------------------
  # Escalation
  # ---------------------------------------------------------------------------

  Scenario: Escalation notification when company misses deadline
    Given a period "Q1-2025" with submission_deadline "2025-04-15"
    And companies "SUB-003" and "SUB-005" have not submitted by "2025-04-15"
    When the deadline passes
    Then the HoldingAdmin receives an escalation notification
    And the notification lists "SUB-003" and "SUB-005" as non-compliant
    And each non-compliant company's current status is included

  # ---------------------------------------------------------------------------
  # Period Dashboard & Completion Tracking
  # ---------------------------------------------------------------------------

  Scenario: Period dashboard displays company status matrix
    Given period "Q1-2025" with 5 assigned companies
    And "SUB-001" report is in state "APPROVED"
    And "SUB-002" report is in state "SUBMITTED"
    And "SUB-003" report is in state "DRAFT"
    And "SUB-004" report is in state "REJECTED"
    And "SUB-005" has not started (no report)
    When the HoldingAdmin opens the period dashboard for "Q1-2025"
    Then a matrix displays each company with color-coded status:
      | company | status    | color  |
      | SUB-001 | APPROVED  | green  |
      | SUB-002 | SUBMITTED | yellow |
      | SUB-003 | DRAFT     | grey   |
      | SUB-004 | REJECTED  | red    |
      | SUB-005 | NOT_STARTED | grey |

  Scenario: Period dashboard loads within performance target
    Given a period with 50+ assigned companies
    When the HoldingAdmin opens the period dashboard
    Then all company statuses load in less than 3 seconds

  Scenario: Completion percentage is calculated correctly
    Given period "Q1-2025" with 10 assigned companies
    And 4 reports are in state "APPROVED"
    When the HoldingAdmin views the period dashboard
    Then the completion percentage shows 40% (4 APPROVED / 10 total)

  Scenario: Period status export as PDF
    Given a period "Q1-2025" dashboard with 50+ company statuses
    When the HoldingAdmin exports the period status as PDF
    Then a PDF file is generated containing the company status matrix
    And the PDF includes the completion percentage and period metadata

  Scenario: Period status export as Excel
    Given a period "Q1-2025" dashboard with 50+ company statuses
    When the HoldingAdmin exports the period status as Excel
    Then an Excel file is generated with company statuses in tabular format
    And the Excel includes color-coded status columns

  Scenario: Period status export works for 50+ companies
    Given a period with 55 assigned companies in various states
    When the HoldingAdmin exports the period status
    Then the export completes successfully with all 55 companies included

  # ---------------------------------------------------------------------------
  # Historical & Comparison (Basic)
  # ---------------------------------------------------------------------------

  Scenario: Closed periods are archived for comparison
    Given period "Q1-2024" is in state "CLOSED"
    And period "Q1-2025" is in state "CLOSED"
    When the HoldingAdmin opens the comparison dashboard
    Then both "Q1-2024" and "Q1-2025" are available for selection

  Scenario: Compare same metric across two periods for same organization
    Given period "Q1-2024" has approved data with metric "total_opex_czk" = 5000000 for "SUB-001"
    And period "Q1-2025" has approved data with metric "total_opex_czk" = 5500000 for "SUB-001"
    When the HoldingAdmin compares "total_opex_czk" for "SUB-001" between "Q1-2024" and "Q1-2025"
    Then the comparison shows:
      | period  | value    |
      | Q1-2024 | 5000000  |
      | Q1-2025 | 5500000  |
    And the absolute delta is 500000
    And the percentage delta is +10.0%

  Scenario: Comparison limited to same period type
    Given period "Q1-2025" of type "QUARTERLY"
    And period "2024" of type "ANNUAL"
    When the HoldingAdmin attempts to compare "Q1-2025" with "2024"
    Then the comparison is denied
    And the response indicates "periods must be of the same type for comparison"

  Scenario: Comparison visualization as bar chart
    Given comparison data for "total_opex_czk" across "Q1-2024" and "Q1-2025"
    When the HoldingAdmin selects bar chart visualization
    Then a bar chart is rendered with bars for each period
    And the chart includes axis labels and value annotations

  Scenario: Comparison visualization as line chart
    Given comparison data for "total_opex_czk" across "Q1-2023", "Q1-2024", and "Q1-2025"
    When the HoldingAdmin selects line chart visualization
    Then a line chart is rendered with data points for each period
    And the trend line connects the data points chronologically

  Scenario: Comparison as table with delta values
    Given comparison data for "total_opex_czk" across "Q1-2024" and "Q1-2025" for 3 organizations
    When the HoldingAdmin views the comparison table
    Then the table displays:
      | organization | Q1-2024  | Q1-2025  | delta_abs | delta_pct |
      | SUB-001      | 5000000  | 5500000  | +500000   | +10.0%    |
      | SUB-002      | 3000000  | 2800000  | -200000   | -6.7%     |
      | SUB-003      | 4500000  | 4500000  | 0         | 0.0%      |

  # ---------------------------------------------------------------------------
  # Integration with FS14 Versioning
  # ---------------------------------------------------------------------------

  Scenario: Corrections within period create versions not overwrites
    Given period "Q1-2025" with an approved report for "SUB-001"
    And the report requires a correction
    When a correction is initiated via FS14 versioning
    Then a new version of the report is created within the same period
    And the original approved version remains intact
    And the comparison dashboard uses the latest approved version

  # ---------------------------------------------------------------------------
  # Edge Cases & Error Handling
  # ---------------------------------------------------------------------------

  Scenario: Creating a period with submission deadline before start date is rejected
    Given an authenticated HoldingAdmin
    When the HoldingAdmin creates a period with start_date "2025-04-01" and submission_deadline "2025-03-15"
    Then the system returns HTTP status 422
    And the response body contains "submission deadline must be after start date"

  Scenario: Creating a period with review deadline before submission deadline is rejected
    Given an authenticated HoldingAdmin
    When the HoldingAdmin creates a period with submission_deadline "2025-04-15" and review_deadline "2025-04-10"
    Then the system returns HTTP status 422
    And the response body contains "review deadline must be after submission deadline"

  Scenario: Non-HoldingAdmin cannot create periods
    Given an authenticated user with role "Editor"
    When the Editor attempts to create a new period
    Then the system returns HTTP status 403
    And the response body contains "insufficient permissions"

  Scenario: Deleting a period with submitted data is prevented
    Given a period "Q1-2025" with 3 submitted reports
    When the HoldingAdmin attempts to delete the period
    Then the deletion is denied
    And the response body contains "cannot delete period with existing submissions"
