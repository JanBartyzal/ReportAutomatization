Feature: FS12 - API & AI Integration / MCP
  As a platform integrator or AI agent
  I want secure API access and AI agent integration via MCP Server
  So that external systems and AI assistants can query platform data within strict security and cost boundaries

  # ---------------------------------------------------------------------------
  # API Key Authentication
  # ---------------------------------------------------------------------------

  Scenario: API request with valid API key succeeds
    Given a user has a valid API key issued by the platform
    When the user sends a request with the API key as Bearer token in the Authorization header
    Then the system authenticates the request
    And returns the requested data with HTTP status 200

  Scenario: API request without Authorization header is rejected
    Given a user does not include an Authorization header
    When the user sends a request to the API
    Then the system returns HTTP status 401
    And the response body contains "unauthorized"

  Scenario: API request with invalid API key is rejected
    Given a user sends a request with an invalid API key as Bearer token
    When the request reaches engine-core:auth
    Then the system returns HTTP status 401
    And the response body contains "invalid_api_key"

  Scenario: API request with expired API key is rejected
    Given a user has an API key that has been revoked or expired
    When the user sends a request with the expired API key
    Then the system returns HTTP status 401
    And the response body contains "api_key_expired"

  # ---------------------------------------------------------------------------
  # MCP Server - AI Agent Integration
  # ---------------------------------------------------------------------------

  Scenario: MCP Server accepts connection from AI agent with valid OAuth token
    Given an AI agent connects to processor-generators:mcp
    And the agent presents a valid OAuth token obtained via On-Behalf-Of flow
    When the MCP Server validates the token
    Then the connection is established
    And the agent inherits the permissions of the originating user

  Scenario: MCP Server rejects AI agent without valid OAuth token
    Given an AI agent connects to processor-generators:mcp without a valid OAuth token
    When the MCP Server attempts to validate the token
    Then the connection is rejected with an authentication error
    And no data access is granted

  Scenario: AI agent inherits user permissions via On-Behalf-Of flow
    Given a user with role "Editor" and org_id "org-789" initiates an AI query
    When the OAuth token is exchanged via On-Behalf-Of flow to processor-generators:mcp
    Then the AI agent operates with "Editor" permissions
    And the agent's data access is limited to org_id "org-789"

  Scenario: AI agent never receives global access
    Given an AI agent connects to processor-generators:mcp with a user-scoped OAuth token
    When the agent attempts to query data outside the user's org_id
    Then the system returns an empty result set
    And the query is logged as an out-of-scope access attempt

  # ---------------------------------------------------------------------------
  # MCP Security - Row-Level Security Enforcement
  # ---------------------------------------------------------------------------

  Scenario: MCP enforces RLS on every AI query
    Given an AI agent is connected to processor-generators:mcp with org_id "org-100"
    When the agent issues a data query
    Then the query is automatically scoped to org_id "org-100"
    And no data from other organizations is included in the response

  Scenario: MCP prevents SQL injection in AI-generated queries
    Given an AI agent sends a query containing SQL injection patterns
    When processor-generators:mcp processes the query
    Then the query is sanitized before execution
    And the system logs the injection attempt

  Scenario: MCP scopes AI query to user's organizational hierarchy
    Given a user belongs to org_id "org-100" under holding "Holding-A"
    And the user has access to divisions "Div-1" and "Div-2"
    When the AI agent queries data on behalf of this user
    Then the results include data only from "Div-1" and "Div-2" within "org-100"

  # ---------------------------------------------------------------------------
  # Cost Control - Token Quotas
  # ---------------------------------------------------------------------------

  Scenario: AI request within monthly user quota succeeds
    Given a user has consumed 8000 of their 10000 monthly token quota
    When the user triggers an AI query consuming 1500 tokens
    Then the query is processed successfully
    And the user's remaining quota is updated to 500 tokens

  Scenario: AI request exceeding monthly user quota returns 429
    Given a user has consumed 9800 of their 10000 monthly token quota
    When the user triggers an AI query that would consume 500 tokens
    Then the system returns HTTP status 429
    And the response body contains "user_quota_exceeded"
    And the response includes the quota reset date

  Scenario: AI request exceeding monthly company quota returns 429
    Given a company has consumed 95000 of their 100000 monthly token quota
    When any user from the company triggers an AI query that would exceed the quota
    Then the system returns HTTP status 429
    And the response body contains "company_quota_exceeded"

  Scenario: Token consumption is visible in Admin UI
    Given a user with role "Admin" is authenticated
    When the admin navigates to the AI cost control section
    Then the UI displays token consumption per user
    And the UI displays token consumption per company
    And the UI displays the remaining monthly quota

  Scenario: Monthly quotas reset at the beginning of each month
    Given a user consumed their entire monthly token quota in February 2026
    When the date changes to 2026-03-01
    Then the user's token quota is reset to the full monthly allocation
    And the company's token quota is recalculated

  # ---------------------------------------------------------------------------
  # API Key Management
  # ---------------------------------------------------------------------------

  Scenario: Admin generates a new API key for a user
    Given a user with role "Admin" is authenticated
    When the admin generates a new API key for user "editor@company.cz"
    Then a new API key is created and returned once
    And the key is associated with the user's org_id and role
    And the key is stored as a hashed value in the database

  Scenario: Admin revokes an existing API key
    Given a user with role "Admin" is authenticated
    And an API key exists for user "editor@company.cz"
    When the admin revokes the API key
    Then the API key is marked as revoked
    And subsequent requests with the revoked key return HTTP status 401
