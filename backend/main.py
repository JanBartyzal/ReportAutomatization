"""
Main FastAPI application entry point.

This module initializes the FastAPI application with the new layered architecture,
configures CORS middleware, and includes all API routers.
"""

import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.db.models import Base
from app.db.session import engine
from app.routers import admin, imports, opex, reports, vector, analytics

# Initialize logger
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ReportAutomation")

# Create database tables (TODO: Replace with Alembic migrations in production)
Base.metadata.create_all(bind=engine)

# Initialize FastAPI application
app = FastAPI(
    title="Report Automation API",
    description="AI-powered PPTX table extraction and analysis with RAG",
    version="2.0.0"
)

# Configure CORS middleware using centralized settings
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/health")
def health_check() -> dict:
    """
    Health check endpoint.
    
    Returns:
        Status dictionary confirming service is running
    """
    return {"status": "ok", "service": "report-automation", "version": "2.0.0"}


# Include API routers with prefixes and tags
app.include_router(admin.router, prefix="/api/admin", tags=["admin"])
app.include_router(imports.router, prefix="/api/import", tags=["import"])
app.include_router(opex.router, prefix="/api/opex", tags=["opex"])
app.include_router(reports.router, prefix="/api/report", tags=["report"])
app.include_router(vector.router, prefix="/api/vector", tags=["vector"])
app.include_router(analytics.router, prefix="/api/analytics", tags=["analytics"])


logger.info("✅ Report Automation API started with layered architecture")
logger.info(f"   Environment: {settings.api_env}")
logger.info(f"   CORS Origins: {settings.cors_origins}")