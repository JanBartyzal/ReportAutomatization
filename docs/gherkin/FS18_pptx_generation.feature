Feature: FS18 - PPTX Report Generation
  As a HoldingAdmin
  I want to manage PPTX templates with placeholders and generate presentation reports from approved data
  So that standardized PowerPoint reports are produced automatically from collected OPEX data

  # ---------------------------------------------------------------------------
  # Template Upload & Management (engine-reporting:pptx-template)
  # ---------------------------------------------------------------------------

  Scenario: HoldingAdmin uploads a PPTX template
    Given an authenticated user with role "HoldingAdmin"
    And a valid PPTX file containing placeholder tags
    When the HoldingAdmin sends POST /templates/pptx with the PPTX file
    Then the template is stored with scope "CENTRAL"
    And the system returns HTTP status 201 with the template_id
    And the template version is set to "v1"

  Scenario: System parses placeholders from uploaded template
    Given a PPTX template containing placeholders "{{it_costs}}", "{{TABLE:cost_breakdown}}", and "{{CHART:monthly_opex}}"
    When the template is uploaded via POST /templates/pptx
    Then the system extracts all placeholders
    And the response includes a list of required data inputs:
      | placeholder            | type  |
      | {{it_costs}}           | TEXT  |
      | {{TABLE:cost_breakdown}} | TABLE |
      | {{CHART:monthly_opex}}  | CHART |

  Scenario: Placeholders are displayed as required data inputs in UI
    Given a PPTX template with id "tmpl-001" has been uploaded
    And the template contains 5 text placeholders, 2 table placeholders, and 1 chart placeholder
    When a HoldingAdmin views the template detail page
    Then all 8 placeholders are listed as required data inputs
    And each placeholder shows its type (TEXT, TABLE, or CHART)

  Scenario: Template versioning on re-upload
    Given a PPTX template "tmpl-001" exists at version "v1"
    When the HoldingAdmin uploads a new version of the same template
    Then the template version increments to "v2"
    And version "v1" remains accessible for reports already using it
    And both versions are listed in the template version history

  Scenario: Template assigned to period and report type
    Given a PPTX template "tmpl-001" at version "v2"
    When the HoldingAdmin assigns the template to period "Q1-2025" and report_type "OPEX"
    Then the assignment is persisted
    And any PPTX generation for "OPEX" reports in "Q1-2025" uses template "tmpl-001" v2

  Scenario: Template preview without data
    Given a PPTX template "tmpl-001" has been uploaded
    When a HoldingAdmin requests a preview of the template
    Then the system renders a preview showing placeholder tags as sample text
    And table placeholders show sample column headers with empty rows
    And chart placeholders show a sample chart frame

  # ---------------------------------------------------------------------------
  # Data Mapping Configuration
  # ---------------------------------------------------------------------------

  Scenario: HoldingAdmin maps text placeholder to form field
    Given a PPTX template "tmpl-001" with placeholder "{{it_costs}}"
    And a form contains field "amount_czk" of type number
    When the HoldingAdmin maps "{{it_costs}}" to field "amount_czk" from the form
    Then the mapping is saved as part of the template configuration
    And the mapping applies to all reports using this template, not per individual report

  Scenario: HoldingAdmin maps table placeholder to data source columns
    Given a PPTX template "tmpl-001" with placeholder "{{TABLE:cost_breakdown}}"
    And the data source contains columns "category", "amount_czk", "amount_eur"
    When the HoldingAdmin maps "{{TABLE:cost_breakdown}}" to columns "category", "amount_czk", "amount_eur"
    Then the table mapping is saved as part of the template configuration

  Scenario: HoldingAdmin maps chart placeholder to metric
    Given a PPTX template "tmpl-001" with placeholder "{{CHART:monthly_opex}}"
    And the data source contains metric "monthly_opex_czk" with monthly breakdown
    When the HoldingAdmin maps "{{CHART:monthly_opex}}" to metric "monthly_opex_czk"
    Then the chart mapping is saved as part of the template configuration

  Scenario: Mapping is per template configuration not per report
    Given a PPTX template "tmpl-001" with placeholder "{{it_costs}}" mapped to field "amount_czk"
    And reports "RPT-001" and "RPT-002" both use template "tmpl-001"
    When PPTX is generated for "RPT-001"
    And PPTX is generated for "RPT-002"
    Then both use the same mapping configuration
    And each report's own data is inserted into the respective PPTX

  # ---------------------------------------------------------------------------
  # Single Report Generation (processor-generators:pptx)
  # ---------------------------------------------------------------------------

  Scenario: Generate PPTX from approved report data
    Given a PPTX template "tmpl-001" with all placeholders mapped
    And an OPEX report "RPT-001" in state "APPROVED" with complete data
    When a user sends POST /generate/pptx with template_id "tmpl-001" and report_id "RPT-001"
    Then the system loads approved source data from the database
    And the generator replaces text placeholders with actual values
    And the generator fills table placeholders with data rows
    And the generator creates charts from mapped metrics using python-pptx and matplotlib/plotly
    And the result PPTX is stored in Blob Storage
    And the Blob Storage URL is saved to report "RPT-001"
    And the response returns HTTP status 202 (Accepted) for async processing

  Scenario: PPTX generation completes asynchronously with notification
    Given a PPTX generation request has been accepted for report "RPT-001"
    When the generation completes successfully
    Then a notification is sent to the requesting user via FS13
    And a WebSocket/SSE event is emitted with the generation result
    And the notification includes a download link for the generated PPTX

  Scenario: Generated PPTX is valid and openable
    Given a PPTX has been generated for report "RPT-001"
    When the file is downloaded
    Then the file is a valid PPTX format
    And the file is openable in Microsoft PowerPoint
    And the file is openable in LibreOffice Impress
    And all text placeholders are replaced with actual data values
    And all tables contain the correct data rows
    And all charts render the correct metrics

  Scenario: Generate 20-slide PPTX within performance target
    Given a PPTX template with 20 slides containing mixed text, table, and chart placeholders
    And an approved report with complete data for all placeholders
    When PPTX generation is triggered
    Then the generation completes in less than 60 seconds

  # ---------------------------------------------------------------------------
  # Missing Data Handling
  # ---------------------------------------------------------------------------

  Scenario: Missing text placeholder data shows DATA MISSING marker
    Given a PPTX template "tmpl-001" with placeholder "{{travel_costs}}"
    And report "RPT-001" has no data mapped to "{{travel_costs}}"
    When PPTX generation is triggered for "RPT-001"
    Then the generated PPTX replaces "{{travel_costs}}" with "DATA MISSING" text in a red frame
    And the generation does not fail

  Scenario: Missing table data shows DATA MISSING marker on slide
    Given a PPTX template with placeholder "{{TABLE:vendor_costs}}"
    And report "RPT-001" has no data for "vendor_costs"
    When PPTX generation is triggered for "RPT-001"
    Then the slide containing "{{TABLE:vendor_costs}}" shows a red frame with "DATA MISSING"
    And the generation completes successfully

  Scenario: Missing chart data shows DATA MISSING marker on slide
    Given a PPTX template with placeholder "{{CHART:capex_trend}}"
    And report "RPT-001" has no data for metric "capex_trend"
    When PPTX generation is triggered for "RPT-001"
    Then the slide containing "{{CHART:capex_trend}}" shows a red frame with "DATA MISSING"
    And the generation completes successfully

  Scenario: Partial missing data generates report with mixed valid and missing sections
    Given a PPTX template with placeholders "{{it_costs}}", "{{TABLE:cost_breakdown}}", "{{CHART:monthly_opex}}"
    And report "RPT-001" has data for "{{it_costs}}" and "{{TABLE:cost_breakdown}}" but not for "{{CHART:monthly_opex}}"
    When PPTX generation is triggered
    Then "{{it_costs}}" is replaced with the actual value
    And "{{TABLE:cost_breakdown}}" is filled with data rows
    And "{{CHART:monthly_opex}}" shows a red frame with "DATA MISSING"
    And the generation completes successfully

  # ---------------------------------------------------------------------------
  # Batch Generation
  # ---------------------------------------------------------------------------

  Scenario: HoldingAdmin triggers batch PPTX generation for all approved reports in a period
    Given period "Q1-2025" has 10 approved OPEX reports
    And a PPTX template is assigned to period "Q1-2025" for report_type "OPEX"
    When the HoldingAdmin triggers batch PPTX generation for period "Q1-2025"
    Then the system queues generation for all 10 reports
    And the response returns HTTP status 202 with a batch job identifier
    And each generated PPTX is stored in Blob Storage with URL linked to its report

  Scenario: Batch generation of 10 reports completes within performance target
    Given a batch PPTX generation job for 10 approved reports
    When the batch job executes
    Then all 10 PPTX files are generated in less than 15 minutes

  Scenario: Batch generation notifies HoldingAdmin on completion
    Given a batch PPTX generation job has been triggered
    When all reports in the batch have been generated
    Then a summary notification is sent to the HoldingAdmin via FS13
    And the notification includes the count of successful and failed generations
    And a download link for a ZIP archive of all generated PPTX files is provided

  Scenario: Batch generation handles individual failures gracefully
    Given a batch PPTX generation job for 5 reports
    And report "RPT-003" has corrupted source data
    When the batch job executes
    Then 4 reports generate successfully
    And report "RPT-003" is marked as failed with an error message
    And the batch job does not abort due to a single failure
    And the summary notification lists "RPT-003" as failed

  # ---------------------------------------------------------------------------
  # Download & Access
  # ---------------------------------------------------------------------------

  Scenario: Generated PPTX is downloadable from report UI
    Given a PPTX has been generated for report "RPT-001"
    And the Blob Storage URL is saved to the report
    When an authorized user opens the report detail page
    Then a "Download PPTX" button is visible
    And clicking the button downloads the generated PPTX file

  Scenario: Only authorized users can download generated PPTX
    Given a PPTX has been generated for report "RPT-001" belonging to organization "SUB-001"
    When an Editor from organization "SUB-002" attempts to download the PPTX
    Then the download is denied with HTTP status 403

  # ---------------------------------------------------------------------------
  # Error Handling
  # ---------------------------------------------------------------------------

  Scenario: Generation request for non-existent template returns 404
    Given no template with id "tmpl-999" exists
    When a user sends POST /generate/pptx with template_id "tmpl-999" and report_id "RPT-001"
    Then the system returns HTTP status 404
    And the response body contains "template not found"

  Scenario: Generation request for non-approved report is rejected
    Given report "RPT-001" is in state "DRAFT"
    When a user sends POST /generate/pptx with template_id "tmpl-001" and report_id "RPT-001"
    Then the system returns HTTP status 422
    And the response body contains "report must be in APPROVED state for PPTX generation"
