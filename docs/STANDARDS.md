# Universal Project Standards
Context: This document defines the coding standards, architectural guidelines, and documentation requirements for this project.
Audience: All human developers and AI Agents contributing to the codebase.

## General Principles
Language: English is mandatory for all code, comments, commit messages, and documentation.
KISS & DRY: Keep It Simple, Stupid. Don't Repeat Yourself. Prefer readability over cleverness.
Functional Style: Prefer pure functions and immutability where possible (especially in React).
Type Safety: Strong typing is strictly required across all stacks to reduce AI hallucination.

## Tech Stack & Guidelines
**Java (Core Microservices)**
- Framework: Spring Boot 3.x.
- Compilation: GraalVM Native Image is mandatory for production builds to ensure sub-second startup.
- Asynchrony: Prefer Java 21 Virtual Threads (Project Loom) or CompletableFuture.
- Database: Entity Framework style approach via Spring Data JPA.

**Python (Data Conversion & AI)**
Framework: FastAPI for REST/gRPC endpoints.
Style: Follow PEP 8.
Typing: Mandatory Type Hints (typing module) and Pydantic models for all data structures.
AI Integration: All LLM calls MUST go through the LiteLLM gateway (OpenAI-compatible).

**React (Frontend)**
Build Tool: Vite.
Style: Functional components with Hooks only.
Props: TypeScript interfaces are strictly required for all props.
Communication: Always use REST API for frontend-to-backend calls.
Framework: React 18+ (Vite)
Jazyk: TypeScript
State Management: TanStack Query (React Query) – pro server state.
Styling: Tailwind CSS + Fluent UI (Microsoft Design Language).
Routing: React Router DOM.
HTTP Client: Axios (s interceptory).

## Communication & Infrastructure
Service Mesh: Dapr (Distributed Application Runtime) is mandatory for all service-to-service communication.

### Protocol Rules (strict)
- **Internal (service-to-service):** gRPC via Dapr sidecars is the **primary and mandatory** protocol. All Atomizers, Sinks, Orchestrator, and support services communicate exclusively via Dapr gRPC service invocation.
- **External (frontend-facing):** REST API exposed **only** through API Gateway (MS-GW). Only edge services (MS-AUTH, MS-ING upload endpoint, MS-QRY, MS-DASH) expose REST endpoints. Internal services (MS-ATM-*, MS-SINK-*, MS-TMPL, MS-ORCH) **never** expose REST endpoints to the outside.
- **Event-driven:** Dapr Pub/Sub for asynchronous inter-service events (e.g., `file-uploaded`, `report.status_changed`).
- **Frontend → Backend:** Always REST via API Gateway. Frontend never calls gRPC directly.
- **Nginx auth_request:** REST call from MS-GW to MS-AUTH for token validation (exception to gRPC rule – Nginx does not support gRPC auth_request).

### Protocol Selection Guide
| Communication Path | Protocol | Reason |
|---|---|---|
| Frontend → API Gateway | REST (HTTPS) | Browser compatibility |
| API Gateway → MS-AUTH (auth_request) | REST | Nginx auth_request limitation |
| API Gateway → MS-ING, MS-QRY, MS-DASH | REST | Frontend-facing edge services |
| MS-ING → MS-SCAN | Dapr gRPC | Internal service |
| MS-ING → MS-ORCH | Dapr Pub/Sub | Async event trigger |
| MS-ORCH → MS-ATM-* | Dapr gRPC | Internal processing |
| MS-ORCH → MS-SINK-* | Dapr gRPC | Internal persistence |
| MS-ORCH → MS-TMPL | Dapr gRPC | Internal mapping |
| MS-ORCH → MS-NOTIF | Dapr Pub/Sub | Async notification |
| MS-NOTIF → MS-FE | WebSocket / SSE | Real-time push |
| Any service → PostgreSQL / Redis | TCP | Direct data access |

Cloud: Optimized for Azure Container Apps and AWS Fargate.

## Documentation Standards
Module Level: Each microservice MUST have a README.md containing:
- Purpose and Dapr app-id.
- Mermaid diagrams for business processes and sequence flows.
- API documentation (Swagger/OpenAPI).
Testing Records: Each service must contain a test-result.md file, updated after every CI/CD test run.
Code Level: Complex logic requires a comment explaining why, not what.

## Specific Project Overrides
Architecture Pattern: Microservices with Dapr Sidecars.
Testing Framework: JUnit 5 (Java), PyTest (Python), Vitest (React).
Connectivity: TRUSTED_HOSTS for Kimai or proxy services must use the regex format (e.g., localhost|127.0.0.1|serverA).

## Network & Environment Defaults
Timezone/Date: UTC internally, ISO 8601 (YYYY-MM-DDTHH:mm:ssZ) for API.
Standard Ports (Local):
Frontend: http://localhost:5173
Java/Python Backend: http://localhost:8000
Dapr Sidecars: Standard Dapr ports (3500 for HTTP, 50001 for gRPC).
Base API URL: /api/v1

## Authentication (Corporate Standard)
Provider: Azure Entra ID (Corporate Default).
- Development Scopes: * User.Read (Basic profile)
- api://<client_id>/access_as_user (Backend API access)
- openid, profile, offline_access.

Local Dev Bypass (only if enabled):
User: admin@example.com / Pass: admin123.

## Secret Management & Configuration
### Local Development (Dev)
For local development, a local JSON/YAML configuration (e.g. application-dev.yml or .env) is allowed that contains mock or development keys.
These files must never be committed to a version control system (Git).

### Production (Prod)
All production secrets (passwords, API keys, certificates) must be stored in Azure Key Vault.
In the production configuration file (JSON/YAML), only these three identifiers are allowed for bootstrap connections:
AZURE_CLIENT_ID
AZURE_TENANT
AZURE_KEY_VALUT_ID
All other values ​​must be dynamically retrieved by the application from Key Vault upon startup. Applcations have to use Managed Identity for access to KeyVault. If this not possible, can store AZURE_CLIENT_SECRET.

## Frontend Architecture & Auth Implementation (React)
### Authentication (MSAL v3 + Azure Entra ID)
Each application must implement a standard login flow:
- Registration in Azure: * Mandatory setting of accessTokenAcceptedVersion: 2 in the Manifest.
- Definition of scope access_as_user for backend calls.
- Initialization: * Use of MsalProvider to wrap the entire application.
- Strict handling of interaction_in_progress state in login components to avoid Race Conditions.
- Templates: Resolution of protected content using <AuthenticatedTemplate> and <UnauthenticatedTemplate>.

### Communication with API (Axios Interceptors)
Securing data transfer between React and backend (Java/Python):
- Centralized Axios: All calls must go through a single instance in src/axios.ts.
- Automatic Tokens: Interceptor must call acquireTokenSilent with the correct scope before each request and insert it as Bearer token.
- Account Handling: If ActiveAccount is not set, interceptor must fallback to the first account from getAllAccounts().

### UI & State Management
Data Fetching: Mandatory use of React Query (or alternative) with cache invalidation after successful actions (e.g. after file upload).
File Handling: Uploads must include a visual indication of progress (onUploadProgress) and support .pptx and .xlsx formats to defined endpoints /api/import/....

### DevOps & Local Runtime
Definition of development and deployment environment.
#### Docker Compose (Local Dev)
Frontend: Runs on port 5173, uses volumes for hot-reload.
Backend: Python/Java containers connected in one network, communicating via Dapr sidecars.

### Environment Variables (.env)
Every project must have .env.example.
Required variables for Frontend:
VITE_AZURE_CLIENT_ID
VITE_AZURE_TENANT_ID
VITE_API_BASE_URL



**Note to AI Agents**
If a user request conflicts with these standards, prioritize these standards unless explicitly instructed to "ignore standards"