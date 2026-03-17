Feature: FS01 - Infrastructure & Core
  As a platform operator
  I want containerized infrastructure with centralized authentication and service discovery
  So that all microservices run securely with consistent routing, auth, and secret management

  # ---------------------------------------------------------------------------
  # Containerized Infrastructure
  # ---------------------------------------------------------------------------

  Scenario: Local development topology starts with Tilt
    Given Docker Desktop is running
    And the Tiltfile is present in the repository root
    When I execute "tilt up"
    Then all services reach a healthy state within 5 minutes
    And Dapr sidecars are attached to each service

  Scenario: New Spring Boot service starts within 30 seconds
    Given a new Spring Boot 3.x service is created from the base image
    When the container is started
    Then the service health endpoint returns 200 within 30 seconds

  Scenario: New FastAPI service starts within 30 seconds
    Given a new FastAPI service is created from the base image
    When the container is started
    Then the service health endpoint returns 200 within 30 seconds

  # ---------------------------------------------------------------------------
  # API Gateway - Nginx Host-Based Routing
  # ---------------------------------------------------------------------------

  Scenario Outline: API Gateway routes requests to the correct backend service
    Given the API Gateway (Nginx) is running
    When a client sends a request to "<path>"
    Then the request is forwarded to "<backend_service>"

    Examples:
      | path          | backend_service |
      | /api/auth     | engine-core:auth         |
      | /api/upload   | engine-ingestor          |
      | /api/query    | engine-data:query          |

  # ---------------------------------------------------------------------------
  # Rate Limiting
  # ---------------------------------------------------------------------------

  Scenario: API rate limiting enforces 100 requests per second
    Given the API Gateway is running
    And a client sends 101 requests to /api/query within 1 second
    Then the 101st request receives HTTP status 429
    And the response body contains "rate limit exceeded"

  Scenario: Auth endpoint rate limiting enforces 10 requests per second with burst 20
    Given the API Gateway is running
    And a client sends 21 requests to /api/auth within 1 second
    Then the 21st request receives HTTP status 429

  Scenario: Upload endpoint rate limiting enforces 10 requests per second with burst 20
    Given the API Gateway is running
    And a client sends 21 requests to /api/upload within 1 second
    Then the 21st request receives HTTP status 429

  # ---------------------------------------------------------------------------
  # ForwardAuth
  # ---------------------------------------------------------------------------

  Scenario: ForwardAuth returns 401 when no token is provided
    Given the API Gateway is configured with ForwardAuth
    When a client sends a request without an Authorization header
    Then the API Gateway returns HTTP status 401
    And the response body contains "unauthorized"

  Scenario: ForwardAuth returns 403 for insufficient permissions
    Given the API Gateway is configured with ForwardAuth
    And a valid Azure Entra ID token is provided for a user with role "Viewer"
    When the user sends a request to an endpoint requiring role "Admin"
    Then the API Gateway returns HTTP status 403
    And the response body contains "forbidden"

  Scenario: ForwardAuth allows valid token with sufficient permissions
    Given the API Gateway is configured with ForwardAuth
    And a valid Azure Entra ID token is provided for a user with role "Admin"
    When the user sends a request to an endpoint requiring role "Admin"
    Then the request is forwarded to the backend service
    And the backend receives the validated user context

  # ---------------------------------------------------------------------------
  # CORS
  # ---------------------------------------------------------------------------

  Scenario: CORS rejects requests from non-whitelisted origins
    Given the API Gateway has a CORS whitelist configured
    When a request arrives with Origin header "https://evil.example.com"
    Then the response does not include an Access-Control-Allow-Origin header
    And the request is rejected

  Scenario: CORS allows requests from whitelisted origins
    Given the API Gateway has a CORS whitelist configured
    When a preflight OPTIONS request arrives with Origin header from the whitelist
    Then the response includes the correct Access-Control-Allow-Origin header

  # ---------------------------------------------------------------------------
  # Service Discovery - Dapr Sidecar Pattern
  # ---------------------------------------------------------------------------

  Scenario: Dapr sidecar is attached to each microservice
    Given a microservice is deployed with its Dapr sidecar configuration
    When the service starts
    Then the Dapr sidecar is running on the standard gRPC port 50001
    And the service can invoke other services via Dapr service invocation

  Scenario: Service-to-service communication uses Dapr gRPC
    Given engine-orchestrator needs to call processor-atomizers:pptx
    When engine-orchestrator invokes the service via Dapr gRPC sidecar
    Then the request is routed to the processor-atomizers:pptx instance
    And the response is returned through the Dapr sidecar

  # ---------------------------------------------------------------------------
  # Centralized Authentication - Azure Entra ID
  # ---------------------------------------------------------------------------

  Scenario: engine-core:auth validates Azure Entra ID v2.0 tokens
    Given engine-core:auth is running
    When a request arrives with a valid Azure Entra ID v2.0 Bearer token
    Then engine-core:auth returns HTTP status 200
    And the response contains the user's roles and org_id

  Scenario: engine-core:auth rejects expired tokens
    Given engine-core:auth is running
    When a request arrives with an expired Azure Entra ID token
    Then engine-core:auth returns HTTP status 401
    And the response body contains "token_expired"

  Scenario: engine-core:auth rejects tokens without AAD Security Group membership
    Given engine-core:auth is running
    And the user does not belong to any required AAD Security Group
    When the user sends a request with a valid token
    Then engine-core:auth returns HTTP status 403
    And the response body contains "security_group_required"

  # ---------------------------------------------------------------------------
  # RBAC Roles
  # ---------------------------------------------------------------------------

  Scenario Outline: RBAC enforces role-based access
    Given a user with role "<role>" is authenticated
    When the user attempts to "<action>"
    Then the system returns HTTP status <status>

    Examples:
      | role         | action                        | status |
      | Admin        | manage users and roles        | 200    |
      | Editor       | upload and edit files         | 200    |
      | Viewer       | view dashboards and data      | 200    |
      | Viewer       | upload files                  | 403    |
      | HoldingAdmin | manage holding hierarchy      | 200    |
      | HoldingAdmin | manage other holdings         | 403    |

  # ---------------------------------------------------------------------------
  # Organizational Hierarchy
  # ---------------------------------------------------------------------------

  Scenario: Org hierarchy enforces Holding to Company to Division structure
    Given a HoldingAdmin user is authenticated
    When the user creates a Company under their Holding
    And assigns a Division to that Company
    Then the hierarchy is stored as Holding > Company > Division
    And users scoped to the Company can only see their Division data

  Scenario: Org hierarchy supports Cost Center assignment
    Given a Company exists in the hierarchy
    When an Admin assigns a Cost Center to the Company
    Then the Cost Center is available for data tagging within that Company

  # ---------------------------------------------------------------------------
  # Azure KeyVault Integration
  # ---------------------------------------------------------------------------

  Scenario: Secrets are available from Azure KeyVault at startup
    Given a microservice is configured with AZURE_KEY_VAULT_ID
    When the service starts
    Then secrets from Azure KeyVault are available as environment variables
    And no secrets are stored in configuration files

  Scenario: Service fails gracefully when KeyVault is unreachable
    Given a microservice is configured with AZURE_KEY_VAULT_ID
    And Azure KeyVault is unreachable
    When the service starts
    Then the service logs an error indicating KeyVault connection failure
    And the service does not start with missing secrets
