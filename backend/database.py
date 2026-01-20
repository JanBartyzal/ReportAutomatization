import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# Načtení URL z proměnné prostředí (nastaveno v Docker Compose) 
DATABASE_URL = os.getenv(
    "DATABASE_URL", 
    "postgresql://user:password@db:5432/schema"
)

# Vytvoření engine pro PostgreSQL 
engine = create_engine(DATABASE_URL)

# Továrna na databázová sezení
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def init_db():
    Base.metadata.create_all(bind=engine)