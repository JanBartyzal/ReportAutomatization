Feature: FS21 - Local Forms & Local PPTX Templates
  As a CompanyAdmin
  I want to create and manage local forms and PPTX templates scoped to my organization
  So that my org can produce internal reports independently of the central holding workflow

  # ---------------------------------------------------------------------------
  # Local Forms - Creation & Visibility
  # ---------------------------------------------------------------------------

  Scenario: CompanyAdmin creates a local form
    Given I am authenticated as a user with role "CompanyAdmin"
    When I create a new form with scope "LOCAL"
    Then the form is persisted with scope "LOCAL" and my org_id
    And the form status is "DRAFT"

  Scenario: Local form is visible only within the same org
    Given a local form exists for org_id "ORG-100"
    When a user from org_id "ORG-100" requests the form list
    Then the local form is included in the response
    When a user from org_id "ORG-200" requests the form list
    Then the local form is not included in the response

  Scenario: Local form is fillable only by users within the same org
    Given a local form with status "PUBLISHED" exists for org_id "ORG-100"
    When a user from org_id "ORG-100" submits a response to the form
    Then the submission is accepted and stored
    When a user from org_id "ORG-200" submits a response to the form
    Then the request is rejected with HTTP status 403

  Scenario: Local form data does not auto-flow to central reporting
    Given a local form with status "PUBLISHED" exists for org_id "ORG-100"
    And users have submitted responses to the local form
    When the central reporting aggregation runs
    Then the local form data is not included in the central report

  # ---------------------------------------------------------------------------
  # Local Forms - Lifecycle
  # ---------------------------------------------------------------------------

  Scenario Outline: Local form follows standard lifecycle without holding approval
    Given I am authenticated as a user with role "CompanyAdmin"
    And a local form exists with status "<current_status>"
    When I transition the form to "<target_status>"
    Then the form status is updated to "<target_status>"
    And no approval request is sent to HoldingAdmin

    Examples:
      | current_status | target_status |
      | DRAFT          | PUBLISHED     |
      | PUBLISHED      | CLOSED        |

  Scenario: CompanyAdmin cannot publish a local form without required fields
    Given I am authenticated as a user with role "CompanyAdmin"
    And a local form in status "DRAFT" is missing required field definitions
    When I transition the form to "PUBLISHED"
    Then the request is rejected with a validation error

  # ---------------------------------------------------------------------------
  # Release Data to Holding
  # ---------------------------------------------------------------------------

  Scenario: CompanyAdmin marks local form data as RELEASED
    Given I am authenticated as a user with role "CompanyAdmin"
    And a local form with status "CLOSED" has collected responses
    When I mark the form data as "RELEASED"
    Then the form data status is updated to "RELEASED"
    And a notification is sent to HoldingAdmin

  Scenario: HoldingAdmin receives notification about released local data
    Given a CompanyAdmin has marked local form data as "RELEASED" for org_id "ORG-100"
    When HoldingAdmin views the notification inbox
    Then a notification indicates that org_id "ORG-100" has released local data
    And the notification contains a link to review the data

  Scenario: HoldingAdmin manually includes released data in central reporting
    Given I am authenticated as a user with role "HoldingAdmin"
    And local form data from org_id "ORG-100" has status "RELEASED"
    When I choose to include the released data in the central report
    Then the data is pulled into the central reporting dataset
    And the inclusion is logged in the audit trail

  Scenario: Released data is not auto-pushed to central reporting
    Given a CompanyAdmin has marked local form data as "RELEASED"
    When the central reporting aggregation runs
    Then the released data is not automatically included
    And the data remains available for manual pull by HoldingAdmin

  # ---------------------------------------------------------------------------
  # Local PPTX Templates
  # ---------------------------------------------------------------------------

  Scenario: CompanyAdmin uploads a local PPTX template
    Given I am authenticated as a user with role "CompanyAdmin"
    When I upload a PPTX template file with scope "LOCAL"
    Then the template is stored with scope "LOCAL" and my org_id
    And the template is available in my org template list

  Scenario: Generator creates PPTX from local template for internal reports
    Given a local PPTX template exists for org_id "ORG-100"
    And form data is available for the template placeholders
    When processor-generators:pptx generates a report using the local template
    Then a PPTX report is produced using the local template layout
    And the generated report is scoped to org_id "ORG-100"

  Scenario: Generated local report is not auto-shared with holding
    Given a PPTX report has been generated from a local template for org_id "ORG-100"
    When HoldingAdmin views the central report repository
    Then the locally generated report is not listed

  # ---------------------------------------------------------------------------
  # Sharing Between CompanyAdmins
  # ---------------------------------------------------------------------------

  Scenario: CompanyAdmin shares a local template with another CompanyAdmin in same holding
    Given I am authenticated as a user with role "CompanyAdmin" in org_id "ORG-100"
    And a local PPTX template exists in my org
    When I share the template with CompanyAdmin of org_id "ORG-200" within the same holding
    Then the template scope is updated to "SHARED_WITHIN_HOLDING"
    And the template is visible to CompanyAdmin of org_id "ORG-200"

  Scenario: CompanyAdmin shares a local form with another CompanyAdmin in same holding
    Given I am authenticated as a user with role "CompanyAdmin" in org_id "ORG-100"
    And a local form exists in my org
    When I share the form with CompanyAdmin of org_id "ORG-200" within the same holding
    Then the form scope is updated to "SHARED_WITHIN_HOLDING"
    And the form is visible to CompanyAdmin of org_id "ORG-200"

  Scenario: Sharing is restricted to the same holding
    Given I am authenticated as a user with role "CompanyAdmin" in org_id "ORG-100" under holding "HOLD-A"
    And a local template exists in my org
    When I attempt to share the template with CompanyAdmin of org_id "ORG-300" under holding "HOLD-B"
    Then the request is rejected with HTTP status 403

  # ---------------------------------------------------------------------------
  # HoldingAdmin Overview
  # ---------------------------------------------------------------------------

  Scenario: HoldingAdmin has overview of all local templates and forms
    Given I am authenticated as a user with role "HoldingAdmin"
    And local templates exist for org_id "ORG-100" and org_id "ORG-200"
    And shared templates exist with scope "SHARED_WITHIN_HOLDING"
    When I request the full template and form overview
    Then all local and shared templates are listed with their scope and org_id
    And all local and shared forms are listed with their scope and org_id
