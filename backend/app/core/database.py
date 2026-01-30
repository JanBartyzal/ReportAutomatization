from sqlalchemy.orm import sessionmaker, Session, Query
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import event, MetaData
from core.context import organization_context
from core.config import Get_Key
import logging


# Unified Declarative Base
Base = declarative_base()

# Load URL from env
DATABASE_URL = Get_Key(
    "DB_URI", 
    "postgresql://user:password@db:5432/demo"
)

# Engine creation
from sqlalchemy import create_engine
engine = create_engine(DATABASE_URL)

# Custom Query class to auto-filter by organization_id
class OrganizationAwareQuery(Query):
    def get(self, ident):
        # Override get to ensure filtering
        obj = super().get(ident)
        if obj and hasattr(obj, 'organization_id'):
            org_id = organization_context.get()
            if org_id and obj.organization_id != org_id:
                return None
        return obj

    def __iter__(self):
        return super(self._filter_by_organization(), OrganizationAwareQuery).__iter__()

    def from_self(self, *entities):
        return super(self._filter_by_organization(), OrganizationAwareQuery).from_self(*entities)

    def count(self):
        return super(self._filter_by_organization(), OrganizationAwareQuery).count()

    def _filter_by_organization(self):
        org_id = organization_context.get()
        if org_id:
            mzero = self._mapper_zero()
            if mzero and hasattr(mzero.class_, 'organization_id'):
                return self.filter(mzero.class_.organization_id == org_id)
        return self

# Session factory
SessionLocal = sessionmaker(
    autocommit=False, 
    autoflush=False, 
    bind=engine,
    query_cls=OrganizationAwareQuery
)

def get_db():
    db = SessionLocal()
    try:
        # Debug logging to verify context
        # logger.debug(f"DB Session created. Context Org ID: {organization_context.get()}")
        yield db
    finally:
        db.close()

def init_db():
    Base.metadata.create_all(bind=engine)
