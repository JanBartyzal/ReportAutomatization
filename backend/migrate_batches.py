import os
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
import uuid
import datetime
from dotenv import load_dotenv

# Load .env file
load_dotenv()

def migrate():
    database_url = os.getenv("DATABASE_URL")
    if not database_url:
        print("DATABASE_URL not found in .env")
        return

    # Fix for async pg driver if it's there
    if database_url.startswith("postgresql+asyncpg://"):
        database_url = database_url.replace("postgresql+asyncpg://", "postgresql://")

    engine = create_engine(database_url)
    Session = sessionmaker(bind=engine)
    session = Session()

    print("Starting migration for Batch Processing...")

    try:
        # 1. Create batches table
        session.execute(text("""
            CREATE TABLE IF NOT EXISTS batches (
                id UUID PRIMARY KEY,
                name VARCHAR NOT NULL,
                status VARCHAR NOT NULL,
                id VARCHAR,
                created_at TIMESTAMP WITHOUT TIME ZONE
            )
        """))
        print("Created 'batches' table.")

        # 2. Add batch_id to upload_files
        try:
            session.execute(text("ALTER TABLE upload_files ADD COLUMN batch_id UUID REFERENCES batches(id)"))
            print("Added 'batch_id' to 'upload_files'.")
        except Exception as e:
            print(f"Column batch_id might already exist in upload_files: {e}")

        # 3. Add batch_id to document_chunks
        try:
            session.execute(text("ALTER TABLE document_chunks ADD COLUMN batch_id UUID REFERENCES batches(id)"))
            session.execute(text("CREATE INDEX IF NOT EXISTS idx_document_chunks_batch_id ON document_chunks(batch_id)"))
            print("Added 'batch_id' to 'document_chunks'.")
        except Exception as e:
            print(f"Column batch_id might already exist in document_chunks: {e}")

        # 4. Create Legacy Batch and assign existing data
        legacy_batch_id = uuid.uuid4()
        now = datetime.datetime.utcnow()
        
        # Check if any data exists that needs a batch
        existing_files = session.execute(text("SELECT id FROM upload_files WHERE batch_id IS NULL")).fetchall()
        existing_chunks = session.execute(text("SELECT id FROM document_chunks WHERE batch_id IS NULL")).fetchall()

        if existing_files or existing_chunks:
            print("Found existing legacy data. Creating 'Legacy Batch'...")
            session.execute(text("""
                INSERT INTO batches (id, name, status, id, created_at)
                VALUES (:id, :name, :status, :id, :created_at)
            """), {
                "id": legacy_batch_id,
                "name": "Legacy Batch",
                "status": "CLOSED",
                "id": "system",
                "created_at": now
            })

            if existing_files:
                session.execute(text("UPDATE upload_files SET batch_id = :batch_id WHERE batch_id IS NULL"), 
                                {"batch_id": legacy_batch_id})
                print(f"Assigned {len(existing_files)} files to Legacy Batch.")

            if existing_chunks:
                session.execute(text("UPDATE document_chunks SET batch_id = :batch_id WHERE batch_id IS NULL"), 
                                {"batch_id": legacy_batch_id})
                print(f"Assigned {len(existing_chunks)} chunks to Legacy Batch.")
        else:
            print("No legacy data found.")

        session.commit()
        print("Migration completed successfully!")

    except Exception as e:
        session.rollback()
        print(f"Migration failed: {e}")
    finally:
        session.close()

if __name__ == "__main__":
    migrate()
