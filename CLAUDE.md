**Project Context** documentation files:
   - `docs/dod_criteria.md` - Definition of Done criteria
   - `docs/STANDARDS.md` - Project coding standards
   - `docs/project_charter.md` - Project charter v4.0
   - `docs/roadmap.md` - Implementation roadmap

## Tech Stack
- Core Microservices - Java 21 - Spring Boot 3.x.
- Data Conversion & AI - Python - FastAPI for REST/gRPC endpoints.
- Frontend - React 18+ (Vite) - TypeScript
- AI Integration: All LLM calls MUST go through the LiteLLM gateway (OpenAI-compatible).
- Frontend - React 18+ (Vite)
- Jazyk: TypeScript
- State Management: TanStack Query (React Query) – pro server state.
- Styling: Tailwind CSS + Fluent UI (Microsoft Design Language).
- Routing: React Router DOM.
- HTTP Client: Axios (s interceptory).
- Communication: Always use REST API for frontend-to-backend calls.
- HTTP Client: Axios (s interceptory).
- Service Mesh: Dapr (Distributed Application Runtime) is mandatory for all service-to-service communication.
- Maven: `i:\apache-maven-3.9.6\bin\mvn.cmd clean compile`


## Rules
- before starting any task, read all the documentation files and understand the project context
- prepare a plan for the task and share it with me for review
- if you have any question, ask me
- if I approve the plan, proceed with the implementation in automated mode (minimize user interaction)
- if you need to change the plan, share it with me for review
- you have approval for create or edit any file
- you have approval for bash commands ls, find, dir, for,curl,curl -X POST,curl -X GET etc... for search and list files and in find in files

## MISC
 - RA = ReportAutomatization = this project
 





