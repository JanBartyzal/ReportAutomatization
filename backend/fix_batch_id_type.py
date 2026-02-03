import os
import sys
from sqlalchemy import text

# Add current directory to path so we can import app
# Assuming this script is in backend/ root
sys.path.append(os.getcwd())

try:
    from app.core.database import engine
except ImportError as e:
    print(f"ImportError: {e}")
    # Try adding 'app' to path if we are inside backend
    sys.path.append(os.path.join(os.getcwd(), 'app'))
    from app.core.database import engine

def fix_column():
    with engine.connect() as conn:
        print("Checking column type...")
        # Check current type
        result = conn.execute(text("SELECT data_type, udt_name FROM information_schema.columns WHERE table_name = 'upload_files' AND column_name = 'batch_id'"))
        row = result.fetchone()
        if row:
            print(f"Current type: {row[0]}, udt_name: {row[1]}")
            # udt_name for uuid[] is usually _uuid
            if row[1] == '_uuid' or row[0] == 'ARRAY': 
                print("Detected UUID array. Attempting to convert to UUID...")
                try:
                    # We need to cast. If data exists, we take the first element.
                    conn.execute(text("ALTER TABLE upload_files ALTER COLUMN batch_id TYPE UUID USING batch_id[1]"))
                    conn.commit()
                    print("Successfully altered column type to UUID.")
                except Exception as e:
                    print(f"Error altering column: {e}")
            else:
                print(f"Column seems to be correct type or different issue. Type: {row[0]}")
        else:
            print("Column batch_id not found in upload_files.")

if __name__ == "__main__":
    fix_column()
