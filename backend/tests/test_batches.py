import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.core.database import Base, get_db
from main import app
from app.core.models import Batch, BatchStatus, UploadFile, Document_chunks
from app.schemas.user import User
from app.core.security import get_current_user
import uuid

# Use in-memory SQLite for testing
SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def override_get_db():
    try:
        db = TestingSessionLocal()
        yield db
    finally:
        db.close()

def override_get_current_user():
    return User(
        id="test-user-id",
        name="Test User",
        email="test@example.com",
        roles=["User"]
    )

app.dependency_overrides[get_db] = override_get_db
app.dependency_overrides[get_current_user] = override_get_current_user

client = TestClient(app)

@pytest.fixture(autouse=True)
def setup_db():
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)

def test_create_batch():
    response = client.post("/api/batches/", json={"name": "Test Batch"})
    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "Test Batch"
    assert data["status"] == "OPEN"
    assert "id" in data

def test_list_batches():
    client.post("/api/batches/", json={"name": "Batch 1"})
    client.post("/api/batches/", json={"name": "Batch 2"})
    
    response = client.get("/api/batches/")
    assert response.status_code == 200
    data = response.json()
    assert len(data) >= 2
    assert any(b["name"] == "Batch 1" for b in data)
    assert any(b["name"] == "Batch 2" for b in data)

def test_close_batch():
    create_resp = client.post("/api/batches/", json={"name": "Closing Batch"})
    batch_id = create_resp.json()["id"]
    
    response = client.post(f"/api/batches/{batch_id}/close")
    assert response.status_code == 200
    assert response.json()["status"] == "CLOSED"
    
    # Try to close again
    response = client.post(f"/api/batches/{batch_id}/close")
    assert response.status_code == 400

def test_upload_rejection_for_closed_batch():
    # Create and close batch
    create_resp = client.post("/api/batches/", json={"name": "Closed Batch"})
    batch_id = create_resp.json()["id"]
    client.post(f"/api/batches/{batch_id}/close")
    
    # Mock file upload
    file_content = b"fake pptx content"
    files = {"file": ("test.pptx", file_content, "application/vnd.openxmlformats-officedocument.presentationml.presentation")}
    
    response = client.post(f"/api/import/upload?batch_id={batch_id}", files=files)
    assert response.status_code == 400
    assert "Upload allowed only for OPEN batches" in response.json()["detail"]

def test_delete_batch():
    create_resp = client.post("/api/batches/", json={"name": "To Delete"})
    batch_id = create_resp.json()["id"]
    
    response = client.delete(f"/api/batches/{batch_id}")
    assert response.status_code == 204
    
    response = client.get(f"/api/batches/{batch_id}")
    assert response.status_code == 404
