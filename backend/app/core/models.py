"""
SQLAlchemy database models for PPTX-AI-Analyzer.

This module defines the database schema including user regions, file uploads,
reports, slide data, and vector embeddings for RAG functionality.
"""

from sqlalchemy import Integer, Column, String, Float, Boolean, Index, JSON, DateTime, ForeignKey, LargeBinary, Enum as SQLEnum
import enum
from sqlalchemy.orm import relationship, declarative_base
import datetime
import uuid
from sqlalchemy.dialects.postgresql import JSONB, UUID
from pgvector.sqlalchemy import Vector

Base = declarative_base()

class SubscriptionTier(str, enum.Enum):
    FREE = "FREE"
    PRO = "PRO"
    ENTERPRISE = "ENTERPRISE"
    

class SystemConfig(Base):
    """
    Key-Value storage for dynamic system configuration.
    """
    __tablename__ = "system_config"
    key = Column(String, primary_key=True)
    value = Column(String)

class Users(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True)
    email = Column(String, unique=True, index=True)
    password = Column(String)
    user_sid = Column(String)
    
    # Renamed from tenant_id
    organization_id = Column(Integer, ForeignKey("organizations.id"))
    organization = relationship("Organization", back_populates="users")
    
    # RBAC: Role for access control (admin, editor, viewer)
    role = Column(String, default="viewer", index=True)
    
    projects = relationship("Projects", back_populates="users")

    created_at = Column(DateTime, default=datetime.datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)

class Projects(Base):
    __tablename__ = "projects"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"))
    organization = relationship("Organization", back_populates="projects")
    
    # Missing explicit user link if Users.projects has back_populates="projects"
    # But Users defines `projects = relationship("Projects", back_populates="users")`
    # So Projects needs `users`. 
    # NOTE: If Projects are 1:N with Users, it needs user_id. 
    user_id = Column(Integer, ForeignKey("users.id"))
    users = relationship("Users", back_populates="projects")

    created_at = Column(DateTime, default=datetime.datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)



class Organization(Base):
    """
    SaaS Tenant / Organization Entity.
    Renamed from Tenants to avid confusion with Azure Tenants.
    """
    __tablename__ = "organizations"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True)
    
    # Optional Azure connection details (can be moved to separate table if 1:N needed)
    azure_tenant_id = Column(String, unique=True, index=True, nullable=True)
    subscription_id = Column(String, unique=True, index=True, nullable=True)
    subscription_name = Column(String, unique=True, index=True, nullable=True)
    
    created_at = Column(DateTime, default=datetime.datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)
    
    # SaaS Subscription
    subscription_tier = Column(SQLEnum(SubscriptionTier), default=SubscriptionTier.FREE)
    max_projects = Column(Integer, default=1)
    max_scans_per_month = Column(Integer, default=10)

    users = relationship("Users", back_populates="organization")
    projects = relationship("Projects", back_populates="organization")
    usage = relationship("OrganizationUsage", back_populates="organization", uselist=False)



class OrganizationUsage(Base):
    __tablename__ = "organization_usage"
    
    id = Column(Integer, primary_key=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"), unique=True)
    organization = relationship("Organization", back_populates="usage")
    
    scans_this_month = Column(Integer, default=0)
    last_reset_date = Column(DateTime, default=datetime.datetime.utcnow)


class Regions(Base):
    """
    User region assignments for data segmentation.
    
    Relationships:
        - upload_files: One-to-many with UploadFile
    """
    __tablename__ = "regions"
    id = Column(Integer, primary_key=True)
    id = Column(String)  # Azure AD Object ID
    region = Column(String)
    upload_files = relationship("UploadFile", back_populates="region")


class BatchStatus(str, enum.Enum):
    """Status of a batch processing workflow."""
    OPEN = "OPEN"
    PROCESSING = "PROCESSING"
    CLOSED = "CLOSED"


class Batch(Base):
    """
    Groups uploads into logical batches for versioning and scoped queries.
    
    Relationships:
        - upload_files: One-to-many with UploadFile
        - document_chunks: One-to-many with Document_chunks
    """
    __tablename__ = "batches"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String, nullable=False)
    status = Column(SQLEnum(BatchStatus), default=BatchStatus.OPEN, nullable=False)
    id = Column(String, index=True)  # Owner's Azure AD Object ID (RLS)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

    upload_files = relationship("UploadFile", back_populates="batch")
    document_chunks = relationship("Document_chunks", back_populates="batch")


class UploadFile(Base):
    """
    Metadata for uploaded PPTX and Excel files.
    
    Implements MD5-based deduplication and row-level security (id filtering).
    
    Relationships:
        - region: Many-to-one with Regions
        - reports: One-to-many with Report
    """
    __tablename__ = "upload_files"
    id = Column(Integer, primary_key=True)
    id = Column(String)  # Owner's Azure AD Object ID (RLS)
    filename = Column(String)  # Sanitized filename with MD5 suffix
    md5hash = Column(String)  # For deduplication checks
    region_id = Column(Integer, ForeignKey("regions.id"))
    batch_id = Column(UUID(as_uuid=True), ForeignKey("batches.id"), nullable=True)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

    region = relationship("Regions", back_populates="upload_files")
    reports = relationship("Report", back_populates="upload_file")
    batch = relationship("Batch", back_populates="upload_files")


class Report(Base):
    """
    Processing report for an uploaded file.
    
    Relationships:
        - upload_file: Many-to-one with UploadFile
        - slide_data: One-to-many with SlideData
    """
    __tablename__ = "reports"
    id = Column(Integer, primary_key=True)
    id = Column(String)  # Owner's Azure AD Object ID (RLS)
    region = Column(String)
    upload_file_id = Column(Integer, ForeignKey("upload_files.id"))
    upload_file = relationship("UploadFile", back_populates="reports")
    slide_data = relationship("SlideData", back_populates="report")
    appendix = Column(JSONB, default={})  # Excel data appendix


class SlideData(Base):
    """
    Extracted data from individual presentation slides.
    
    Stores structured JSON data including tables, images, and text content.
    
    Relationships:
        - report: Many-to-one with Report
    """
    __tablename__ = "slide_data"
    id = Column(Integer, primary_key=True)
    report_id = Column(Integer, ForeignKey("reports.id"))
    report = relationship("Report", back_populates="slide_data")
    slide_index = Column(Integer)
    title = Column(String)
    table_data = Column(JSONB)  # Extracted table rows
    image_data = Column(JSONB)  # Image metadata and base64
    text_content = Column(JSONB)  # Text elements


class Document_chunks(Base):
    """
    Vector embeddings for RAG (Retrieval Augmented Generation).
    
    Stores markdown-formatted slide content with pgvector embeddings
    for semantic search.
    """
    __tablename__ = "document_chunks"
    id = Column(Integer, primary_key=True)
    content = Column(String)  # Markdown-formatted table/text
    mdata = Column(JSONB)  # Metadata: {"report_id": x, "slide_index": y}
    batch_id = Column(UUID(as_uuid=True), ForeignKey("batches.id"), nullable=True, index=True)
    embedding = Column(Vector(768))  # Vector embedding for similarity search

    batch = relationship("Batch", back_populates="document_chunks")
