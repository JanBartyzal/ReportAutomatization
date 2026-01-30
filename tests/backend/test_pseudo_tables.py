import pytest
from app.services.parsers.ppt_shapes import PseudoTableParser

@pytest.fixture
def parser():
    return PseudoTableParser(x_tolerance=10, y_tolerance=10, min_rows=3, min_cols=2)

def test_perfect_grid(parser):
    """Test detection of a perfectly aligned 3x2 grid."""
    shapes = [
        {"text": "R1C1", "top": 100, "left": 100, "width": 50, "height": 20},
        {"text": "R1C2", "top": 100, "left": 160, "width": 50, "height": 20},
        {"text": "R2C1", "top": 130, "left": 100, "width": 50, "height": 20},
        {"text": "R2C2", "top": 130, "left": 160, "width": 50, "height": 20},
        {"text": "R3C1", "top": 160, "left": 100, "width": 50, "height": 20},
        {"text": "R3C2", "top": 160, "left": 160, "width": 50, "height": 20},
    ]
    results = parser.parse(shapes)
    assert len(results) == 1
    table = results[0]
    assert table["type"] == "pseudo_table"
    assert table["confidence_score"] > 0.9
    assert len(table["data"]) == 3
    assert len(table["data"][0]) == 2
    assert table["data"][0][0] == "R1C1"
    assert table["data"][2][1] == "R3C2"

def test_alignment_jitter(parser):
    """Test detection with slight positional variations (within tolerance)."""
    shapes = [
        {"text": "R1C1", "top": 100, "left": 100, "width": 50, "height": 20},
        {"text": "R1C2", "top": 102, "left": 165, "width": 50, "height": 20},
        {"text": "R2C1", "top": 135, "left": 98, "width": 50, "height": 20},
        {"text": "R2C2", "top": 133, "left": 162, "width": 50, "height": 20},
        {"text": "R3C1", "top": 161, "left": 103, "width": 50, "height": 20},
        {"text": "R3C2", "top": 159, "left": 158, "width": 50, "height": 20},
    ]
    results = parser.parse(shapes)
    assert len(results) == 1
    assert results[0]["confidence_score"] < 1.0
    assert len(results[0]["data"]) == 3

def test_missing_cells(parser):
    """Test grid with a missing cell (should be None)."""
    shapes = [
        {"text": "R1C1", "top": 100, "left": 100, "width": 50, "height": 20},
        {"text": "R1C2", "top": 100, "left": 160, "width": 50, "height": 20},
        {"text": "R2C1", "top": 130, "left": 100, "width": 50, "height": 20},
        {"text": "R3C1", "top": 160, "left": 100, "width": 50, "height": 20},
        {"text": "R3C2", "top": 160, "left": 160, "width": 50, "height": 20},
    ]
    results = parser.parse(shapes)
    assert len(results) == 1
    data = results[0]["data"]
    assert data[1][1] is None

def test_noise_rejection(parser):
    """Test that random non-grid shapes are rejected."""
    shapes = [
        {"text": "Title", "top": 10, "left": 10, "width": 100, "height": 20},
        {"text": "Footer", "top": 500, "left": 10, "width": 100, "height": 20},
        {"text": "Random 1", "top": 100, "left": 100, "width": 50, "height": 20},
        {"text": "Random 2", "top": 200, "left": 300, "width": 50, "height": 20},
    ]
    results = parser.parse(shapes)
    assert len(results) == 0
