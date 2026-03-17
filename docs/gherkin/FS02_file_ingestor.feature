Feature: FS02 - File Ingestor
  As a platform user
  I want to securely upload files for automated processing
  So that my PPTX, XLSX, PDF, and CSV files are ingested, scanned, sanitized, and stored reliably

  # ---------------------------------------------------------------------------
  # Upload Endpoint
  # ---------------------------------------------------------------------------

  Scenario: Successful file upload via multipart/form-data
    Given the user is authenticated with role "Editor"
    When the user sends a POST request to /api/upload with a valid 10MB PPTX file
    And the upload_purpose is "PARSE"
    Then the server returns HTTP status 202
    And the response contains a file_id and upload status "ACCEPTED"

  Scenario: Successful file upload via chunked streaming
    Given the user is authenticated with role "Editor"
    When the user sends a POST request to /api/upload using chunked transfer encoding with a valid XLSX file
    And the upload_purpose is "FORM_IMPORT"
    Then the server returns HTTP status 202
    And the response contains a file_id

  Scenario: Upload purpose PARSE triggers parsing pipeline
    Given the user uploads a file with upload_purpose "PARSE"
    When the upload completes successfully
    Then the orchestrator event contains purpose "PARSE"

  Scenario: Upload purpose FORM_IMPORT triggers form import pipeline
    Given the user uploads a file with upload_purpose "FORM_IMPORT"
    When the upload completes successfully
    Then the orchestrator event contains purpose "FORM_IMPORT"

  # ---------------------------------------------------------------------------
  # Streaming to Azure Blob Storage
  # ---------------------------------------------------------------------------

  Scenario: File is streamed to Blob Storage without full memory buffering
    Given the user uploads a 40MB PPTX file
    When the upload is in progress
    Then the service memory usage does not exceed the configured stream buffer size
    And the file is written to Azure Blob Storage incrementally

  Scenario: Blob naming follows the required convention
    Given a user with org_id "acme-corp" uploads a file named "Q4_report.pptx"
    When the file is stored in Blob Storage
    Then the blob path matches the pattern "{org_id}/{yyyy}/{MM}/{file_id}/Q4_report.pptx"

  # ---------------------------------------------------------------------------
  # File Size Limits
  # ---------------------------------------------------------------------------

  Scenario Outline: File size limits are enforced by file type
    Given the user uploads a "<file_type>" file of size <size_mb> MB
    When the upload is processed
    Then the server returns HTTP status <status>

    Examples:
      | file_type | size_mb | status |
      | PPTX      | 50      | 202    |
      | PPTX      | 51      | 413    |
      | XLSX      | 50      | 202    |
      | XLSX      | 51      | 413    |
      | CSV       | 50      | 202    |
      | CSV       | 51      | 413    |
      | PDF       | 100     | 202    |
      | PDF       | 101     | 413    |

  # ---------------------------------------------------------------------------
  # MIME Type Validation
  # ---------------------------------------------------------------------------

  Scenario Outline: Allowed MIME types are accepted
    Given the user uploads a file with extension "<extension>" and valid magic number
    When the MIME type is validated
    Then the upload is accepted

    Examples:
      | extension |
      | .pptx     |
      | .xlsx     |
      | .pdf      |
      | .csv      |

  Scenario: Disallowed MIME type is rejected with 415
    Given the user uploads a file with extension ".exe"
    When the MIME type is validated
    Then the server returns HTTP status 415
    And the response body contains "unsupported_media_type"

  Scenario: MIME type spoofing is detected via magic number check
    Given the user uploads a file with extension ".pptx" but the content is actually an EXE binary
    When the magic number check is performed
    Then the server returns HTTP status 415
    And the response body contains "mime_type_mismatch"

  # ---------------------------------------------------------------------------
  # ClamAV Antivirus Scan
  # ---------------------------------------------------------------------------

  Scenario: Clean file passes ClamAV scan
    Given the user uploads a clean PPTX file
    When the file is scanned via ClamAV on TCP socket port 3310
    Then the scan returns status "CLEAN"
    And the file is stored in Blob Storage

  Scenario: Infected file is rejected with 422
    Given the user uploads a file containing the EICAR test virus signature
    When the file is scanned via ClamAV on TCP socket port 3310
    Then the server returns HTTP status 422
    And the response body contains:
      """
      { "error": "INFECTED", "details": "..." }
      """
    And the file is NOT stored in Blob Storage

  Scenario: ClamAV scan occurs before blob storage write
    Given the user uploads a file
    When the file enters the ingestion pipeline
    Then the ClamAV scan completes BEFORE any write to Blob Storage

  # ---------------------------------------------------------------------------
  # Sanitization
  # ---------------------------------------------------------------------------

  Scenario: VBA macros are removed during sanitization
    Given the user uploads a PPTX file containing VBA macros
    When the file is sanitized
    Then the sanitized file does not contain any VBA macros
    And the sanitized file is stored permanently in Blob Storage

  Scenario: External links are removed during sanitization
    Given the user uploads a PPTX file containing external links
    When the file is sanitized
    Then the sanitized file does not contain external links

  Scenario: Raw files are retained for 90 days
    Given a file has been uploaded and sanitized
    When 90 days have elapsed since the upload
    Then the raw (unsanitized) file is eligible for deletion
    And the sanitized file remains in permanent storage

  # ---------------------------------------------------------------------------
  # Metadata Persistence
  # ---------------------------------------------------------------------------

  Scenario: Upload metadata is stored in PostgreSQL
    Given a user with org_id "acme-corp" uploads a file named "budget.xlsx"
    When the upload completes successfully
    Then a record is created in PostgreSQL with the following fields:
      | Field           | Value                    |
      | UserId          | <authenticated_user_id>  |
      | OrgId           | acme-corp                |
      | Filename        | budget.xlsx              |
      | MimeType        | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet |
      | ScanStatus      | CLEAN                    |
    And the record includes Size, UploadTimestamp, and BlobUrl

  # ---------------------------------------------------------------------------
  # Orchestrator Trigger
  # ---------------------------------------------------------------------------

  Scenario: Orchestrator event is triggered via Dapr PubSub after successful save
    Given a file has been uploaded, scanned, sanitized, and stored
    When the metadata record is saved to PostgreSQL
    Then a Dapr PubSub event is published to the orchestrator topic
    And the event contains file_id, org_id, blob_url, mime_type, and upload_purpose

  Scenario: Orchestrator event is delivered within 1 second of blob save
    Given a file has been successfully saved to Blob Storage
    When the Dapr PubSub event is published
    Then the engine-orchestrator service receives the event within 1 second

  # ---------------------------------------------------------------------------
  # Performance
  # ---------------------------------------------------------------------------

  Scenario: 20MB PPTX upload completes within 5 seconds on 100Mbps connection
    Given a network connection with at least 100 Mbps bandwidth
    When the user uploads a 20MB PPTX file
    Then the upload completes and returns a response within 5 seconds
