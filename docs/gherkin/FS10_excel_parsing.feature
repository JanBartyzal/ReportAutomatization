Feature: FS10 – Excel Parsing Logic
  As the platform
  I want to parse Excel files per-sheet into JSONB with partial success handling
  So that individual sheet failures do not block overall processing and Excel data is unified with PPTX data at the database level

  Background:
    Given the Excel Atomizer (processor-atomizers:xls) is running as a Python FastAPI service
    And communication with engine-orchestrator is via Dapr gRPC
    And parsed data is stored in PostgreSQL as JSONB

  # ---------------------------------------------------------------------------
  # Per-Sheet Parsing
  # ---------------------------------------------------------------------------

  @processor-atomizers:xls @per-sheet
  Scenario: Excel file is parsed sheet by sheet into separate JSONB records
    Given an Excel file "report.xlsx" contains sheets "Revenue", "Costs", and "Summary"
    When processor-atomizers:xls processes the file
    Then 3 separate JSONB records are created in the database
    And each record is linked to the same file_id
    And each record includes the sheet name as metadata

  @processor-atomizers:xls @per-sheet
  Scenario: Each sheet record contains structured tabular data
    Given an Excel sheet "Revenue" contains a header row and 50 data rows
    When processor-atomizers:xls parses the sheet
    Then the resulting JSONB record contains an array of 50 row objects
    And each row object uses the header values as keys
    And numeric cells are stored as numbers, not strings

  @processor-atomizers:xls @per-sheet
  Scenario: Empty sheets are handled gracefully
    Given an Excel file contains a sheet "Notes" with no data
    When processor-atomizers:xls processes the file
    Then a JSONB record is created for "Notes" with an empty data array
    And the record metadata indicates the sheet is empty
    And the sheet status is "COMPLETED"

  # ---------------------------------------------------------------------------
  # Partial Success
  # ---------------------------------------------------------------------------

  @processor-atomizers:xls @partial-success
  Scenario: Partial success when one sheet fails and others succeed
    Given an Excel file "data.xlsx" contains 10 sheets
    And sheet 7 "Corrupted" contains malformed data that causes a parsing error
    When processor-atomizers:xls processes the file
    Then 9 sheets are parsed successfully and saved as JSONB records
    And sheet "Corrupted" is marked with status "FAILED" and an error message
    And the overall file status is set to "PARTIAL"

  @processor-atomizers:xls @partial-success
  Scenario: All sheets succeed results in COMPLETED status
    Given an Excel file "clean.xlsx" contains 5 sheets with valid data
    When processor-atomizers:xls processes all sheets without errors
    Then all 5 JSONB records are saved successfully
    And each sheet record has status "COMPLETED"
    And the overall file status is set to "COMPLETED"

  @processor-atomizers:xls @partial-success
  Scenario: All sheets fail results in FAILED status
    Given an Excel file "broken.xlsx" contains 3 sheets all with corrupted data
    When processor-atomizers:xls processes the file and all sheets fail
    Then each sheet record is marked with status "FAILED" and its respective error message
    And the overall file status is set to "FAILED"
    And the failure is reported back to engine-orchestrator

  @processor-atomizers:xls @partial-success
  Scenario: Partial success reports individual sheet errors to engine-orchestrator
    Given an Excel file has 10 sheets with 1 failed sheet
    When the parsing completes with partial success
    Then engine-orchestrator receives a response indicating status "PARTIAL"
    And the response includes a list of sheet results with statuses and error details
    And the processing log records the partial success with the failed sheet name

  # ---------------------------------------------------------------------------
  # Data Compatibility with PPTX
  # ---------------------------------------------------------------------------

  @processor-atomizers:xls @data-compatibility
  Scenario: JSONB records from Excel are indistinguishable from PPTX at the database level
    Given an Excel file and a PPTX file have both been parsed
    When the JSONB records are stored in the structured_data table
    Then both records share the same schema structure
    And both records include "file_id", "source_type", "data", and "metadata" columns
    And the source_type is "EXCEL" for the Excel record and "PPTX" for the PPTX record

  @processor-atomizers:xls @data-compatibility
  Scenario: Unified querying works across Excel and PPTX data
    Given JSONB records exist from both Excel and PPTX sources for org_id "org-001"
    When a query is executed against the structured_data table without a source_type filter
    Then results include records from both Excel and PPTX sources
    And the data format is consistent regardless of the original file type

  @processor-atomizers:xls @data-compatibility
  Scenario: Dashboard aggregation treats Excel and PPTX data uniformly
    Given parsed data from Excel file "opex.xlsx" and PPTX file "summary.pptx" exists
    When the dashboard aggregation endpoint processes a GROUP BY query
    Then data from both sources is aggregated together seamlessly
    And the aggregation result does not differentiate between file source types unless explicitly filtered

  # ---------------------------------------------------------------------------
  # Edge Cases
  # ---------------------------------------------------------------------------

  @processor-atomizers:xls @edge-case
  Scenario: Excel file with a single sheet is parsed correctly
    Given an Excel file "single.xlsx" contains exactly 1 sheet "Data"
    When processor-atomizers:xls processes the file
    Then 1 JSONB record is created for sheet "Data"
    And the overall file status is "COMPLETED"

  @processor-atomizers:xls @edge-case
  Scenario: Excel file with merged cells is handled
    Given an Excel sheet contains merged cells in the header row
    When processor-atomizers:xls parses the sheet
    Then the merged cell value is applied to all spanned columns
    And the resulting JSONB record has distinct keys for each column

  @processor-atomizers:xls @edge-case
  Scenario: Excel file with formula cells stores computed values
    Given an Excel sheet contains cells with formulas (e.g., =SUM(A1:A10))
    When processor-atomizers:xls parses the sheet
    Then the JSONB record contains the computed values, not the formula strings
    And numeric results are stored as numbers
