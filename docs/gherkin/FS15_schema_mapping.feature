Feature: FS15 - Template & Schema Mapping Registry
  As a data administrator
  I want to define column mapping templates that normalize heterogeneous data from different subsidiaries
  So that columns like "Cena", "Cost", and "Naklady" are all mapped to a canonical schema before storage

  # ---------------------------------------------------------------------------
  # Editor UI - Mapping Template Definition
  # ---------------------------------------------------------------------------

  Scenario: Editor creates a new mapping template
    Given a user with role "Editor" is authenticated
    When the user creates a new mapping template named "OPEX Standard CZ"
    And defines a rule "If column contains 'Cena' or 'Cost' or 'Naklady' then map as 'amount_czk'"
    Then the mapping template is saved in engine-data:template
    And the template is associated with the user's org_id

  Scenario: Editor defines multiple column mapping rules in a template
    Given a mapping template "OPEX Standard CZ" exists
    When the Editor adds the following rules:
      | source_pattern            | target_field    |
      | Cena, Cost, Naklady       | amount_czk      |
      | Stredisko, Cost Center    | cost_center     |
      | Obdobi, Period, Mesic     | period          |
      | Spolecnost, Company       | company_name    |
    Then all 4 rules are saved in the template
    And each rule stores both the source patterns and the target field name

  Scenario: Editor edits an existing mapping rule
    Given a mapping template "OPEX Standard CZ" exists with a rule mapping "Cena" to "amount_czk"
    When the Editor updates the rule to also match "Price"
    Then the rule's source patterns include "Cena", "Cost", "Naklady", and "Price"
    And the target field remains "amount_czk"

  Scenario: Editor deletes a mapping rule from a template
    Given a mapping template "OPEX Standard CZ" exists with 4 rules
    When the Editor deletes the rule for "company_name"
    Then the template contains 3 rules
    And the deleted rule is no longer applied during mapping

  Scenario: Viewer cannot create or edit mapping templates
    Given a user with role "Viewer" is authenticated
    When the user attempts to create a mapping template
    Then the system returns HTTP status 403

  # ---------------------------------------------------------------------------
  # Learning - Automatic Mapping Suggestions
  # ---------------------------------------------------------------------------

  Scenario: System suggests mappings based on previous successful mappings
    Given a mapping template successfully mapped column "IT Naklady" to "amount_czk" for org "Alpha Corp"
    When a new file from "Alpha Corp" is uploaded with column "IT Naklady"
    Then engine-data:template suggests mapping "IT Naklady" to "amount_czk"
    And the suggestion includes a confidence score

  Scenario: System learns from mappings across organizations
    Given organization "Alpha Corp" mapped "Celkove naklady" to "amount_czk"
    And organization "Beta Corp" mapped "Total Cost" to "amount_czk"
    When organization "Gamma Corp" uploads a file with column "Celkove naklady"
    Then engine-data:template suggests mapping "Celkove naklady" to "amount_czk"
    And the suggestion references the learned pattern

  Scenario: User confirms a suggested mapping
    Given engine-data:template suggests mapping column "IT Budget" to "amount_czk" with confidence 0.9
    When the Editor confirms the suggestion
    Then the mapping is applied to the current file
    And the confirmed mapping strengthens the learning model for future files

  Scenario: User rejects a suggested mapping and provides correct mapping
    Given engine-data:template suggests mapping column "Poznamka" to "amount_czk" with confidence 0.4
    When the Editor rejects the suggestion
    And manually maps "Poznamka" to "notes"
    Then the manual mapping is applied
    And the learning model is updated to associate "Poznamka" with "notes"

  Scenario: Low confidence suggestions are flagged for manual review
    Given a new file contains a column "Misc Data"
    And no previous mapping matches with confidence above 0.5
    When engine-data:template processes the column
    Then the column is flagged as "unmatched - manual review required"
    And no automatic mapping is applied

  # ---------------------------------------------------------------------------
  # engine-orchestrator Integration - gRPC Call Before DB Write
  # ---------------------------------------------------------------------------

  Scenario: engine-orchestrator calls engine-data:template via gRPC before writing data to database
    Given a file has been parsed by an Atomizer
    And the parsed data contains columns that need mapping
    When engine-orchestrator processes the file in its workflow
    Then engine-orchestrator calls engine-data:template via Dapr gRPC with the column headers
    And engine-data:template returns the mapped column names
    And engine-orchestrator writes the data with mapped columns to the database via engine-data:sink-tbl

  Scenario: engine-data:template returns identity mapping for already-canonical columns
    Given a parsed file contains a column named "amount_czk"
    When engine-orchestrator calls engine-data:template with the column header "amount_czk"
    Then engine-data:template returns the mapping "amount_czk" to "amount_czk"
    And no transformation is applied

  Scenario: engine-data:template returns error for completely unmappable columns
    Given a parsed file contains a column "XYZABC123" with no matching template rules
    And the learning model has no suggestions
    When engine-orchestrator calls engine-data:template with the column header "XYZABC123"
    Then engine-data:template returns a "mapping_not_found" status for that column
    And engine-orchestrator stores the data with the original column name and a flag "unmapped"

  # ---------------------------------------------------------------------------
  # Excel Import to Form - POST /map/excel-to-form
  # ---------------------------------------------------------------------------

  Scenario: Excel file is mapped to form fields via POST /map/excel-to-form
    Given a form definition exists with fields "amount_czk", "cost_center", and "period"
    And an Editor uploads an Excel file with columns "Cena", "Stredisko", "Mesic"
    When the system calls POST /map/excel-to-form on engine-data:template
    Then engine-data:template maps "Cena" to form field "amount_czk"
    And maps "Stredisko" to form field "cost_center"
    And maps "Mesic" to form field "period"
    And returns the mapped data ready for form population

  Scenario: Excel-to-form mapping with partial match
    Given a form definition exists with fields "amount_czk", "cost_center", and "period"
    And an Editor uploads an Excel file with columns "Cena" and "Unknown Column"
    When the system calls POST /map/excel-to-form on engine-data:template
    Then engine-data:template maps "Cena" to form field "amount_czk"
    And "Unknown Column" is returned as "unmapped"
    And the response indicates partial mapping success

  Scenario: Excel-to-form endpoint validates file format
    Given an Editor uploads a non-Excel file to POST /map/excel-to-form
    When engine-data:template processes the request
    Then the system returns HTTP status 415
    And the response contains "unsupported file format"

  # ---------------------------------------------------------------------------
  # Template Management
  # ---------------------------------------------------------------------------

  Scenario: Admin lists all mapping templates for the organization
    Given 3 mapping templates exist for org_id "org-100"
    And a user with role "Admin" for org_id "org-100" is authenticated
    When the admin requests the list of mapping templates
    Then the response contains 3 templates
    And each template includes its name, rule count, and last modified date

  Scenario: Mapping templates are scoped by organization
    Given mapping templates exist for org_id "org-100" and org_id "org-200"
    And a user belongs to org_id "org-100"
    When the user requests the list of mapping templates
    Then only templates for org_id "org-100" are returned
