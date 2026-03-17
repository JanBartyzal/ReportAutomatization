Feature: FS03 - Atomizers
  As the orchestrator service
  I want to invoke specialized atomizers for each file type
  So that uploaded files are parsed into structured data for downstream storage and analysis

  # ---------------------------------------------------------------------------
  # Common Atomizer Behavior
  # ---------------------------------------------------------------------------

  Scenario: Atomizer returns 200 with structured JSON for a valid file
    Given a valid file has been uploaded and stored in Blob Storage
    When the orchestrator invokes the appropriate atomizer via Dapr gRPC
    Then the atomizer returns HTTP status 200
    And the response contains structured JSON or an artifact_url
    And the response never contains inline binary data

  Scenario: Atomizer returns 422 for a corrupt file
    Given a corrupt file has been uploaded and stored in Blob Storage
    When the orchestrator invokes the appropriate atomizer via Dapr gRPC
    Then the atomizer returns HTTP status 422
    And the response contains an error detail explaining the parsing failure
    And the atomizer does NOT return HTTP status 500

  Scenario: Atomizer downloads the file from Blob Storage via URL
    Given the orchestrator sends a gRPC request containing a blob_url
    When the atomizer processes the request
    Then the atomizer downloads the file directly from Blob Storage using the provided URL
    And the atomizer does NOT expect the file to be sent inline in the request

  # ---------------------------------------------------------------------------
  # PPTX Atomizer (processor-atomizers:pptx)
  # ---------------------------------------------------------------------------

  Scenario: PPTX ExtractStructure returns slide structure
    Given a valid PPTX file is stored in Blob Storage
    When the orchestrator calls gRPC ExtractStructure on processor-atomizers:pptx
    Then the response contains a list of slides with their indices and layout names

  Scenario: PPTX ExtractSlideContent returns texts, tables, and notes
    Given a valid PPTX file is stored in Blob Storage
    When the orchestrator calls gRPC ExtractSlideContent on processor-atomizers:pptx for slide index 1
    Then the response contains:
      | Field  | Description                          |
      | texts  | Array of text blocks from the slide  |
      | tables | Array of table structures             |
      | notes  | Speaker notes for the slide          |

  Scenario: PPTX RenderSlideImage generates PNG thumbnail
    Given a valid PPTX file is stored in Blob Storage
    When the orchestrator calls gRPC RenderSlideImage on processor-atomizers:pptx for slide index 1
    Then a PNG image of resolution 1280x720 is generated via LibreOffice Headless
    And the image is stored in Blob Storage
    And the response contains the artifact_url of the stored image

  Scenario: PPTX MetaTable logic flags low confidence results
    Given a PPTX slide contains a table
    When the atomizer extracts the table with AI-assisted analysis
    And the confidence score is below 0.85
    Then the result is returned as plain text with a "low_confidence" flag set to true

  Scenario: PPTX MetaTable logic returns structured table for high confidence
    Given a PPTX slide contains a well-formed table
    When the atomizer extracts the table with AI-assisted analysis
    And the confidence score is 0.85 or higher
    Then the result is returned as a structured table object

  # ---------------------------------------------------------------------------
  # Excel Atomizer (processor-atomizers:xls)
  # ---------------------------------------------------------------------------

  Scenario: Excel ExtractStructure returns sheet list
    Given a valid XLSX file is stored in Blob Storage
    When the orchestrator calls gRPC ExtractStructure on processor-atomizers:xls
    Then the response contains a list of sheet names and their indices

  Scenario: Excel ExtractSheetContent returns headers, rows, and data types
    Given a valid XLSX file is stored in Blob Storage
    When the orchestrator calls gRPC ExtractSheetContent on processor-atomizers:xls for sheet "Sheet1"
    Then the response contains:
      | Field      | Description                              |
      | headers    | Array of column header names             |
      | rows       | Array of row data objects                |
      | data_types | Array of detected data types per column  |

  Scenario: Excel Atomizer handles partial success
    Given a XLSX file with 10 sheets is stored in Blob Storage
    And 1 of the 10 sheets contains corrupt data
    When the orchestrator calls gRPC ExtractStructure on processor-atomizers:xls
    Then the response status is "PARTIAL"
    And the response contains successfully parsed data for 9 sheets
    And the response includes an error detail for the 1 failed sheet

  # ---------------------------------------------------------------------------
  # PDF/OCR Atomizer (processor-atomizers:pdf)
  # ---------------------------------------------------------------------------

  Scenario: PDF Atomizer detects and extracts text from a text-based PDF
    Given a text-based PDF file is stored in Blob Storage
    When the orchestrator calls gRPC on processor-atomizers:pdf
    Then the text is extracted directly without OCR
    And the response contains structured text blocks

  Scenario: PDF Atomizer detects and OCRs a scanned PDF
    Given a scanned (image-based) PDF file is stored in Blob Storage
    When the orchestrator calls gRPC on processor-atomizers:pdf
    Then the PDF is processed via Tesseract OCR
    And the response contains the extracted text

  Scenario: PDF Atomizer distinguishes mixed text and scanned pages
    Given a PDF file with both text-based and scanned pages is stored in Blob Storage
    When the orchestrator calls gRPC on processor-atomizers:pdf
    Then text pages are extracted directly
    And scanned pages are processed via Tesseract OCR
    And the combined result is returned as structured JSON

  # ---------------------------------------------------------------------------
  # CSV Atomizer (processor-atomizers:csv)
  # ---------------------------------------------------------------------------

  Scenario Outline: CSV Atomizer auto-detects delimiter
    Given a CSV file using "<delimiter>" as delimiter is stored in Blob Storage
    When the orchestrator calls gRPC on processor-atomizers:csv
    Then the atomizer correctly identifies the delimiter as "<delimiter>"
    And the response contains parsed rows and headers

    Examples:
      | delimiter |
      | ,         |
      | ;         |
      | \|        |
      | \t        |

  Scenario: CSV Atomizer auto-detects encoding
    Given a CSV file encoded in ISO-8859-1 is stored in Blob Storage
    When the orchestrator calls gRPC on processor-atomizers:csv
    Then the atomizer correctly detects the encoding
    And the response contains properly decoded text

  Scenario: CSV Atomizer auto-detects header row
    Given a CSV file with a header row is stored in Blob Storage
    When the orchestrator calls gRPC on processor-atomizers:csv
    Then the atomizer identifies the first row as the header
    And the response uses header values as column names

  # ---------------------------------------------------------------------------
  # AI Gateway (processor-atomizers:ai)
  # ---------------------------------------------------------------------------

  Scenario: AI Gateway performs semantic analysis via AnalyzeSemantic
    Given extracted content from a file is available
    When the orchestrator calls gRPC AnalyzeSemantic on processor-atomizers:ai
    Then the response contains:
      | Field              | Description                              |
      | classification     | Document type classification             |
      | summarization      | Brief summary of the content             |
      | entity_extraction  | List of extracted entities and values     |

  Scenario: AI Gateway enforces token logging
    Given the orchestrator calls gRPC AnalyzeSemantic on processor-atomizers:ai
    When the LLM processes the request via LiteLLM
    Then the token usage (prompt_tokens, completion_tokens) is logged

  Scenario: AI Gateway returns 429 when quota is exceeded
    Given a user's organization has exceeded its AI token quota
    When the orchestrator calls gRPC AnalyzeSemantic on processor-atomizers:ai
    Then the AI Gateway returns HTTP status 429
    And the response contains "quota_exceeded"

  # ---------------------------------------------------------------------------
  # Cleanup Worker (processor-atomizers:cleanup)
  # ---------------------------------------------------------------------------

  Scenario: Cleanup Worker deletes temp files older than 24 hours
    Given temporary files exist in Blob Storage that are older than 24 hours
    When the Cleanup Worker CronJob executes (runs every hour)
    Then all temporary files older than 24 hours are deleted from Blob Storage

  Scenario: Cleanup Worker deletes generator temp files
    Given generator temporary files exist in Blob Storage older than 24 hours
    When the Cleanup Worker CronJob executes
    Then the generator temporary files older than 24 hours are also deleted

  Scenario: Cleanup Worker does not delete files younger than 24 hours
    Given temporary files exist in Blob Storage that are less than 24 hours old
    When the Cleanup Worker CronJob executes
    Then those temporary files are NOT deleted
