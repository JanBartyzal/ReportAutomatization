Feature: FS14 - Data Versioning & Diff Tool
  As a financial controller
  I want every data change to create a new version with the original always preserved
  So that no edit overwrites the original and I can trace exactly what changed between versions

  # ---------------------------------------------------------------------------
  # Versioning - Every Change Creates a New Version
  # ---------------------------------------------------------------------------

  Scenario: First data import creates version 1
    Given an Editor uploads and processes an OPEX data file
    When the data is saved to the database
    Then the data is stored as version "v1"
    And the version metadata includes the user ID, timestamp, and source file reference

  Scenario: Editing existing data creates a new version
    Given OPEX data exists at version "v1" for org_id "org-100" and period "2026-Q1"
    When an Editor modifies the IT cost value from 500000 to 600000
    Then a new version "v2" is created with the updated value
    And version "v1" remains unchanged in the database
    And the version "v2" metadata includes the editing user and timestamp

  Scenario: Multiple edits create sequential versions
    Given OPEX data exists at version "v2" for org_id "org-100" and period "2026-Q1"
    When an Editor adds a new row for "Marketing" costs
    Then a new version "v3" is created
    And versions "v1" and "v2" remain unchanged
    And the version history shows 3 entries

  Scenario: Original data is never overwritten
    Given OPEX data exists at versions "v1", "v2", and "v3"
    When any user queries version "v1"
    Then the system returns the exact original data from the first import
    And no field in "v1" has been modified

  Scenario: Re-import of file creates a new version
    Given OPEX data exists at version "v1" from file "opex_q1.xlsx"
    When an Editor re-uploads a corrected version of "opex_q1.xlsx"
    And the processing pipeline completes
    Then a new version "v2" is created from the re-imported data
    And version "v1" from the original file is preserved

  # ---------------------------------------------------------------------------
  # Version Metadata & History
  # ---------------------------------------------------------------------------

  Scenario: User views version history of a data set
    Given OPEX data for org_id "org-100" and period "2026-Q1" has 4 versions
    When a user requests the version history
    Then the system returns a list of 4 versions
    And each entry includes version number, author, timestamp, and change summary

  Scenario: Version history is scoped by RLS
    Given OPEX data for org_id "org-100" has 3 versions
    And a user belongs to org_id "org-200"
    When the user requests the version history for org_id "org-100"
    Then the system returns HTTP status 403

  # ---------------------------------------------------------------------------
  # Diff Tool - UI Version Comparison
  # ---------------------------------------------------------------------------

  Scenario: Diff tool shows value changes between two versions
    Given OPEX data version "v1" has IT costs of 500000
    And OPEX data version "v2" has IT costs of 1000000
    When a user compares version "v1" with version "v2"
    Then the diff tool highlights the IT costs field as changed
    And displays "+500000" as the difference for IT costs

  Scenario: Diff tool shows added rows between versions
    Given OPEX data version "v1" has 10 rows
    And OPEX data version "v2" has 12 rows with 2 new cost center entries
    When a user compares version "v1" with version "v2"
    Then the diff tool highlights the 2 new rows as "added"
    And the added rows are displayed with a visual indicator

  Scenario: Diff tool shows removed rows between versions
    Given OPEX data version "v1" has 10 rows
    And OPEX data version "v2" has 8 rows with 2 cost centers removed
    When a user compares version "v1" with version "v2"
    Then the diff tool highlights the 2 missing rows as "removed"
    And the removed rows are displayed with a visual indicator

  Scenario: Diff tool displays OPEX-specific change summary
    Given OPEX data version "v1" has total IT costs of 2000000
    And OPEX data version "v2" has total IT costs of 2500000
    When a user compares version "v1" with version "v2"
    Then the diff tool displays a summary "+500k in IT costs v2 vs v1"

  Scenario: Diff tool supports comparing non-adjacent versions
    Given OPEX data has versions "v1", "v2", "v3", and "v4"
    When a user selects version "v1" and version "v4" for comparison
    Then the diff tool shows all cumulative changes between "v1" and "v4"
    And each changed field indicates the net difference

  # ---------------------------------------------------------------------------
  # Integration with FS17 - Approved Data Lock
  # ---------------------------------------------------------------------------

  Scenario: Approved report data becomes read-only
    Given OPEX data version "v3" is associated with a report in "APPROVED" state
    When an Editor attempts to create a new version from "v3"
    Then the system rejects the edit
    And the response contains "data locked after approval"

  Scenario: Reopened report creates new version from approved data
    Given OPEX data version "v3" is associated with a report in "APPROVED" state
    When a HoldingAdmin transitions the report back to "DRAFT"
    Then a new version "v4" is created as a copy of "v3"
    And the Editor can now modify version "v4"
    And version "v3" remains unchanged and linked to the original approval
