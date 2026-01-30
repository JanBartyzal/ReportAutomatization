# Project Default Configuration

**Context:** These are the default values and configuration assumptions for the local development environment.

**Usage:** AI Agents should assume these values unless specified otherwise in the task description.

## 1. Environment \& Infrastructure
- **Environment:** `Local Development`
- **Containerization:** Docker Compose is the default for running dependencies (DB, Redis).
- **Timezone:** UTC (internally), Browser Local (presentation).
- **Date Format:** ISO 8601 (`YYYY-MM-DDTHH:mm:ssZ`) for API/Backend communication.

## 2. Network \& Ports (Standardized)
- **Frontend (React):** `http://localhost:5173`
- **Backend (Python/FastAPI/Flask):** `http://localhost:8000`
- **Backend (.NET/C#):** `http://localhost:5000` (http) / `5001` (https)
- **Database (Postgres/SQL Server):** Exposed on standard ports (`5432` / `1433`) to localhost.

## 3. Authentication (Local Dev Only)
- **Superadmin:**
  - User: `admin@example.com`
  - Pass: `admin123` (or `Pass123!`)
- **Test User:**
  - User: `user@example.com`
  - Pass: `user123`
- **OAuth Scope:** `api://<client_id>/user_impersonation` (for Backend API access)

## 4. Path \& Structure Conventions
- **Secrets/Env:** `.env` file (never committed). Use `.env.example` as a template.
- **Assets:** `/public/assets` or `/static`
- **Logs:** `/logs` or stdout (docker).

## 5. Project Specific Defaults
- **Base API URL:** {{ INSERT: e.g., /api/v1 }}
- **Upload limit:** {{ INSERT: e.g., 10MB }}
- **Default Locale:** `en` (fallback), `cs` (primary if Czech project)

