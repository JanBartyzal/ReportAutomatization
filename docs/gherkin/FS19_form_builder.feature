Feature: FS19 - Dynamic Form Builder & Data Collection
  As a HoldingAdmin or Editor
  I want to create dynamic forms, collect data through them, and support Excel import/export
  So that subsidiaries can submit structured OPEX data through configurable forms with validation

  # ---------------------------------------------------------------------------
  # Form Builder - Field Types & Configuration
  # ---------------------------------------------------------------------------

  Scenario: HoldingAdmin creates a new form with drag-and-drop editor
    Given an authenticated user with role "HoldingAdmin"
    When the HoldingAdmin opens the form builder
    Then a drag-and-drop UI editor is displayed
    And the editor provides field type palette, form canvas, and properties panel

  Scenario Outline: HoldingAdmin adds a field of each supported type
    Given the form builder is open with a new form
    When the HoldingAdmin drags a "<field_type>" field onto the canvas
    Then the field is added to the form
    And the properties panel shows configuration options for "<field_type>"

    Examples:
      | field_type      |
      | text            |
      | number          |
      | percentage      |
      | date            |
      | dropdown        |
      | table           |
      | file_attachment |

  Scenario: HoldingAdmin configures number field with currency and unit
    Given a number field "total_opex" is on the form canvas
    When the HoldingAdmin sets currency to "CZK" and unit to "thousands"
    Then the field displays with currency symbol and unit label
    And the field accepts only numeric input

  Scenario: HoldingAdmin marks a field as required
    Given a text field "company_name" is on the form canvas
    When the HoldingAdmin toggles the "Required" setting to on
    Then the field is marked as required
    And the field shows a required indicator in the form preview

  Scenario: HoldingAdmin adds validation rules to a field
    Given a number field "employee_count" is on the form canvas
    When the HoldingAdmin sets min value to 0 and max value to 100000
    Then the validation rules are saved for the field
    And values outside the range 0-100000 are rejected during form filling

  Scenario: HoldingAdmin adds regex validation to a text field
    Given a text field "cost_center_code" is on the form canvas
    When the HoldingAdmin sets a regex pattern "^CC-[0-9]{4}$"
    Then the validation rule is saved
    And only values matching the pattern "CC-XXXX" are accepted

  Scenario: HoldingAdmin adds field dependency rule
    Given a number field "field_A" and a text field "field_B" are on the form canvas
    When the HoldingAdmin creates a dependency rule "if field_A > 0 then field_B is required"
    Then the dependency is saved
    And during filling, when field_A has a value greater than 0, field_B becomes required

  Scenario: HoldingAdmin adds sections and descriptive texts
    Given the form builder is open
    When the HoldingAdmin adds a section "Personnel Costs" with description "Enter all personnel-related expenditures"
    Then the section appears on the canvas with the heading and description
    And fields can be dragged into the section

  Scenario: HoldingAdmin configures a table field with fixed columns
    Given the form builder is open
    When the HoldingAdmin adds a table field with columns "Category", "Amount CZK", "Amount EUR"
    Then the table field is configured with 3 fixed columns
    And during filling, users can add rows but not modify column structure

  Scenario: HoldingAdmin creates and publishes form within 10 minutes
    Given a HoldingAdmin without technical knowledge
    When the HoldingAdmin creates a form with 15 fields across 3 sections using drag-and-drop
    And adds validation rules and required settings
    And previews and publishes the form
    Then the entire process completes in less than 10 minutes

  # ---------------------------------------------------------------------------
  # Form Preview & Publishing
  # ---------------------------------------------------------------------------

  Scenario: HoldingAdmin previews form before publishing
    Given a form in state "DRAFT" with multiple fields and sections
    When the HoldingAdmin clicks "Preview"
    Then the form renders exactly as it will appear to Editors
    And all field types, validation indicators, and sections are visible
    And no data can be submitted from the preview

  Scenario: HoldingAdmin publishes a draft form
    Given a form in state "DRAFT"
    When the HoldingAdmin publishes the form
    Then the form state changes to "PUBLISHED"
    And the form becomes visible to assigned users

  # ---------------------------------------------------------------------------
  # Form Versioning
  # ---------------------------------------------------------------------------

  Scenario: Modifying a published form creates a new version
    Given a form "FORM-001" in state "PUBLISHED" at version 1
    And 3 organizations have already submitted data using version 1
    When the HoldingAdmin modifies the form and saves
    Then a new version 2 of the form is created
    And the existing submissions remain bound to version 1
    And new submissions use version 2
    And both versions are stored separately

  Scenario: Historical data is not overwritten by form upgrade
    Given form "FORM-001" version 1 has submitted data from "SUB-001"
    And form "FORM-001" version 2 adds a new field "cloud_costs"
    When the HoldingAdmin queries data submitted under version 1
    Then the data is intact and complete as originally submitted
    And the field "cloud_costs" is not present in version 1 data

  # ---------------------------------------------------------------------------
  # Form Assignment & States
  # ---------------------------------------------------------------------------

  Scenario: Form assigned to period and report type
    Given a published form "FORM-001"
    When the HoldingAdmin assigns it to period "Q1-2025" and report_type "OPEX"
    Then the form is linked to period "Q1-2025" and report_type "OPEX"

  Scenario: Form assigned to specific companies
    Given a published form "FORM-001" assigned to period "Q1-2025"
    When the HoldingAdmin assigns the form to companies "SUB-001", "SUB-002", "SUB-003"
    Then only Editors from those 3 companies see the form in their task list

  Scenario: Form transitions to CLOSED state after deadline
    Given a published form "FORM-001" with deadline "2025-03-31"
    When the current date passes "2025-03-31"
    Then the form state changes to "CLOSED"
    And Editors can no longer submit data through the form
    And the form is tied to deadline management via FS20

  Scenario: Only HoldingAdmin sees forms in DRAFT state
    Given a form "FORM-002" in state "DRAFT"
    When an Editor browses the form list
    Then "FORM-002" is not visible to the Editor

  # ---------------------------------------------------------------------------
  # Form Filling - Editor Experience
  # ---------------------------------------------------------------------------

  Scenario: Editor sees list of forms to fill in current period
    Given an authenticated Editor for organization "SUB-001"
    And forms "FORM-001" and "FORM-002" are assigned to "SUB-001" for period "Q1-2025"
    When the Editor opens the form list for period "Q1-2025"
    Then both "FORM-001" and "FORM-002" are listed
    And each form shows its completion status

  Scenario: Auto-save triggers every 30 seconds during form filling
    Given an Editor is filling form "FORM-001"
    And the Editor has entered data into 3 fields
    When 30 seconds have elapsed since the last change
    Then the form data is automatically saved as DRAFT
    And a save indicator confirms the auto-save

  Scenario: Auto-save triggers on section change
    Given an Editor is filling form "FORM-001" in section "Personnel Costs"
    And the Editor has entered data into 2 fields
    When the Editor navigates to section "IT Costs"
    Then the data from "Personnel Costs" is automatically saved
    And the Editor can continue filling "IT Costs"

  Scenario: Data preserved after connection loss
    Given an Editor is filling form "FORM-001"
    And the Editor has entered data into 5 fields
    And auto-save has persisted the data
    When the Editor loses network connection and reconnects
    Then all 5 fields contain the previously saved data
    And the Editor can continue filling the form

  Scenario: Real-time validation marks error fields before submission
    Given an Editor is filling form "FORM-001"
    And the field "employee_count" has max value validation of 100000
    When the Editor enters 150000 in "employee_count"
    Then the field "employee_count" is immediately marked with a validation error
    And the error message "value must be <= 100000" is displayed inline

  Scenario: All validation errors returned at once, not one by one
    Given a form with 3 fields failing validation
    When the Editor attempts to submit the form
    Then all 3 validation errors are displayed simultaneously
    And each error field is highlighted with its specific error message

  Scenario: Editor saves form as DRAFT and returns later
    Given an Editor has partially filled form "FORM-001" and saved as DRAFT
    When the Editor returns to form "FORM-001" the next day
    Then all previously entered data is loaded
    And the Editor can continue filling from where they left off

  Scenario: Editor adds field-level comment
    Given an Editor is filling form "FORM-001"
    And the field "depreciation_czk" has value 5000000
    When the Editor adds a comment "This number includes one-time write-off from Q1"
    Then the comment is saved and associated with the field "depreciation_czk"
    And the comment is visible to HoldingAdmin during review

  Scenario: Editor submits completed form entering FS17 workflow
    Given an Editor has filled all required fields in form "FORM-001"
    And all validation rules pass
    When the Editor submits the form
    Then the form data enters the submission workflow as defined in FS17
    And the report state transitions to "SUBMITTED"

  # ---------------------------------------------------------------------------
  # Form Data Scope (CENTRAL Only for FS19)
  # ---------------------------------------------------------------------------

  Scenario: Forms in FS19 are scoped to CENTRAL
    Given a HoldingAdmin creates a new form
    Then the form scope is set to "CENTRAL"
    And the data model includes scope and owner_org_id fields for future FS21 extension

  # ---------------------------------------------------------------------------
  # Excel Template Export
  # ---------------------------------------------------------------------------

  Scenario: Export form as structured Excel template
    Given a published form "FORM-001" with 3 sections and 12 fields
    When a user sends GET /forms/FORM-001/export/excel-template
    Then the system returns an Excel file
    And the Excel has one sheet per section
    And columns match the form fields with appropriate headers
    And Excel validation rules mirror form validation (dropdowns, min/max)
    And a hidden metadata sheet "__form_meta" contains form_id and form_version_id

  Scenario: Excel template metadata sheet contains correct identifiers
    Given a published form "FORM-001" at version 2
    When the Excel template is exported
    Then the hidden sheet "__form_meta" contains form_id "FORM-001"
    And the hidden sheet "__form_meta" contains form_version_id matching version 2

  # ---------------------------------------------------------------------------
  # Excel Import - Template-Based
  # ---------------------------------------------------------------------------

  Scenario: Import Excel with matching form version
    Given a published form "FORM-001" at version 2
    And an Excel file filled from the exported template with form_version_id matching version 2
    When the Editor sends POST /forms/FORM-001/import/excel with the filled Excel
    Then the system reads the "__form_meta" sheet and confirms version match
    And data is directly mapped to form fields without confirmation dialog
    And the imported data is displayed in the form UI for visual review

  Scenario: Import Excel with mismatched form version shows warning
    Given a published form "FORM-001" at version 3
    And an Excel file with "__form_meta" containing form_version_id for version 2
    When the Editor sends POST /forms/FORM-001/import/excel with the Excel
    Then the system detects the version mismatch
    And a warning is displayed to the Editor
    And the system attempts best-effort mapping via FS15 Schema Mapping
    And the Editor must review and confirm the mapping before import completes

  Scenario: Imported data shown in form UI for visual review before submission
    Given an Excel has been imported into form "FORM-001"
    When the import completes
    Then all imported data is displayed in the form UI
    And the Editor can review each field value
    And the Editor can edit any imported value before submission
    And the data is not submitted until the Editor explicitly submits

  # ---------------------------------------------------------------------------
  # Arbitrary Excel Import (No Template)
  # ---------------------------------------------------------------------------

  Scenario: Editor uploads arbitrary Excel as data input
    Given an Editor has their own Excel file with cost data
    And the Excel does not contain a "__form_meta" sheet
    When the Editor uploads the Excel to form "FORM-001"
    Then the system parses the Excel via processor-atomizers:xls
    And the system offers a column-to-field mapping interface using FS15 Schema Mapping

  Scenario: Automatic column mapping suggested via FS15
    Given an arbitrary Excel with columns "Kategorie", "Castka CZK", "Castka EUR"
    And form "FORM-001" has fields "category", "amount_czk", "amount_eur"
    When the system processes the mapping
    Then FS15 suggests automatic mappings:
      | excel_column  | form_field  | confidence |
      | Kategorie     | category    | HIGH       |
      | Castka CZK    | amount_czk  | HIGH       |
      | Castka EUR    | amount_eur  | HIGH       |
    And the Editor can confirm the mapping in less than 2 minutes

  Scenario: Editor reviews and confirms column mapping
    Given the system has suggested column-to-field mappings for an arbitrary Excel
    When the Editor reviews the mapping
    Then the Editor can accept, reject, or modify each mapping
    And after confirmation, data is imported into the form fields
    And the imported data is editable as if entered manually

  Scenario: Original arbitrary Excel saved as report attachment
    Given an Editor has imported data from an arbitrary Excel
    When the import is confirmed
    Then the original Excel file is saved as a report attachment
    And the attachment is accessible for audit trail purposes

  # ---------------------------------------------------------------------------
  # Filled Data Availability
  # ---------------------------------------------------------------------------

  Scenario: Filled form data immediately available in central reporting
    Given an Editor has submitted form "FORM-001" for organization "SUB-001"
    And the report has been approved via FS17
    When a HoldingAdmin queries central reporting data
    Then the data from form "FORM-001" submitted by "SUB-001" is included in the results

  # ---------------------------------------------------------------------------
  # Edge Cases & Error Handling
  # ---------------------------------------------------------------------------

  Scenario: Import Excel with no recognizable columns
    Given an arbitrary Excel with columns that do not match any form fields
    When the system attempts FS15 Schema Mapping
    Then all mappings show confidence "LOW" or "NONE"
    And the Editor is warned that automatic mapping could not be determined
    And the Editor must manually map each column

  Scenario: File attachment field accepts valid file types
    Given a form with a file_attachment field configured for "PDF, XLSX, DOCX"
    When an Editor uploads a PDF file
    Then the file is accepted and attached to the form submission

  Scenario: File attachment field rejects invalid file types
    Given a form with a file_attachment field configured for "PDF, XLSX, DOCX"
    When an Editor attempts to upload an EXE file
    Then the upload is rejected with error "unsupported file type"

  Scenario: Concurrent form filling by same user on multiple devices
    Given an Editor opens form "FORM-001" on device A and device B
    And the Editor enters data on device A
    When the Editor enters different data on device B
    Then the system detects the conflict via auto-save timestamps
    And the Editor is notified of the conflict with option to resolve

  Scenario: Form with dependency rule validates correctly
    Given a form with fields "overtime_hours" and "overtime_justification"
    And dependency rule "if overtime_hours > 0 then overtime_justification is required"
    When an Editor enters overtime_hours = 150 and leaves overtime_justification empty
    Then the validation error "overtime_justification is required when overtime_hours > 0" is displayed
