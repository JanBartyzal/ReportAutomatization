import pytest
from unittest.mock import MagicMock
from app.services.aggregation_service import AggregationService

@pytest.fixture
def aggregation_service():
    return AggregationService(fuzzy_threshold=90)

def test_normalize_column_name(aggregation_service):
    """Test column name normalization logic."""
    assert aggregation_service.normalize_column_name("Total Revenue (EUR)") == "total_revenue_eur"
    assert aggregation_service.normalize_column_name("  Column   Name  ") == "column_name"
    assert aggregation_service.normalize_column_name("Col-1!@#$%^&*()Name") == "col_1_name"
    assert aggregation_service.normalize_column_name("Already_Normalized") == "already_normalized"

def test_infer_column_types(aggregation_service):
    """Test data type inference logic."""
    table_data = [
        {"Price": "100", "Name": "Product A", "Tax": "10%"},
        {"Price": "200.50", "Name": "Product B", "Tax": "5.5%"},
        {"Price": "N/A", "Name": "Product C", "Tax": ""}, # Mixed/missing
    ]
    type_map = aggregation_service.infer_column_types(table_data)
    
    assert type_map["Price"] == "numeric"
    assert type_map["Name"] == "string"
    assert type_map["Tax"] == "numeric"

def test_generate_schema_fingerprint(aggregation_service):
    """Test that fingerprint is consistent and handles case/order."""
    headers1 = ["Name", "Revenue", "Region"]
    types1 = {"Name": "string", "Revenue": "numeric", "Region": "string"}
    
    headers2 = ["REGION", "name", "REVENUE"]
    types2 = {"REGION": "string", "name": "string", "REVENUE": "numeric"}
    
    fp1 = aggregation_service.generate_schema_fingerprint(headers1, types1)
    fp2 = aggregation_service.generate_schema_fingerprint(headers2, types2)
    
    assert fp1 == fp2
    assert len(fp1) == 64 # SHA-256

def test_fuzzy_match_columns(aggregation_service):
    """Test fuzzy matching of column headers."""
    assert aggregation_service.fuzzy_match_columns("Revenue", "Revenue") is True
    assert aggregation_service.fuzzy_match_columns("Total Revenue", "Revenue Total") is True 
    assert aggregation_service.fuzzy_match_columns("Revenue (EUR)", "Revenue Total") is False
    assert aggregation_service.fuzzy_match_columns("Rev", "Revenue") is False

def test_extract_schema_from_table_data(aggregation_service):
    """Test schema extraction from raw table data."""
    table_data = [
        {"A": 1, "B": "test"},
        {"A": 2, "B": "prod"}
    ]
    schema = aggregation_service.extract_schema_from_table_data(table_data)
    assert schema is not None
    cols, types = schema
    assert cols == ["A", "B"]
    assert types["A"] == "numeric"
    assert types["B"] == "string"

def test_aggregate_by_fingerprint_logic(aggregation_service):
    """
    Test virtual union logic. 
    Note: This tests the internal logic by mocking DB results if possible, 
    but here we focus on testing the service's aggregation behavior.
    """
    # Mocking SlideData and records for aggregate_by_fingerprint
    # Instead of full DB test, we verify the logic inside aggregate_by_fingerprint 
    # if it correctly merges rows and adds metadata.
    
    db = MagicMock()
    
    # Setup mock records
    mock_slide1 = MagicMock()
    mock_slide1.table_data = [{"Col1": "Val1", "Col2": 10}]
    mock_slide1.slide_index = 1
    
    mock_slide2 = MagicMock()
    mock_slide2.table_data = [{"Col1": "Val2", "Col2": 20}]
    mock_slide2.slide_index = 5
    
    # They should have the same fingerprint
    headers = ["Col1", "Col2"]
    types = {"Col1": "string", "Col2": "numeric"}
    fingerprint = aggregation_service.generate_schema_fingerprint(headers, types)
    
    # Mock DB query results: List of (SlideData, filename, region)
    db.query.return_value.join.return_value.join.return_value.filter.return_value.all.return_value = [
        (mock_slide1, "file1.pptx", "EMEA"),
        (mock_slide2, "file2.pptx", "APAC")
    ]
    
    result = aggregation_service.aggregate_by_fingerprint(fingerprint, "user_oid", db)
    
    assert result["row_count"] == 2
    assert len(result["data"]) == 2
    assert result["data"][0]["_source_file"] == "file1.pptx"
    assert result["data"][0]["_region"] == "EMEA"
    assert result["data"][1]["_source_file"] == "file2.pptx"
    assert result["data"][1]["_region"] == "APAC"
    # Check if metadata columns are added to 'columns'
    assert "_source_file" in result["columns"]
    assert "_region" in result["columns"]
    assert "_slide_number" in result["columns"]
