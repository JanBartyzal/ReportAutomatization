import os
import sys
import traceback

# Add current directory to path
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.append(current_dir)

try:
    from app.core.database import engine
    from sqlalchemy import text

    def fix_column():
        with engine.connect() as conn:
            print("Connected to database.")
            print("Checking column type for 'batch_id' in 'upload_files'...")
            
            # Check current type
            result = conn.execute(text("SELECT data_type, udt_name FROM information_schema.columns WHERE table_name = 'upload_files' AND column_name = 'batch_id'"))
            row = result.fetchone()
            
            if row:
                data_type, udt_name = row
                print(f"Current type: {data_type}, udt_name: {udt_name}")
                
                # udt_name for uuid[] is usually _uuid
                if udt_name == '_uuid' or data_type == 'ARRAY': 
                    print("Detected UUID array. Attempting to convert to UUID...")
                    try:
                        # We need to cast. If data exists, we take the first element.
                        conn.execute(text("ALTER TABLE upload_files ALTER COLUMN batch_id TYPE UUID USING batch_id[1]"))
                        conn.commit()
                        print("Successfully altered column type to UUID.")
                    except Exception as e:
                        print(f"Error altering column: {e}")
                        conn.rollback() # Important to rollback on error
                else:
                    print(f"Column seems to be correct type or different issue. Type: {data_type}, UDT: {udt_name}")
            else:
                print("Column batch_id not found in upload_files.")

    if __name__ == "__main__":
        fix_column()

except Exception:
    traceback.print_exc()
