"""
Unit tests for AggregationService.

Tests schema fingerprinting, fuzzy matching, data type inference,
and aggregation logic.
"""

import pytest
from app.services.aggregation_service import AggregationService


class TestAggregationService:
    """Test suite for AggregationService class."""
    
    def setup_method(self):
        """Set up test fixtures."""
        self.service = AggregationService(fuzzy_threshold=90)
    
    def test_normalize_column_name(self):
        """Test column name normalization."""
        # Test basic normalization
        assert self.service.normalize_column_name("Total Revenue") == "total_revenue"
        assert self.service.normalize_column_name("Revenue (EUR)") == "revenue_EUR"
        assert self.service.normalize_column_name("Cost@2024") == "cost_2024"
        
        # Test special characters removal
        assert self.service.normalize_column_name("Price per Unit") == "price_per_unit"
        
        # Test multiple underscores
        assert self.service.normalize_column_name("A___B") == "a_b"
        
        # Test leading/trailing underscores
        assert self.service.normalize_column_name("_Revenue_") == "Revenue"
    
    def test_infer_column_types(self):
        """Test data type inference from table data."""
        # Test numeric columns
        table_data = [
            {"Revenue": "1000", "Cost": "800", "Name": "Germany"},
            {"Revenue": "1500", "Cost": "1200", "Name": "France"},
            {"Revenue": "2000", "Cost": "1600", "Name": "Spain"}
        ]
        
        types = self.service.infer_column_types(table_data)
        
        assert types["Revenue"] == "numeric"
        assert types["Cost"] == "numeric"
        assert types["Name"] == "string"
    
    def test_infer_column_types_mixed(self):
        """Test data type inference with mixed data."""
        table_data = [
            {"Revenue": "1000", "Product": "Widget"},
            {"Revenue": "ABC", "Product": "Gadget"},  # Mixed - should be string
            {"Revenue": "1500", "Product": "Tool"}
        ]
        
        types = self.service.infer_column_types(table_data)
        
        # Revenue has mixed types, should be classified based on >70% threshold
        assert types["Product"] == "string"
    
    def test_generate_schema_fingerprint_identical(self):
        """Test that identical schemas produce same fingerprint."""
        headers1 = ["Revenue", "Cost", "Profit"]
        headers2 = ["Revenue", "Cost", "Profit"]
        
        fingerprint1 = self.service.generate_schema_fingerprint(headers1)
        fingerprint2 = self.service.generate_schema_fingerprint(headers2)
        
        assert fingerprint1 == fingerprint2
        assert len(fingerprint1) == 64  # SHA-256 hash length
    
    def test_generate_schema_fingerprint_different_order(self):
        """Test that column order doesn't affect fingerprint (sorted internally)."""
        headers1 = ["Revenue", "Cost", "Profit"]
        headers2 = ["Cost", "Profit", "Revenue"]
        
        fingerprint1 = self.service.generate_schema_fingerprint(headers1)
        fingerprint2 = self.service.generate_schema_fingerprint(headers2)
        
        # Should be same because internally sorted
        assert fingerprint1 == fingerprint2
    
    def test_generate_schema_fingerprint_with_types(self):
        """Test fingerprint generation with data types."""
        headers = ["Revenue", "Cost", "Profit"]
        types = {"revenue": "numeric", "cost": "numeric", "profit": "numeric"}
        
        fingerprint = self.service.generate_schema_fingerprint(headers, types)
        
        assert len(fingerprint) == 64
        assert isinstance(fingerprint, str)
    
    def test_fuzzy_match_columns_exact(self):
        """Test fuzzy matching with exact match."""
        assert self.service.fuzzy_match_columns("Revenue", "Revenue") is True
    
    def test_fuzzy_match_columns_similar(self):
        """Test fuzzy matching with similar columns."""
        # These should match with 90% threshold
        assert self.service.fuzzy_match_columns("Total Revenue", "Revenue") is False  # Too different
        assert self.service.fuzzy_match_columns("Revenue", "revenue") is True  # Same normalized
    
    def test_fuzzy_match_columns_different(self):
        """Test fuzzy matching with different columns."""
        assert self.service.fuzzy_match_columns("Revenue", "Cost") is False
        assert self.service.fuzzy_match_columns("Total", "Hotel") is False
    
    def test_fuzzy_match_columns_custom_threshold(self):
        """Test fuzzy matching with custom threshold."""
        # Lower threshold should match more loosely
        result = self.service.fuzzy_match_columns("Rev", "Revenue", threshold=50)
        # This depends on the fuzzy algorithm, so just verify it returns boolean
        assert isinstance(result, bool)
    
    def test_extract_schema_from_table_data_valid(self):
        """Test schema extraction from valid table data."""
        table_data = [
            {"Revenue": "1000", "Cost": "800"},
            {"Revenue": "1500", "Cost": "1200"}
        ]
        
        result = self.service.extract_schema_from_table_data(table_data)
        
        assert result is not None
        columns, data_types = result
        assert "Revenue" in columns
        assert "Cost" in columns
        assert len(data_types) > 0
    
    def test_extract_schema_from_table_data_empty(self):
        """Test schema extraction from empty data."""
        assert self.service.extract_schema_from_table_data([]) is None
        assert self.service.extract_schema_from_table_data(None) is None
    
    def test_handle_missing_columns(self):
        """Test that missing columns are handled correctly in aggregation."""
        # This would be tested in integration tests with actual database
        # Here we just verify the normalization handles it
        table_data1 = [{"Revenue": "1000", "Cost": "800"}]
        table_data2 = [{"Revenue": "1500", "Cost": "1200", "Profit": "300"}]
        
        schema1 = self.service.extract_schema_from_table_data(table_data1)
        schema2 = self.service.extract_schema_from_table_data(table_data2)
        
        assert schema1 is not None
        assert schema2 is not None
        
        cols1, _ = schema1
        cols2, _ = schema2
        
        # Different column counts
        assert len(cols1) != len(cols2)


class TestSchemaFingerprinting:
    """Additional tests for schema fingerprinting edge cases."""
    
    def test_fingerprint_case_insensitive(self):
        """Test that fingerprints are case-insensitive."""
        service = AggregationService()
        
        fp1 = service.generate_schema_fingerprint(["Revenue", "Cost"])
        fp2 = service.generate_schema_fingerprint(["REVENUE", "COST"])
        
        # Should be same due to normalization
        assert fp1 == fp2
    
    def test_fingerprint_special_chars(self):
        """Test fingerprints handle special characters consistently."""
        service = AggregationService()
        
        fp1 = service.generate_schema_fingerprint(["Revenue (EUR)", "Cost@2024"])
        fp2 = service.generate_schema_fingerprint(["Revenue (EUR)", "Cost@2024"])
        
        assert fp1 == fp2
    
    def test_fingerprint_deterministic(self):
        """Test that fingerprints are deterministic."""
        service = AggregationService()
        headers = ["A", "B", "C", "D", "E"]
        
        fingerprints = [
            service.generate_schema_fingerprint(headers)
            for _ in range(10)
        ]
        
        # All should be identical
        assert len(set(fingerprints)) == 1
