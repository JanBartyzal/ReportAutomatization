**Feature Specification: Excel Upload & Appendix Association**

**Feature ID:** FEAT-05 Parent Epic: Excel Viewer / Data Import
**Related DoD:** `docs/dod_criteria.md` 
**Related Standards:** `docs/project_standarts.md`, `docs/project_defaults.md`

**1. Problem Statement**
PowerPoint presentations are not the only data sources for financial reports and OPEX planning. Users often maintain supporting calculations, detailed breakdowns, or mapping tables in Excel files. To provide a complete analysis, the system must support uploading these Excel files, parsing their content, and attaching the extracted data as an "appendix" to the main PowerPoint data structure.

**2. User Story**
As a Financial Analyst, I want to upload an .xlsx file via the Import Opex page (/import/opex/excel), So that the tables from the Excel file are converted to JSON and attached to the currently processed Plan/Presentation as a data appendix.

**3. Functional Requirements**
**3.1 Frontend (React)**
Location: Route /import/upload/opex/excel (or a modal within the Opex Dashboard).
Modify current PPTX upload page /import/upload/opex/ to /import/upload/opex/pptx to accept Poterpoint file
Modify both uploadpages to accept Powerpoint or Excel files and process it via file-specific endpoint


**UI Components:**
Drag & Drop zone accepting .xlsx and .xls files.
File validation (max size e.g., 20MB, correct extension).
Progress bar indicating upload status.
Plan Association: A selector (dropdown or context) to choose which existing PPTX Plan this Excel file belongs to (unless the upload happens within the context of an open Plan).

Action: On upload success, trigger a refresh of the Plan detail view to show the newly attached appendix data.

**3.2 Backend (FastAPI)**
Endpoint: POST /api/import/upload/opex/excel
Modify current endpoint /api/import/uploadopex to /api/import/upload/opex/pptx

**Processing:**
Parse the Excel file using a library like pandas or openpyxl.
Iterate through all Sheets.
Detect data regions (tables).
Convert data to the standardized JSON schema.
Storage:
Update the JSON document of the associated PowerPoint Plan.
Add/Update the appendix field in the database record.

**4. Technical Requirements**
**Algorithm Steps**
Receive File: Accept multipart/form-data upload.

Validate: Check mime-type (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet).

Parse Sheets:
For each sheet in the workbook:
Treat the used range as a table.
First row = Headers (keys).
Subsequent rows = Values.
Sanitize: Handle NaN, Infinite, and Date objects (convert to ISO string).

Merge:
Fetch the existing Plan JSON by ID.
Append the parsed Excel data to the appendix node.
Save the updated structure to the database.

**API Contract (Draft)**
Request: POST /api/import/append-excel/{plan_id} Body: FormData (file: binary)
Response (200 OK):

**JSON**
{
  "message": "Excel data successfully appended.",
  "sheets_processed": 3,
  "total_rows": 150
}

**5. Output Format (JSON Schema)**
The Excel data will be stored under a new appendix key within the main Plan JSON. The structure mimics the native table schema but groups data by Sheet.

**JSON**

{
  "plan_id": "uuid...",
  "slides": [ ... ],
  "appendix": {
    "source_file": "calculation_v2.xlsx",
    "uploaded_at": "2026-01-30T12:00:00Z",
    "sheets": [
      {
        "sheet_name": "Sheet1",
        "tables": [
          {
            "id": "excel-sheet1-range-A1",
            "name": "Cost Breakdown",
            "rows": [
              {
                "Category": "Marketing",
                "Q1": 10000,
                "Q2": 12000,
                "Q3": 11000,
                "Q4": 15000
              },
              {
                "Category": "IT",
                "Q1": 5000,
                "Q2": 5000,
                "Q3": 6000,
                "Q4": 6000
              }
            ]
          }
        ]
      }
    ]
  }
}

**6. Acceptance Criteria**
[ ] User can upload a valid .xlsx file.
[ ] System rejects non-Excel files.
[ ] All visible sheets are parsed into JSON.
[ ] The JSON is successfully merged into the parent PPTX Plan object in the database.
[ ] The frontend displays a success message after processing.
[ ] Empty sheets are ignored.