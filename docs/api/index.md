# Report Automation API Documentation

Welcome to the Report Automation API documentation. This API provides AI-powered PPTX table extraction, analysis, and RAG (Retrieval-Augmented Generation) capabilities.

## Base URL

All API endpoints are prefixed with:
```
/api
```

## Authentication

Most endpoints require authentication. The system uses a User object (usually containing `id` and `email`) provided via dependency injection.

- **Secured Endpoints**: Require a valid session or token.
- **Admin Endpoints**: Require admin privileges.
- **Row Level Security (RLS)**: Data is strictly filtered by the user's `id`. Users can only access and manipulate their own data.

## API Modules

The API is organized into several modules:

- [Health Check](health_api.md): Service status monitoring.
- [Admin](admin_api.md): System-wide statistics and management.
- [Imports](imports_api.md): File upload (PPTX, OPEX) and listing.
- [OPEX](opex_api.md): PowerPoint processing and slide data retrieval.
- [Analytics](analytics_api.md): Schema detection and cross-file data aggregation.
- [Reports](reports_api.md): Report generation and retrieval.
- [Vector & RAG](vector_api.md): Document indexing and AI-powered semantic search.
