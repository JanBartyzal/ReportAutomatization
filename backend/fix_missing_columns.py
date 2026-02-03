import os
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from dotenv import load_dotenv

# Load .env file
load_dotenv()

def fix_columns():
    # Use DB_URI if present, otherwise fall back to DATABASE_URL or default
    database_url = os.getenv("DB_URI") or os.getenv("DATABASE_URL")
    if not database_url:
        print("Neither DB_URI nor DATABASE_URL found in .env")
        return

    # Fix for async pg driver if it's there
    if database_url.startswith("postgresql+asyncpg://"):
        database_url = database_url.replace("postgresql+asyncpg://", "postgresql://")
    
    # If running locally, postgres might need to be localhost
    if "postgres" in database_url and "@postgres" in database_url:
        database_url = database_url.replace("@postgres", "@localhost")

    print(f"Connecting to: {database_url.split('@')[-1]}") # Print host/db only for security
    
    engine = create_engine(database_url)
    Session = sessionmaker(bind=engine)
    session = Session()

    print("Checking for missing columns in 'upload_files' and 'reports'...")

    try:
        # Check upload_files
        try:
            session.execute(text("SELECT owner_id FROM upload_files LIMIT 1"))
            print("Column 'owner_id' already exists in 'upload_files'.")
        except Exception:
            session.rollback()
            print("Adding 'owner_id' to 'upload_files'...")
            session.execute(text("ALTER TABLE upload_files ADD COLUMN owner_id VARCHAR"))
            print("Successfully added 'owner_id' to 'upload_files'.")

        # Check reports
        try:
            session.execute(text("SELECT owner_id FROM reports LIMIT 1"))
            print("Column 'owner_id' already exists in 'reports'.")
        except Exception:
            session.rollback()
            print("Adding 'owner_id' to 'reports'...")
            session.execute(text("ALTER TABLE reports ADD COLUMN owner_id VARCHAR"))
            print("Successfully added 'owner_id' to 'reports'.")

        # Check batches
        try:
            session.execute(text("SELECT owner_id FROM batches LIMIT 1"))
            print("Column 'owner_id' already exists in 'batches'.")
        except Exception:
            session.rollback()
            print("Adding 'owner_id' to 'batches'...")
            session.execute(text("ALTER TABLE batches ADD COLUMN owner_id VARCHAR"))
            print("Successfully added 'owner_id' to 'batches'.")

        # Optional: Backfill owner_id if needed
        # session.execute(text("UPDATE upload_files SET owner_id = 'system' WHERE owner_id IS NULL"))
        # session.execute(text("UPDATE reports SET owner_id = 'system' WHERE owner_id IS NULL"))

        session.commit()
        print("Migration completed successfully!")

    except Exception as e:
        session.rollback()
        print(f"Migration failed: {e}")
    finally:
        session.close()

if __name__ == "__main__":
    fix_columns()
