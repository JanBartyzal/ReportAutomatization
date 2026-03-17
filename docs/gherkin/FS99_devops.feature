Feature: FS99 - DevOps & Observability
  As a platform engineer
  I want automated CI/CD pipelines, distributed tracing, centralized logging, and metrics
  So that the platform is reliably built, deployed, and observable across all services

  # ---------------------------------------------------------------------------
  # CI/CD Pipeline
  # ---------------------------------------------------------------------------

  Scenario: Standard pipeline executes all stages in order
    Given source code is pushed to the main branch
    When the CI/CD pipeline is triggered
    Then the pipeline executes stages in order: Linting, Unit Tests, Integration Tests, Docker Build, Push to Registry
    And the pipeline fails fast if any stage fails

  Scenario: Linting stage catches code style violations
    Given source code contains a style violation
    When the Linting stage runs
    Then the pipeline fails with a linting error
    And the error details identify the file and rule violation

  Scenario: Unit test stage runs all service tests
    Given all microservices have unit tests
    When the Unit Tests stage runs
    Then tests for Java services are executed with JUnit 5
    And tests for Python services are executed with PyTest
    And tests for React frontend are executed with Vitest
    And a test coverage report is generated

  Scenario: Integration test stage validates service interactions
    Given the Docker Build stage has completed
    When the Integration Tests stage runs
    Then services are started with Dapr sidecars in the test environment
    And end-to-end API tests are executed against the running services

  Scenario: Docker images are pushed to the container registry
    Given all test stages have passed
    When the Docker Build stage completes
    Then Docker images are tagged with the commit SHA and "latest"
    And images are pushed to the configured container registry

  Scenario: GraalVM Native Image build runs on release pipeline
    Given a release tag is created on the repository
    When the release CI/CD pipeline is triggered
    Then a separate pipeline stage builds GraalVM Native Images for Java services
    And the native images achieve sub-second startup time
    And native images are pushed to the container registry with the release tag

  # ---------------------------------------------------------------------------
  # OpenTelemetry Tracing
  # ---------------------------------------------------------------------------

  Scenario: End-to-end trace spans the full request path
    Given all services are instrumented with OpenTelemetry
    When a user uploads a file through the Frontend
    Then a single trace ID is propagated through Frontend, API Gateway, engine-orchestrator, Atomizer, and Sink
    And the trace is visible in Jaeger or Tempo

  Scenario: Trace includes all service hops
    Given a file upload request is processed
    When I view the trace in the trace backend
    Then the trace contains spans for engine-ingestor, engine-orchestrator, processor-atomizers, and engine-data (sink modules)
    And each span shows duration, status, and service name

  Scenario: Failed requests include error details in trace
    Given a file upload fails at the Atomizer stage
    When I view the trace in the trace backend
    Then the failed span includes the error message and stack trace
    And the trace status is marked as ERROR

  # ---------------------------------------------------------------------------
  # Centralized Logging
  # ---------------------------------------------------------------------------

  Scenario: All services emit structured JSON logs
    Given a microservice is running
    When the service processes a request
    Then log entries are emitted in structured JSON format
    And each log entry contains fields: timestamp, level, service, trace_id, message

  Scenario: Logs are collected in the centralized logging backend
    Given all services emit structured JSON logs
    When logs are shipped to the logging backend (Loki or ELK)
    Then logs from all services are searchable in a single interface
    And logs can be filtered by service name, log level, and trace_id

  Scenario: Logs can be correlated with traces
    Given a request has been processed with trace_id "abc-123"
    When I search for trace_id "abc-123" in the logging backend
    Then all log entries from all services involved in that request are returned
    And the logs are ordered chronologically

  # ---------------------------------------------------------------------------
  # Metrics & Dashboards
  # ---------------------------------------------------------------------------

  Scenario: Prometheus scrapes metrics from every service
    Given all services expose a /metrics endpoint
    When Prometheus performs a scrape cycle
    Then metrics are collected from every running service
    And the metrics include JVM stats (Java), process stats (Python), and custom application metrics

  Scenario: Grafana dashboard shows error rate
    Given Prometheus is collecting metrics from all services
    When I open the Grafana error rate dashboard
    Then the dashboard displays HTTP error rates (4xx, 5xx) per service
    And the data refreshes at the configured interval

  Scenario: Grafana dashboard shows engine-orchestrator workflow queue depth
    Given engine-orchestrator exposes a metric for workflow queue depth
    When I open the Grafana engine-orchestrator dashboard
    Then the current queue depth is displayed
    And historical queue depth trend is visible

  Scenario: Grafana dashboard shows Atomizer latency
    Given Atomizer services expose processing latency metrics
    When I open the Grafana Atomizer dashboard
    Then p50, p95, and p99 latency percentiles are displayed
    And latency is broken down by Atomizer type (PPTX, XLS, PDF)

  Scenario: Grafana dashboard shows DB connection pool utilization
    Given database connection pool metrics are exposed by Java services
    When I open the Grafana DB dashboard
    Then active connections, idle connections, and pool max size are displayed
    And alerts are configured for pool exhaustion (>90% utilization)

  # ---------------------------------------------------------------------------
  # Local Development Environment
  # ---------------------------------------------------------------------------

  Scenario: Tilt up starts complete local topology
    Given Docker Desktop is running
    And a Tiltfile is present in the repository root
    When I execute "tilt up"
    Then all microservices start in local Kubernetes (Kind) or Docker Compose
    And Dapr sidecars are attached to each service
    And the platform is accessible at the configured local ports

  Scenario: React frontend supports hot-reload in local dev
    Given the local development environment is running via "tilt up"
    When I modify a React component source file
    Then the change is reflected in the browser without manual restart
    And the hot-reload completes within 5 seconds

  Scenario: Python services support hot-reload in local dev
    Given the local development environment is running via "tilt up"
    When I modify a Python FastAPI source file
    Then the service restarts automatically with the updated code
    And the reload completes within 10 seconds
