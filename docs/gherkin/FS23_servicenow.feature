Feature: FS23 - ServiceNow API Integration & Automation
  As an IT operations manager
  I want automated data pulls from ServiceNow with report generation and distribution
  So that IT ticketing and asset management data is available for BI analysis without manual export

  # ---------------------------------------------------------------------------
  # API Integration
  # ---------------------------------------------------------------------------

  Scenario: engine-integrations:servicenow connects to ServiceNow REST API
    Given ServiceNow instance URL is configured in engine-integrations:servicenow
    And valid credentials are stored in Azure KeyVault
    When engine-integrations:servicenow initiates a connection to ServiceNow
    Then the connection is established successfully
    And the available ServiceNow tables are retrieved

  Scenario: engine-integrations:servicenow pulls IT ticket data from ServiceNow
    Given engine-integrations:servicenow is connected to ServiceNow
    When a data pull is triggered for the "incident" table
    Then incident records are retrieved from ServiceNow
    And the records are stored in the platform data store

  Scenario: engine-integrations:servicenow pulls asset management data from ServiceNow
    Given engine-integrations:servicenow is connected to ServiceNow
    When a data pull is triggered for the "cmdb_ci" table
    Then asset records are retrieved from ServiceNow
    And the records are stored in the platform data store

  # ---------------------------------------------------------------------------
  # Authentication & Security
  # ---------------------------------------------------------------------------

  Scenario: OAuth2 credentials are retrieved from Azure KeyVault
    Given ServiceNow OAuth2 client_id and client_secret are stored in Azure KeyVault
    When engine-integrations:servicenow starts a data pull
    Then credentials are dynamically retrieved from Azure KeyVault
    And the OAuth2 token exchange is completed successfully

  Scenario: Basic auth credentials are retrieved from Azure KeyVault
    Given ServiceNow basic auth username and password are stored in Azure KeyVault
    When engine-integrations:servicenow starts a data pull with auth type "BASIC"
    Then credentials are dynamically retrieved from Azure KeyVault
    And the request includes a valid Basic Authorization header

  Scenario: Communication with ServiceNow is encrypted
    Given engine-integrations:servicenow is configured to connect to ServiceNow
    When a data pull is executed
    Then all HTTP communication uses TLS 1.2 or higher
    And no credentials are transmitted in plain text

  Scenario: Connection fails gracefully when credentials are missing
    Given ServiceNow credentials are not found in Azure KeyVault
    When engine-integrations:servicenow attempts to start a data pull
    Then the pull fails with error "CREDENTIALS_NOT_FOUND"
    And an alert is sent via engine-reporting:notification

  # ---------------------------------------------------------------------------
  # Scheduler
  # ---------------------------------------------------------------------------

  Scenario Outline: Configurable data pull intervals
    Given engine-integrations:servicenow scheduler is configured with interval "<interval>"
    When the scheduled time arrives
    Then a data pull from ServiceNow is triggered automatically
    And the pull result is logged with timestamp and record count

    Examples:
      | interval |
      | DAILY    |
      | WEEKLY   |

  Scenario: Admin configures the pull schedule via engine-core:admin
    Given I am authenticated as a user with role "Admin"
    When I set the ServiceNow pull interval to "DAILY" at "02:00 UTC"
    Then the scheduler is updated with the new configuration
    And the next scheduled pull reflects the new time

  Scenario: Scheduler retries on transient failure
    Given the ServiceNow pull schedule is "DAILY"
    And the previous pull failed due to a network timeout
    When the retry interval elapses
    Then engine-integrations:servicenow retries the data pull
    And the retry attempt is logged

  # ---------------------------------------------------------------------------
  # Report Distribution
  # ---------------------------------------------------------------------------

  Scenario: Auto-generate Excel report from fresh ServiceNow data
    Given a ServiceNow data pull has completed successfully
    And an Excel report template is configured for the pulled data
    When processor-generators:xls generates the report
    Then an Excel file is produced containing the latest ServiceNow data
    And the file is stored in the report repository

  Scenario: Email distribution of generated report
    Given an Excel report has been generated from ServiceNow data
    And email distribution list is configured with addresses "ops@example.com, mgr@example.com"
    When engine-reporting:notification sends the distribution emails
    Then each configured address receives an email with the report attached
    And the email delivery status is logged

  Scenario: Report generation failure triggers notification
    Given a ServiceNow data pull has completed successfully
    And the Excel report generation fails due to template error
    When the failure is detected
    Then engine-reporting:notification sends an alert to the configured admin addresses
    And the error details are included in the notification

  # ---------------------------------------------------------------------------
  # BI Dashboards
  # ---------------------------------------------------------------------------

  Scenario: ServiceNow data is available in engine-data:dashboard for visualization
    Given ServiceNow incident data has been pulled and stored
    When I open the BI dashboard in engine-data:dashboard
    Then ServiceNow data sources are available for widget configuration
    And I can create charts and tables from the incident data

  Scenario: ServiceNow data is treated the same as PPTX/XLS imported data
    Given ServiceNow data and PPTX-extracted data exist in the platform
    When I configure a dashboard widget
    Then both data sources are available in the data source selector
    And I can combine ServiceNow and file-imported data in the same dashboard
