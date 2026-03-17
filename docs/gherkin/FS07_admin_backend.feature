Feature: FS07 – Admin Backend & UI
  As a platform administrator
  I want to manage roles, secrets, API keys, failed jobs, forms, and templates
  So that I can control access, maintain security, and recover from processing failures

  Background:
    Given the user is authenticated via Azure Entra ID
    And the user has a valid JWT with role and org_id claims
    And the API Gateway routes admin requests to engine-core:admin

  # ---------------------------------------------------------------------------
  # Role Management
  # ---------------------------------------------------------------------------

  @engine-core:auth @roles
  Scenario Outline: Role-based access control enforces permissions
    Given a user with role "<role>" in org_id "org-001"
    When the user attempts to access "<endpoint>" with method "<method>"
    Then the response status is <status>

    Examples:
      | role    | endpoint                     | method | status |
      | Admin   | /api/v1/admin/roles          | GET    | 200    |
      | Admin   | /api/v1/admin/roles          | POST   | 200    |
      | Editor  | /api/v1/admin/roles          | POST   | 403    |
      | Viewer  | /api/v1/admin/roles          | GET    | 403    |
      | Viewer  | /api/v1/query/records        | GET    | 200    |

  @engine-core:auth @roles
  Scenario: Admin assigns a role to a user within the holding structure
    Given an Admin user in holding "holding-001"
    When the Admin assigns the "Editor" role to user "user-042" for company "company-005"
    Then user "user-042" has the "Editor" role scoped to "company-005"
    And user "user-042" cannot access data from "company-006"

  @engine-core:auth @roles @hierarchy
  Scenario: Hierarchical permissions cascade from holding to subsidiary
    Given a HoldingAdmin for holding "holding-001" which contains companies "company-A" and "company-B"
    When the HoldingAdmin queries data across the holding
    Then the response includes data from both "company-A" and "company-B"
    And a CompanyAdmin for "company-A" can only see data from "company-A"

  @engine-core:auth @roles @reviewer
  Scenario: Reviewer sub-role of HoldingAdmin can approve or reject reports
    Given a user with the "Reviewer" sub-role under HoldingAdmin for holding "holding-001"
    When the Reviewer accesses the report approval endpoint "/api/v1/reports/pending"
    Then the response status is 200
    And the Reviewer can approve or reject submitted reports
    But the Reviewer cannot modify admin settings or manage roles

  # ---------------------------------------------------------------------------
  # Secrets Management
  # ---------------------------------------------------------------------------

  @engine-core:admin @secrets
  Scenario: Superadmin views current secrets metadata from KeyVault
    Given a user with the "Superadmin" role
    When the Superadmin accesses "/api/v1/admin/secrets"
    Then the response lists secret names and last-updated timestamps
    And no secret values are included in the response

  @engine-core:admin @secrets
  Scenario: Superadmin updates a secret in Azure KeyVault
    Given a user with the "Superadmin" role
    When the Superadmin sends a PUT request to "/api/v1/admin/secrets/DB_PASSWORD" with a new value
    Then the secret is updated in Azure KeyVault
    And the response confirms the update with a new version identifier
    And the old secret value is retained as a previous version in KeyVault

  @engine-core:admin @secrets
  Scenario: Non-superadmin cannot access secrets management
    Given a user with the "Admin" role but not "Superadmin"
    When the user attempts to access "/api/v1/admin/secrets"
    Then the response status is 403
    And the response body contains "Insufficient permissions for secrets management"

  # ---------------------------------------------------------------------------
  # API Key Management
  # ---------------------------------------------------------------------------

  @engine-core:admin @api-keys
  Scenario: Admin generates an API key for a service account
    Given an Admin user in org_id "org-001"
    When the Admin sends a POST request to "/api/v1/admin/api-keys" with body:
      """
      {
        "service_name": "external-etl",
        "permissions": ["read:data", "write:upload"],
        "expires_in_days": 90
      }
      """
    Then the response status is 201
    And the response contains the plaintext API key exactly once
    And the API key is stored in the database as a bcrypt hash
    And the plaintext key is never persisted or logged

  @engine-core:admin @api-keys
  Scenario: API key is validated on subsequent requests
    Given a service account sends a request with header "X-API-Key: <valid-key>"
    When engine-core:auth validates the API key
    Then the bcrypt hash of the provided key matches the stored hash
    And the request is authorized with the permissions assigned to the key

  @engine-core:admin @api-keys
  Scenario: Expired API key is rejected
    Given an API key was generated with an expiration of 90 days
    And the key has expired
    When a request is sent with the expired API key
    Then the response status is 401
    And the response body contains "API key expired"

  @engine-core:admin @api-keys
  Scenario: Admin revokes an existing API key
    Given an active API key "key-abc-123" exists for service "external-etl"
    When the Admin sends a DELETE request to "/api/v1/admin/api-keys/key-abc-123"
    Then the response status is 200
    And subsequent requests using "key-abc-123" receive a 401 response

  # ---------------------------------------------------------------------------
  # Failed Jobs UI
  # ---------------------------------------------------------------------------

  @engine-core:admin @failed-jobs
  Scenario: Admin views the list of failed jobs
    Given the failed_jobs table contains 5 failed processing records
    When the Admin accesses "/api/v1/admin/failed-jobs"
    Then the response status is 200
    And the response contains 5 entries
    And each entry includes "job_id", "file_id", "error_detail", "failed_at", and "status"

  @engine-core:admin @failed-jobs
  Scenario: Admin views detailed error information for a failed job
    Given a failed job with job_id "job-777" exists with a stack trace
    When the Admin accesses "/api/v1/admin/failed-jobs/job-777"
    Then the response status is 200
    And the response includes the full error detail and stack trace
    And the response includes the original file metadata

  @engine-core:admin @failed-jobs
  Scenario: Admin reprocesses a failed job
    Given a failed job with job_id "job-777" exists in status "FAILED"
    When the Admin clicks "Reprocess" which sends a POST to "/api/v1/admin/failed-jobs/job-777/reprocess"
    Then the response status is 202
    And the job status changes to "REQUEUED"
    And engine-orchestrator receives a reprocessing trigger for the associated file

  @engine-core:admin @failed-jobs
  Scenario: Reprocessing a job that is not in FAILED status is rejected
    Given a job with job_id "job-888" exists in status "COMPLETED"
    When the Admin attempts to reprocess job "job-888"
    Then the response status is 409
    And the response body contains "Job is not in a reprocessable state"

  # ---------------------------------------------------------------------------
  # Form and Template Management
  # ---------------------------------------------------------------------------

  @engine-core:admin @forms
  Scenario: Admin lists all form definitions for the organization
    Given form definitions exist for org_id "org-001"
    When the Admin accesses "/api/v1/admin/forms"
    Then the response status is 200
    And the response contains a list of form definitions with their status and version

  @engine-core:admin @forms
  Scenario: Admin creates a new form definition
    Given an Admin user in org_id "org-001"
    When the Admin sends a POST to "/api/v1/admin/forms" with a form schema definition
    Then the response status is 201
    And the form is saved with version 1 and status "DRAFT"

  @engine-core:admin @templates
  Scenario: Admin manages report templates
    Given an Admin user in org_id "org-001"
    When the Admin accesses "/api/v1/admin/templates"
    Then the response status is 200
    And the response lists available report templates with metadata
