try:
    from app.core.models import Batch, BatchStatus
    print("Batch model imported successfully")
except Exception as e:
    print(f"Failed to import Batch model: {e}")

try:
    from app.routers import batches
    print("Batches router imported successfully")
except Exception as e:
    print(f"Failed to import Batches router: {e}")
