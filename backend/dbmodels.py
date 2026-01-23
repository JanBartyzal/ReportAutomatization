from sqlalchemy import Integer, Column, String, Float, Boolean, Index, JSON, DateTime,ForeignKey, LargeBinary
from sqlalchemy.ext.declarative import declarative_base
import datetime
from sqlalchemy.orm import relationship
from sqlalchemy.dialects.postgresql import JSONB
from pgvector.sqlalchemy import Vector

Base = declarative_base()

class Regions(Base):
    __tablename__ = "regions"
    id = Column(Integer, primary_key=True)
    oid = Column(String)  
    region = Column(String)
    upload_files = relationship("UploadFile", back_populates="region")
   

class UploadFile(Base):
    __tablename__ = "upload_files"
    id = Column(Integer, primary_key=True)
    oid = Column(String)  
    filename = Column(String)
    md5hash = Column(String)
    region_id = Column(Integer, ForeignKey("regions.id"))
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

    region = relationship("Regions", back_populates="upload_files")    
    reports = relationship("Report", back_populates="upload_file")

class Report(Base):
    __tablename__ = "reports"
    id = Column(Integer, primary_key=True)
    oid = Column(String)  
    region = Column(String)
    upload_file_id = Column(Integer, ForeignKey("upload_files.id"))
    upload_file = relationship("UploadFile", back_populates="reports")
    slide_data = relationship("SlideData", back_populates="report")
    
class SlideData(Base):
    __tablename__ = "slide_data"
    id = Column(Integer, primary_key=True)
    report_id = Column(Integer, ForeignKey("reports.id"))
    report = relationship("Report", back_populates="slide_data")
    slide_index = Column(Integer)
    title = Column(String)
    table_data = Column(JSONB)
    image_data = Column(JSONB)
    text_content = Column(JSONB)


class Document_chunks(Base):
    __tablename__ = "document_chunks"
    id = Column(Integer, primary_key=True)
    content = Column(String)
    mdata = Column(JSONB)
    embedding = Column(Vector(768))
