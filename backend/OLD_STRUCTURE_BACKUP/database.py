"""
Database connection and session management.

This module provides SQLAlchemy engine and session factory for the application.
All configuration is loaded from environment variables via Pydantic Settings.
"""

from typing import Generator
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from core.config import settings

# Create engine for PostgreSQL using settings
engine = create_engine(str(settings.database_url))

# Session factory for database connections
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def get_db() -> Generator[Session, None, None]:
    """
    Dependency injection for database sessions.
    
    Provides a SQLAlchemy session that is automatically closed after use.
    Use with FastAPI Depends() to get a database session in route handlers.
    
    Yields:
        Session: SQLAlchemy database session
        
    Example:
        @app.get("/items")
        def get_items(db: Session = Depends(get_db)):
            return db.query(Item).all()
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def init_db() -> None:
    """
    Initialize database tables.
    
    Creates all tables defined in dbmodels.Base.
    Note: In production, use Alembic migrations instead of this function.
    """
    from dbmodels import Base
    Base.metadata.create_all(bind=engine)