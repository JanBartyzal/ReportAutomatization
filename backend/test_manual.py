"""Quick test script to validate PseudoTableParser"""
from pseudo_table_parser import PseudoTableParser

shapes = [
    {"text": "H1", "top": 100.0, "left": 50.0, "width": 100.0, "height": 20.0},
    {"text": "H2", "top": 100.0, "left": 160.0, "width": 100.0, "height": 20.0},
    {"text": "R1C1", "top": 130.0, "left": 50.0, "width": 100.0, "height": 20.0},
    {"text": "R1C2", "top": 130.0, "left": 160.0, "width": 100.0, "height": 20.0},
    {"text": "R2C1", "top": 160.0, "left": 50.0, "width": 100.0, "height": 20.0},
    {"text": "R2C2", "top": 160.0, "left": 160.0, "width": 100.0, "height": 20.0},
]

parser = PseudoTableParser()
result = parser.parse(shapes)

if result:
    print(f"✅ Parser detected table successfully")
    print(f"   Type: {result['type']}")
    print(f"   Confidence: {result['confidence_score']}")
    print(f"   Rows: {len(result['data'])}")
    print(f"   Cols: {len(result['data'][0]) if result['data'] else 0}")
    print(f"   Data: {result['data']}")
else:
    print("❌ Parser failed to detect table")
