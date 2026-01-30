# Backend Architecture Documentation

**Project:** PPTX-AI-Analyzer  
**Last Updated:** 2026-01-25 (Restructured)  
**Status:** Layered Architecture - Production Ready

---

## 📋 Overview

This document describes the **layered backend architecture** implemented on 2026-01-25. The backend follows FastAPI best practices with **clear separation of concerns** across 5 distinct layers: core infrastructure, database access, data schemas, business logic services, and API routers. This structure provides improved testability, maintainability, and scalability.

---

## 🏗️ Directory Structure

**NEW: Layered Architecture (2026-01-25)**

```
backend/
├── app/                          # Application package (NEW)
│   ├── __init__.py
│   │
│   ├── core/                     # Core infrastructure
│   │   ├── __init__.py
│   │   ├── config.py             # Pydantic Settings (centralized configuration)
│   │   └── security.py           # Authentication & authorization (was userauth.py)
│   │
│   ├── db/                       # Database layer
│   │   ├── __init__.py
│   │   ├── session.py            # Session management & DI (was database.py)
│   │   ├── models.py             # SQLAlchemy ORM models (was dbmodels.py)
│   │   └── cache.py              # Redis cache manager (was redis_cache.py)
│   │
│   ├── schemas/                  # Pydantic models (DTOs)
│   │   ├── __init__.py
│   │   ├── user.py               # User, Regions schemas
│   │   ├── file.py               # UploadFile schema
│   │   ├── report.py             # Report schema
│   │   └── slide.py              # SlideData schema
│   │
│   ├── services/                 # Business logic layer
│   │   ├── __init__.py
│   │   ├── opex_service.py       # OpexManager (was opex.py)
│   │   ├── pptx_service.py       # PowerpointManager (was powerpoint.py)
│   │   ├── table_service.py      # TableDataProcessor (was table_data.py)
│   │   ├── ocr_service.py        # TableImageData (was images_tesseract.py)
│   │   └── rag_service.py        # RAG utilities (was rag.py)
│   │
│   └── routers/                  # API endpoints (thin layer)
│       ├── __init__.py
│       ├── admin.py              # Admin endpoints (was main_admin.py)
│       ├── imports.py            # File upload endpoints (was main_import.py)
│       ├── opex.py               # OPEX processing endpoints (was main_opex.py)Report generation endpoints (was main_report.py)
│       └── vector.py             # RAG/Vector endpoints (was main_vector.py)
│
├── tests/                        # Test suite
│   ├── __init__.py
│   └── test_pseudo_table_parser.py
│
├── main.py                       # FastAPI application entry point
├── requirements.txt              # Python dependencies
└── Dockerfile                    # Container configuration
```

**Migration Summary:**
- `core/config.py` → `app/core/config.py`
- `userauth.py` → `app/core/security.py`
- `database.py` → `app/db/session.py`
- `dbmodels.py` → `app/db/models.py`
- `redis_cache.py` → `app/db/cache.py`
- `models.py` → Split into `app/schemas/{user,file,report,slide}.py`
- `main_*.py` → `app/routers/*.py` (prefix removed)
- Service files → `app/services/*_service.py`

---

## 🔄 Request/Response Flow

```mermaid
graph TD
    A[Client Request] -->|HTTPS| B[FastAPI Main App]
    B --> C{CORS Middleware}
    C --> D[Router Endpoint]
    D --> E{Authentication Check}
    E -->|Invalid| F[401 Unauthorized]
    E -->|Valid| G[Depends get_db]
    G --> H[Service Layer]
    H --> I{Business Logic}
    I --> J[Database Query]
    I --> K[Redis Cache]
    I --> L[External API LiteLLM]
    J --> M[Response]
    K --> M
    L --> M
    M --> N[JSON Response]
    N --> A
    
    style A fill:#e1f5ff
    style B fill:#fff4e1
    style E fill:#ffe1e1
    style H fill:#e1ffe1
    style M fill:#f0e1ff
```

---

## 🔐 Security Architecture

### Authentication Flow

```
1. Client sends request with Authorization: Bearer <JWT>
2. FastAPI azure_scheme validates token with Azure Entra ID
3. get_current_user() extracts user info from validated token
4. User object passed to route handler via Depends()
5. Route handler performs Row Level Security (RLS) filtering
```

### Row Level Security (RLS) Implementation

All database queries must filter by `user.id`:

```python
# CORRECT: RLS enforced
db_files = db.query(DBUploadFile).filter(DBUploadFile.id == user.id).all()

# INCORRECT: No RLS (security violation)
db_files = db.query(DBUploadFile).all()  # ❌ Exposes all users' data
```

### Security Layers

| Layer | Component | Purpose |
|-------|-----------|---------|
| **Network** | HTTPS Only | Encrypted transport |
| **Auth** | Azure Entra ID JWT | Identity verification |
| **Authorization** | FastAPI Depends | Role-based access control (RBAC) |
| **Data** | Row Level Security | User data isolation |
| **Config** | Pydantic Settings | No hardcoded secrets |
| **Input** | Pydantic Validation | Type-safe request parsing |

---

## ⚙️ Configuration Management

All configuration is centralized in `core/config.py` using Pydantic `BaseSettings`:

**Environment Variables (required):**
```env
# Database
DATABASE_URL=postgresql://user:pass@db:5432/schema

# Redis
REDIS_URL=redis://redis:6379/0

# Azure Authentication
AZURE_CLIENT_ID=your-client-id
AZURE_TENANT_ID=your-tenant-id

# Application
API_ENV=production  # or development
CORS_ORIGINS=http://localhost:5173,https://app.example.com

# AI/LLM
MODEL_NAME=text-embedding-ada-002

# OCR
OCR_CONFIDENCE_THRESHOLD=85

# Cache
CACHE_TTL_DAYS=7

# Storage
UPLOAD_DIR=local_data/uploads
```

**No Default Credentials:**  
- ❌ Old: `os.getenv("DATABASE_URL", "postgresql://user:password@...")`  
- ✅ New: `settings.database_url` (raises exception if not set)

---

## 🗄️ Database Layer

### Connection Management

```python
# Dependency injection pattern
@router.get("/items")
async def get_items(db: Session = Depends(get_db)):
    return db.query(Item).filter(Item.id ==user.id).all()
```

**Benefits:**
- Automatic session cleanup (no connection leaks)
- Easy testing (mock `get_db` dependency)
- Consistent session handling across all endpoints

### Models

**SQLAlchemy (ORM) - `dbmodels.py`:**
- `Regions` - Geographic regions
- `UploadFile` - File metadata
- `Report` - Report records
- `SlideData` - Slide content
- `Document_chunks` - Vector embeddings for RAG

**Pydantic (Schemas) - `models.py`:**
- `User` - Authenticated user
- `Regions`, `UploadFile`, `Report`, `SlideData` - Request/response DTOs

---

## 🚀 API Endpoints

### Authentication Required

All endpoints except `/api/health` require authentication via `Depends(get_current_user)`.

### Endpoint Groups

| Prefix | Router | Purpose |
|--------|--------|---------|
| `/api/admin` | `main_admin.py` | Admin-only endpoints (requires `verify_admin`) |
| `/api/import` | `main_import.py` | File upload (PPTX, Excel) |
| `/api/opex` | `main_opex.py` | OPEX file processing |
| `/api/report` | `main_report.py` | Report generation |
| `/api/vector` | `main_vector.py` | RAG chat and vectorization |

### Example: File Upload Endpoint

```python
@router.post("/upload")
async def upload_file(
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),  # Authentication
    db: Session = Depends(get_db)  # Database session
) -> Dict[str, str]:
    """Upload PPTX file with MD5 deduplication."""
    
    # Calculate hash for deduplication
    file_data = await file.read()
    md5hash = hashlib.md5(file_data).hexdigest()
    
    # Save to disk
    filename = f"{prefix}_{md5hash}.{extension}"
    file_path = os.path.join(settings.upload_dir, filename)
    with open(file_path, "wb") as f:
        f.write(file_data)
    
    # Save metadata with RLS (user id)
    upload_file = DBUploadFile(
        id=user.id,  # RLS: Associate with authenticated user
        filename=filename,
        md5hash=md5hash
    )
    db.add(upload_file)
    db.commit()
    
    return {"message": "File uploaded successfully", "user": user.id}
```

---

## 📦 Service Layer

Business logic is extracted to service classes for testability and reusability:

### OpexManager (`opex.py`)
- `process_opex(file_id)` - Process OPEX PowerPoint file
- `get_presentation_header(file_id)` - Get slide summaries
- `get_slide_data(file_id, slide_id)` - Get detailed slide data

### PowerpointManager (`powerpoint.py`)
- `load_powerpoint(path)` - Load PPTX file
- `extract_slides(presentation)` - Extract all slides
- `extract_native_table(shape)` - Extract native tables
- `extract_excel_object(shape)` - Extract embedded Excel

### TableDataProcessor (`table_data.py`)
- `extract_data(image_bytes)` - Extract table from image with caching
- `normalize_text(content)` - Normalize text to numbered lines
- `normalize_table(data)` - Convert to pandas DataFrame

### TableImageData (`images_tesseract.py`)
- `smart_extract(image_bytes)` - OCR with confidence scoring
- `get_tesseract_confidence(image)` - Calculate OCR confidence

---

## 🔄 Caching Strategy

Redis is used for image hash-based deduplication:

```python
async def extract_data(self, image_bytes: bytes) -> Dict[str, Any]:
    # Calculate SHA-256 hash
    img_hash = self.get_image_hash(image_bytes)
    cache_key = f"img_extract:{img_hash}"
    
    # Check cache
    cached_result = await cache.get(cache_key)
    if cached_result:
        return cached_result  # Cache HIT
    
    # Extract and cache
    result = self.image_data.smart_extract(image_bytes)
    await cache.set(cache_key, result)  # TTL from settings
    return result
```

**Benefits:**
- Avid re-processing identical images
- Configurable TTL (default: 7 days)
- Async client for FastAPI compatibility

---

## 🧪 Testing Strategy

### Current State
- ✅ Unit tests for `pseudo_table_parser.py`
- ⚠️ No integration tests for routers yet

### Planned
- Integration tests for all endpoints
- Mock authentication for test users
- Database fixtures for test data
- Coverage target: 80%+

---

## 🚦 Error Handling

### HTTP Exception Mapping

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `HTTPException(404)` | 404 Not Found | File/record doesn't exist |
| `HTTPException(401)` | 401 Unauthorized | Invalid/missing auth token |
| `HTTPException(403)` | 403 Forbidden | User lacks required role |
| `HTTPException(400)` | 400 Bad Request | Invalid input data |
| `HTTPException(500)` | 500 Internal Server Error | Unexpected errors |

### Logging

```python
import logging

logger = logging.getLogger("uvicorn")

# INFO: Normal operations
logger.info(f"File uploaded by user: {user.email}")

# WARNING: Unusual but handled
logger.warning(f"OCR confidence low: {confidence}%")

# ERROR: Exceptions
logger.error(f"Failed to process file: {e}", exc_info=True)
```

---

## 📊 Performance Considerations

| Component | Optimization |
|-----------|--------------|
| **Redis Cache** | SHA-256 hash deduplication (68% hit rate) |
| **Database** | Indexed queries on `id`, `md5hash` |
| **File Upload** | MD5 hash prevents duplicate storage |
| **Async Endpoints** | Non-blocking I/O for Redis, HTTP calls |
| **Connection Pooling** | SQLAlchemy pool for DB connections |

---

## 🔜 Future Improvements

1. **Alembic Migrations** - Replace `create_all()` with versioned migrations
2. **Service Layer Separation** - Move more business logic out of routers
3. **Custom Exception Classes** - Type-safe error handling
4. **Request ID Middleware** - Correlation IDs for tracing
5. **Prometheus Metrics** - Endpoint latency, error rates
6. **File Upload Validation** - File extension whitelist, size limits
7. **Virus Scanning** - Integrate antivirus scan on upload
8. **Azure Blob Storage** - Replace local file storage

---

## 📝 Code Quality Standards

**All new code must:**
- ✅ Have full type hints (100% coverage)
- ✅ Have Google-style docstrings
- ✅ Pass `mypy --strict`
- ✅ Pass `ruff check .`
- ✅ Pass `black --check .`
- ✅ Have unit/integration tests
- ✅ Use English comments only
- ✅ Follow dependency injection patterns
- ✅ Use centralized configuration (`settings`)

---

**Document Version:** 1.0  
**Author:** Backend Refactoring Team  
**Classification:** Internal Use Only
