Feature: FS09 – Frontend SPA
  As a platform user
  I want a responsive single-page application with authentication, file upload, data viewing, and orchestration controls
  So that I can interact with the platform seamlessly from the browser

  Background:
    Given the frontend is built with React 18, Vite, and TypeScript
    And the application uses TanStack Query, Zustand, Tailwind CSS, and FluentUI
    And all API calls go through Axios with interceptors

  # ---------------------------------------------------------------------------
  # Authentication – MSAL Provider
  # ---------------------------------------------------------------------------

  @frontend @auth @MSAL
  Scenario: User logs in via Azure Entra ID
    Given the user is not authenticated
    When the user clicks the "Sign In" button
    Then the MSAL Provider initiates the Azure Entra ID login flow
    And upon successful authentication the user is redirected to the dashboard
    And the access token is stored in the MSAL cache

  @frontend @auth @MSAL
  Scenario: User logs out
    Given the user is authenticated
    When the user clicks the "Sign Out" button
    Then the MSAL Provider clears the session
    And the user is redirected to the login page
    And no access token remains in the browser cache

  @frontend @auth @token-refresh
  Scenario: Axios interceptor automatically refreshes expired tokens
    Given the user is authenticated with an access token that is about to expire
    When the user makes an API request
    Then the Axios interceptor calls acquireTokenSilent before the request
    And the refreshed Bearer token is attached to the request header
    And the API call succeeds without user interaction

  @frontend @auth @token-refresh
  Scenario: Token refresh with retry on failure
    Given the silent token acquisition fails on the first attempt
    When the Axios interceptor retries the token acquisition
    Then the retry succeeds and the request proceeds with the new token
    And if all retries fail the user is redirected to the login page

  @frontend @auth @token-refresh
  Scenario: Interceptor falls back to first account when active account is not set
    Given no active account is set in the MSAL instance
    And at least one account exists from getAllAccounts()
    When the Axios interceptor prepares a request
    Then it uses the first account from getAllAccounts() for acquireTokenSilent
    And the request proceeds normally

  # ---------------------------------------------------------------------------
  # Upload Manager
  # ---------------------------------------------------------------------------

  @frontend @upload @drag-drop
  Scenario: User uploads a file via drag and drop
    Given the user is on the Upload page
    When the user drags a .pptx file into the drop zone (react-dropzone)
    Then the file is accepted and displayed in the upload queue
    And the upload starts automatically

  @frontend @upload @progress
  Scenario: Upload progress bar shows real-time status
    Given the user has initiated a file upload
    When the upload is in progress
    Then the progress bar displays the percentage completed via XHR onUploadProgress
    And the progress updates in real time

  @frontend @upload @auto-refresh
  Scenario: File list refreshes automatically after upload completes
    Given the user has uploaded a file successfully
    When the upload response returns with status 201
    Then React Query invalidates the file list query cache
    And the file list refreshes automatically to include the new file

  @frontend @upload @validation
  Scenario: Upload rejects unsupported file types
    Given the user is on the Upload page
    When the user attempts to upload a file with extension ".exe"
    Then the file is rejected with a validation message "Unsupported file type"
    And the upload does not proceed

  @frontend @upload @multiple
  Scenario: User uploads multiple files in a single batch
    Given the user is on the Upload page
    When the user selects multiple .xlsx and .pptx files
    Then each file is uploaded individually with its own progress bar
    And the file list refreshes after all uploads complete

  # ---------------------------------------------------------------------------
  # Viewer – Read-Only Data Display
  # ---------------------------------------------------------------------------

  @frontend @viewer
  Scenario: User views parsed data slide by slide
    Given a PPTX file has been parsed and stored
    When the user opens the file in the Viewer
    Then parsed data is displayed slide by slide
    And each slide shows extracted text and table content

  @frontend @viewer @png-preview
  Scenario: Viewer shows PNG previews for each slide
    Given slide PNG images are stored in Blob Storage
    When the user views a parsed PPTX file
    Then each slide displays a PNG preview loaded from the Blob URL
    And the preview is shown alongside the extracted data

  @frontend @viewer @read-only
  Scenario: Viewer is read-only and does not allow editing
    Given the user is viewing parsed data in the Viewer
    When the user attempts to modify any displayed content
    Then no editable fields are available
    And all data is rendered as read-only components

  # ---------------------------------------------------------------------------
  # Real-Time Updates
  # ---------------------------------------------------------------------------

  @frontend @real-time @P1
  Scenario: Frontend polls for processing status updates (P1)
    Given a file has been uploaded and is being processed
    When the frontend activates React Query polling at 3-second intervals
    Then the file status updates from "PROCESSING" to "COMPLETED" automatically
    And the user sees the updated status without manual page refresh

  @frontend @real-time @P2
  Scenario: Frontend receives real-time updates via SSE or WebSocket (P2)
    Given SSE or WebSocket is enabled in the P2 phase
    When a file processing status changes
    Then the frontend receives a push notification through the real-time channel
    And the UI updates immediately without polling delay

  # ---------------------------------------------------------------------------
  # Orchestrator Trigger
  # ---------------------------------------------------------------------------

  @frontend @orchestrator
  Scenario: User triggers an engine-orchestrator workflow from the frontend
    Given the user has uploaded a file that requires processing
    When the user clicks "Start Processing"
    Then the frontend sends a POST request to the API Gateway
    And engine-orchestrator receives the workflow trigger
    And the file status changes to "PROCESSING"

  @frontend @orchestrator
  Scenario: User triggers PPTX generation from the frontend
    Given approved data exists for the selected reporting period
    When the user clicks "Generate Report"
    Then the frontend sends a POST to "/api/v1/orchestrator/generate-pptx"
    And the UI shows a processing indicator until completion

  # ---------------------------------------------------------------------------
  # Form Builder & Submission UI
  # ---------------------------------------------------------------------------

  @frontend @form-builder
  Scenario: Admin creates a form using the Form Builder UI
    Given an Admin user is on the Form Builder page
    When the Admin adds fields, sets validation rules, and saves the form
    Then the form definition is sent to the backend via POST
    And the form appears in the list of available forms

  @frontend @form-filling
  Scenario: User fills in and submits a form
    Given a published form exists for the user's organization
    When the user fills in all required fields and clicks "Submit"
    Then the form data is sent to the backend
    And the response confirms successful submission
    And the form status changes to "SUBMITTED"

  @frontend @submission-workflow
  Scenario: User views the submission workflow status
    Given the user has submitted a report through the platform
    When the user navigates to the Submissions page
    Then the submission status is displayed (Draft, Submitted, Approved, Rejected)
    And the user can see timestamps and reviewer comments

  # ---------------------------------------------------------------------------
  # Period Dashboard
  # ---------------------------------------------------------------------------

  @frontend @period-dashboard
  Scenario: HoldingAdmin views the period dashboard
    Given a reporting period "Q2/2025" is active
    When the HoldingAdmin navigates to the Period Dashboard
    Then the dashboard shows delivery status per subsidiary
    And aggregated data charts are rendered using Recharts or Nivo
    And the dashboard includes both file-sourced and form-sourced data

  # ---------------------------------------------------------------------------
  # PPTX Generator Trigger
  # ---------------------------------------------------------------------------

  @frontend @pptx-generator
  Scenario: User triggers PPTX report generation from the UI
    Given approved data exists and a PPTX template is configured
    When the user selects the template and clicks "Generate PPTX"
    Then the frontend sends the generation request to the API
    And a progress indicator is displayed
    And the generated PPTX download link appears upon completion
