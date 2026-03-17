Feature: FS13 - Notification Center & Alerts
  As a platform user
  I want to receive timely in-app and email notifications about processing events and report lifecycle changes
  So that I stay informed about import results, errors, deadlines, and approval workflows

  # ---------------------------------------------------------------------------
  # In-App Notifications - WebSocket/SSE
  # ---------------------------------------------------------------------------

  Scenario: User receives in-app notification on processing completion
    Given a user with role "Editor" is authenticated and connected via WebSocket/SSE
    And the user has uploaded a file that is being processed
    When the processing pipeline completes successfully
    Then engine-reporting:notification pushes a real-time notification to the user's session
    And the notification contains the file name and processing status "completed"

  Scenario: User receives in-app notification on processing error
    Given a user with role "Editor" is authenticated and connected via WebSocket/SSE
    And the user has uploaded a file that is being processed
    When the processing pipeline encounters an error
    Then engine-reporting:notification pushes a real-time notification with status "error"
    And the notification contains the error description

  Scenario: User receives notification when WebSocket/SSE reconnects
    Given a user was disconnected from the WebSocket/SSE connection
    And 3 notifications were generated during the disconnection
    When the user reconnects
    Then the system delivers all 3 missed notifications
    And the notifications are ordered by timestamp

  # ---------------------------------------------------------------------------
  # Email Notifications - SMTP
  # ---------------------------------------------------------------------------

  Scenario: Critical error triggers email notification
    Given a user has email notifications enabled for critical errors
    When a processing pipeline fails with a critical error for the user's file
    Then engine-reporting:notification sends an email via SMTP to the user's registered email address
    And the email subject contains "Critical Error" and the file name

  Scenario: Batch job completion triggers email notification
    Given a user with role "Admin" has email notifications enabled for batch completions
    When a batch processing job completes
    Then engine-reporting:notification sends an email summarizing the batch results
    And the email includes the count of successful and failed items

  # ---------------------------------------------------------------------------
  # Granular Notification Settings
  # ---------------------------------------------------------------------------

  Scenario: User opts out of import notifications
    Given a user is authenticated
    When the user sets notification preference "import_completed" to "opt-out"
    Then the preference is saved for the user
    And the user no longer receives notifications for import completions

  Scenario: User opts in to parsing failure notifications
    Given a user is authenticated
    And the user previously opted out of "parsing_failed" notifications
    When the user sets notification preference "parsing_failed" to "opt-in"
    Then the preference is saved for the user
    And the user begins receiving notifications for parsing failures

  Scenario: Organization-level notification settings override user defaults
    Given an Admin sets organization-level notification preference "report_ready" to "opt-out"
    When a new user is created in the organization
    Then the user inherits the organization default of "opt-out" for "report_ready"
    And the user can override the organization default in their personal settings

  Scenario Outline: Granular settings per event type
    Given a user is authenticated
    When the user sets notification preference for event "<event_type>" to "<preference>"
    Then the preference is saved
    And the system respects the preference for future "<event_type>" events

    Examples:
      | event_type        | preference |
      | import_completed  | opt-in     |
      | parsing_failed    | opt-out    |
      | report_ready      | opt-in     |
      | report_submitted  | opt-out    |
      | deadline_missed   | opt-in     |

  # ---------------------------------------------------------------------------
  # Notification Triggers from FS17 - State Transitions
  # ---------------------------------------------------------------------------

  Scenario: REPORT_SUBMITTED notification sent to HoldingAdmin
    Given an Editor submits a report transitioning it to "SUBMITTED"
    And the HoldingAdmin has "report_submitted" notifications enabled
    When engine-reporting:lifecycle publishes a "report.status_changed" event
    Then engine-reporting:notification sends an in-app notification to the HoldingAdmin
    And the notification contains the report name, organization, and new status "SUBMITTED"

  Scenario: REPORT_APPROVED notification sent to Editor
    Given a HoldingAdmin approves a report transitioning it to "APPROVED"
    And the report's Editor has "report_approved" notifications enabled
    When engine-reporting:lifecycle publishes a "report.status_changed" event
    Then engine-reporting:notification sends an in-app notification to the Editor
    And the notification contains the report name and new status "APPROVED"

  Scenario: REPORT_REJECTED notification sent to Editor with comment
    Given a HoldingAdmin rejects a report with comment "Missing Q1 IT costs"
    And the report's Editor has "report_rejected" notifications enabled
    When engine-reporting:lifecycle publishes a "report.status_changed" event
    Then engine-reporting:notification sends an in-app notification to the Editor
    And the notification contains the rejection comment "Missing Q1 IT costs"
    And an email notification is also sent to the Editor

  # ---------------------------------------------------------------------------
  # Notification Triggers from FS20 - Deadlines & Escalations
  # ---------------------------------------------------------------------------

  Scenario: DEADLINE_APPROACHING notification sent 3 days before deadline
    Given a reporting period has a deadline of "2026-03-20"
    And an Editor has not yet submitted the report
    And the current date is "2026-03-17"
    When the deadline check job runs
    Then engine-reporting:notification sends a "DEADLINE_APPROACHING" notification to the Editor
    And the notification includes the deadline date and days remaining

  Scenario: DEADLINE_MISSED notification sent after deadline passes
    Given a reporting period had a deadline of "2026-03-15"
    And an Editor has not submitted the report
    And the current date is "2026-03-16"
    When the deadline check job runs
    Then engine-reporting:notification sends a "DEADLINE_MISSED" notification to the Editor
    And engine-reporting:notification sends a "DEADLINE_MISSED" escalation notification to the HoldingAdmin
    And the notification identifies the organization and missed period

  # ---------------------------------------------------------------------------
  # Extended Notification Types
  # ---------------------------------------------------------------------------

  Scenario Outline: System sends correct notification for each extended type
    Given the event "<event_type>" is triggered
    And the target user has notifications enabled for "<event_type>"
    When engine-reporting:notification processes the event
    Then the user receives an in-app notification of type "<event_type>"
    And the notification payload includes "<expected_field>"

    Examples:
      | event_type           | expected_field      |
      | REPORT_SUBMITTED     | report_id           |
      | REPORT_APPROVED      | report_id           |
      | REPORT_REJECTED      | rejection_comment   |
      | DEADLINE_APPROACHING | deadline_date       |
      | DEADLINE_MISSED      | missed_deadline     |

  # ---------------------------------------------------------------------------
  # Notification Delivery via Dapr Pub/Sub
  # ---------------------------------------------------------------------------

  Scenario: engine-reporting:notification receives events via Dapr Pub/Sub
    Given engine-orchestrator publishes a notification event to the "notify" topic
    When engine-reporting:notification subscribes to the "notify" topic via Dapr Pub/Sub
    Then engine-reporting:notification processes the event
    And delivers the notification to the target user via the configured channel

  Scenario: engine-reporting:notification pushes real-time notification to frontend via WebSocket/SSE
    Given a notification event is processed by engine-reporting:notification
    And the target user is connected to the frontend
    When engine-reporting:notification pushes the notification via WebSocket/SSE
    Then frontend receives the notification in real time
    And the notification badge count is incremented in the UI
