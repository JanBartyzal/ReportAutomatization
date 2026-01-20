from sqlalchemy import Integer, Column, String, Float, Boolean, Index, JSON, DateTime,ForeignKey, LargeBinary
from sqlalchemy.ext.declarative import declarative_base
import datetime
from sqlalchemy.orm import relationship
from sqlalchemy.dialects.postgresql import JSONB

Base = declarative_base()

class Regions(Base):
    __tablename__ = "regions"
    oid = Column(String, primary_key=True)
    region = Column(String)
   

class UploadFile(Base):
    __tablename__ = "upload_files"
    oid = Column(String, primary_key=True)
    filename = Column(String)
    md5hash = Column(String)

class Report(Base):
    __tablename__ = "reports"
    oid = Column(String, primary_key=True)
    region = Column(String)
    upload_file_oid = Column(String)

class SlideData(Base):
    __tablename__ = "slide_data"
    oid = Column(String, primary_key=True)
    report_oid = Column(String)
    slide_index = Column(Integer)
    title = Column(String)
    table_data = Column(JSONB)
    image_data = Column(JSONB)
