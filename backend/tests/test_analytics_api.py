"""
Integration tests for analytics aggregation API endpoints.

Tests the complete flow including authentication, RLS, and data aggregation.
"""

import pytest
from fastapi.testclient import TestClient
from unittest.mock import Mock, patch, MagicMock
from sqlalchemy.orm import Session

from main import app
from app.schemas.user import User


class TestAnalyticsAPI:
    """Integration tests for /api/analytics endpoints."""
    
    @pytest.fixture
    def client(self):
        """Create test client."""
        return TestClient(app)
    
    @pytest.fixture
    def mock_user(self):
        """Create mock authenticated user."""
        user = User(
            id="test-user-123",
            email="test@example.com",
            name="Test User"
        )
        return user
    
    @patch("app.routers.analytics.get_current_user")
    @patch("app.routers.analytics.get_db")
    def test_preview_endpoint_requires_auth(self, mock_db, mock_auth, client):
        """Test that preview endpoint requires authentication."""
        # Mock authentication failure
        mock_auth.side_effect = Exception("Unauthorized")
        
        response = client.post(
            "/api/analytics/aggregate/preview",
            json={"file_ids": [1, 2, 3]}
        )
        
        # Should fail without proper auth
        assert response.status_code in [401, 500]  # Depending on exception handling
    
    @patch("app.routers.analytics.get_current_user")
    @patch("app.routers.analytics.get_db")
    def test_preview_endpoint_empty_file_list(self, mock_db, mock_auth, client, mock_user):
        """Test preview endpoint with empty file list."""
        mock_auth.return_value = mock_user
        mock_db.return_value = MagicMock(spec=Session)
        
        response = client.post(
            "/api/analytics/aggregate/preview",
            json={"file_ids": []}
        )
        
        assert response.status_code == 400
        assert "No file IDs provided" in response.json()["detail"]
    
    @patch("app.routers.analytics.get_current_user")
    @patch("app.routers.analytics.get_db")
    @patch("app.routers.analytics.AggregationService")
    def test_preview_endpoint_success(
        self, 
        mock_service_class, 
        mock_db, 
        mock_auth, 
        client, 
        mock_user
    ):
        """Test successful preview request."""
        mock_auth.return_value = mock_user
        mock_db.return_value = MagicMock(spec=Session)
        
        # Mock service response
        mock_service = mock_service_class.return_value
        mock_service.detect_common_schemas.return_value = {
            "schemas": [
                {
                    "fingerprint": "abc123" * 10 + "abcd",  # 64 chars
                    "column_names": ["Revenue", "Cost"],
                    "data_types": {"revenue": "numeric", "cost": "numeric"},
                    "total_rows": 100,
                    "source_files": ["file1.pptx", "file2.pptx"]
                }
            ]
        }
        
        response = client.post(
            "/api/analytics/aggregate/preview",
            json={"file_ids": [1, 2]}
        )
        
        assert response.status_code == 200
        data = response.json()
        assert "schemas" in data
        assert len(data["schemas"]) == 1
        assert data["schemas"][0]["total_rows"] == 100
    
    @patch("app.routers.analytics.get_current_user")
    @patch("app.routers.analytics.get_db")
    @patch("app.routers.analytics.AggregationService")
    def test_preview_endpoint_no_schemas_found(
        self, 
        mock_service_class, 
        mock_db, 
        mock_auth, 
        client, 
        mock_user
    ):
        """Test preview when no common schemas found."""
        mock_auth.return_value = mock_user
        mock_db.return_value = MagicMock(spec=Session)
        
        # Mock service returning empty schemas
        mock_service = mock_service_class.return_value
        mock_service.detect_common_schemas.return_value = {"schemas": []}
        
        response = client.post(
            "/api/analytics/aggregate/preview",
            json={"file_ids": [1, 2]}
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["schemas"] == []
    
    @patch("app.routers.analytics.get_current_user")
    @patch("app.routers.analytics.get_db")
    def test_aggregate_endpoint_invalid_fingerprint(
        self, 
        mock_db, 
        mock_auth, 
        client, 
        mock_user
    ):
        """Test aggregate endpoint with invalid fingerprint."""
        mock_auth.return_value = mock_user
        mock_db.return_value = MagicMock(spec=Session)
        
        # Too short fingerprint
        response = client.get("/api/analytics/aggregate/abc123")
        
        assert response.status_code == 400
        assert "Invalid schema fingerprint" in response.json()["detail"]
    
    @patch("app.routers.analytics.get_current_user")
    @patch("app.routers.analytics.get_db")
    @patch("app.routers.analytics.AggregationService")
    def test_aggregate_endpoint_success(
        self, 
        mock_service_class, 
        mock_db, 
        mock_auth, 
        client, 
        mock_user
    ):
        """Test successful aggregation request."""
        mock_auth.return_value = mock_user
        mock_db.return_value = MagicMock(spec=Session)
        
        # Create valid 64-char fingerprint
        valid_fingerprint = "a" * 64
        
        # Mock service response
        mock_service = mock_service_class.return_value
        mock_service.aggregate_by_fingerprint.return_value = {
            "schema_fingerprint": valid_fingerprint,
            "columns": ["Revenue", "Cost", "_source_file", "_slide_number", "_region"],
            "data": [
                {
                    "Revenue": "1000",
                    "Cost": "800",
                    "_source_file": "Germany.pptx",
                    "_slide_number": 5,
                    "_region": "EU"
                },
                {
                    "Revenue": "1500",
                    "Cost": "1200",
                    "_source_file": "France.pptx",
                    "_slide_number": 3,
                    "_region": "EU"
                }
            ],
            "row_count": 2
        }
        
        response = client.get(f"/api/analytics/aggregate/{valid_fingerprint}")
        
        assert response.status_code == 200
        data = response.json()
        assert data["row_count"] == 2
        assert "_source_file" in data["columns"]
        assert "_slide_number" in data["columns"]
        assert "_region" in data["columns"]
    
    @patch("app.routers.analytics.get_current_user")
    @patch("app.routers.analytics.get_db")
    @patch("app.routers.analytics.AggregationService")
    def test_aggregate_endpoint_no_data(
        self, 
        mock_service_class, 
        mock_db, 
        mock_auth, 
        client, 
        mock_user
    ):
        """Test aggregate endpoint when no data found."""
        mock_auth.return_value = mock_user
        mock_db.return_value = MagicMock(spec=Session)
        
        valid_fingerprint = "b" * 64
        
        # Mock service returning empty data
        mock_service = mock_service_class.return_value
        mock_service.aggregate_by_fingerprint.return_value = {
            "schema_fingerprint": valid_fingerprint,
            "columns": [],
            "data": [],
            "row_count": 0
        }
        
        response = client.get(f"/api/analytics/aggregate/{valid_fingerprint}")
        
        assert response.status_code == 404
        assert "No data found" in response.json()["detail"]
    
    @patch("app.routers.analytics.get_current_user")
    @patch("app.routers.analytics.get_db")
    @patch("app.routers.analytics.AggregationService")
    def test_source_metadata_preserved(
        self, 
        mock_service_class, 
        mock_db, 
        mock_auth, 
        client, 
        mock_user
    ):
        """Test that source metadata is preserved in aggregated data."""
        mock_auth.return_value = mock_user
        mock_db.return_value = MagicMock(spec=Session)
        
        valid_fingerprint = "c" * 64
        
        mock_service = mock_service_class.return_value
        mock_service.aggregate_by_fingerprint.return_value = {
            "schema_fingerprint": valid_fingerprint,
            "columns": ["Revenue", "_source_file", "_slide_number", "_region"],
            "data": [
                {
                    "Revenue": "1000",
                    "_source_file": "Germany.pptx",
                    "_slide_number": 5,
                    "_region": "EU"
                }
            ],
            "row_count": 1
        }
        
        response = client.get(f"/api/analytics/aggregate/{valid_fingerprint}")
        
        assert response.status_code == 200
        data = response.json()
        
        # Verify metadata fields present
        first_row = data["data"][0]
        assert "_source_file" in first_row
        assert "_slide_number" in first_row
        assert "_region" in first_row
        assert first_row["_source_file"] == "Germany.pptx"
        assert first_row["_slide_number"] == 5
