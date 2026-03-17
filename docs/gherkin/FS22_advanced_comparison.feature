Feature: FS22 - Advanced Period Comparison
  As a HoldingAdmin or Analyst
  I want to compare KPIs across periods, report types, and organizations
  So that I can identify trends and anomalies in consolidated financial data

  # ---------------------------------------------------------------------------
  # NOTE: This Feature Set is a PLACEHOLDER (Phase P6).
  # Scenarios will be detailed after FS20 deployment.
  # All scenarios are tagged @placeholder.
  # ---------------------------------------------------------------------------

  # ---------------------------------------------------------------------------
  # Configurable KPIs
  # ---------------------------------------------------------------------------

  @placeholder
  Scenario: Admin configures KPIs for period comparison
    Given I am authenticated as a user with role "HoldingAdmin"
    When I navigate to the comparison configuration in engine-data:dashboard
    Then I can select which KPIs are available for period comparison
    And the selected KPIs are persisted in the configuration store

  @placeholder
  Scenario: Configured KPIs appear in the comparison dashboard
    Given KPIs "Revenue", "OPEX", and "Headcount" are configured for comparison
    When I open the period comparison dashboard
    Then the configured KPIs are available as comparison metrics

  # ---------------------------------------------------------------------------
  # Cross-Type Comparison
  # ---------------------------------------------------------------------------

  @placeholder
  Scenario: Compare Q1 data against full year normalized to monthly values
    Given quarterly data exists for Q1 of the current year
    And full-year data exists for the previous year
    When I select cross-type comparison "Q1 vs Full Year" with normalization "monthly"
    Then the dashboard displays Q1 monthly values alongside previous year monthly averages
    And the normalization method is clearly indicated in the chart legend

  @placeholder
  Scenario: Compare quarterly data normalized to daily values
    Given quarterly data exists for Q1 and Q2
    When I select cross-type comparison "Q1 vs Q2" with normalization "daily"
    Then the dashboard displays daily-normalized values for both quarters

  # ---------------------------------------------------------------------------
  # Multi-Org Comparison
  # ---------------------------------------------------------------------------

  @placeholder
  Scenario: HoldingAdmin compares same metric across all subsidiaries
    Given I am authenticated as a user with role "HoldingAdmin"
    And OPEX data exists for subsidiaries "ORG-100", "ORG-200", and "ORG-300"
    When I select metric "OPEX" for multi-org comparison
    Then the dashboard displays "OPEX" values for all three subsidiaries side by side
    And each subsidiary is clearly labeled

  @placeholder
  Scenario: Multi-org comparison respects data access permissions
    Given I am authenticated as a user with role "CompanyAdmin" for org_id "ORG-100"
    When I attempt to view multi-org comparison across all subsidiaries
    Then I can only see data for org_id "ORG-100"
    And data for other organizations is not accessible

  # ---------------------------------------------------------------------------
  # Drill-Down
  # ---------------------------------------------------------------------------

  @placeholder
  Scenario: Drill down comparison to cost center level
    Given period comparison data is displayed for metric "OPEX"
    When I drill down into a specific subsidiary
    Then the comparison is broken down by cost center
    And each cost center shows the selected metric for the compared periods

  @placeholder
  Scenario: Drill down comparison to division level
    Given period comparison data is displayed for metric "Revenue"
    When I drill down into a specific subsidiary at division level
    Then the comparison is broken down by division
    And each division shows the selected metric for the compared periods

  # ---------------------------------------------------------------------------
  # Export Comparison Reports
  # ---------------------------------------------------------------------------

  @placeholder
  Scenario: Export comparison report as PPTX via FS18 generator
    Given a period comparison is displayed on the dashboard
    When I click "Export as PPTX"
    Then processor-generators:pptx generates a PPTX report containing the comparison charts and data
    And the exported file is available for download

  @placeholder
  Scenario: Exported comparison includes normalization metadata
    Given a cross-type comparison "Q1 vs Full Year" with normalization "monthly" is displayed
    When I export the comparison as PPTX
    Then the generated report includes a metadata slide describing the normalization method
    And the data source periods are clearly labeled
