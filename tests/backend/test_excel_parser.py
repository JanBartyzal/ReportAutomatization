import pytest
import pandas as pd
import os
from app.services.parsers.excel import ExcelProcessor
from app.schemas.slide import SlideData

def test_excel_processor_simple(tmp_path):
    # Create simple Excel file
    df = pd.DataFrame({"Name": ["John", "Jane"], "Age": [30, 25]})
    file_path = tmp_path / "test.xlsx"
    df.to_excel(file_path, index=False)
    
    processor = ExcelProcessor()
    slides = processor.parse(str(file_path))
    
    assert len(slides) == 1
    assert isinstance(slides[0], SlideData)
    assert slides[0].title == "Sheet: Sheet1"
    assert len(slides[0].table_data) == 2
    assert slides[0].table_data[0]["Name"] == "John"

def test_excel_processor_multi_sheet(tmp_path):
    # Create multi-sheet Excel file
    df1 = pd.DataFrame({"A": [1]})
    df2 = pd.DataFrame({"B": [2]})
    file_path = tmp_path / "multi.xlsx"
    with pd.ExcelWriter(file_path) as writer:
        df1.to_excel(writer, sheet_name="Sheet1", index=False)
        df2.to_excel(writer, sheet_name="Sheet2", index=False)
    
    processor = ExcelProcessor()
    slides = processor.parse(str(file_path))
    
    assert len(slides) == 2
    assert slides[0].title == "Sheet: Sheet1"
    assert slides[1].title == "Sheet: Sheet2"
    assert slides[1].table_data[0]["B"] == 2

def test_excel_processor_empty_sheet(tmp_path):
    # Create Excel with an empty sheet
    df = pd.DataFrame()
    file_path = tmp_path / "empty.xlsx"
    df.to_excel(file_path, index=False)
    
    processor = ExcelProcessor()
    slides = processor.parse(str(file_path))
    
    assert len(slides) == 0
