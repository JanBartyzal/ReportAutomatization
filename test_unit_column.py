"""
Simple unit test for column-based table detection.
"""
import sys
import os

# Setup path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'backend'))

# Direct import test
try:
    from app.services.parsers.ppt_shapes import PseudoTableParser
    print("✓ Successfully imported PseudoTableParser")
except Exception as e:
    print(f"✗ Import failed: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)

# Test column-based detection
print("\n" + "=" * 80)
print("TEST 1: Column-Based Layout (4 columns x 3 rows)")
print("=" * 80)

parser = PseudoTableParser(x_tolerance=50.0, y_tolerance=50.0, min_rows=3, min_cols=2)

# Simulate column-based layout from demo file
# Column 1 at left=771788, Column 2 at left=2961031, Column 3 at left=9092030, Column 4 at left=10502720
shapes = [
    # Column 1 (left=771788) - "Text Category"
    {"text": "Text Category", "top": 1503083, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text Category", "top": 2049052, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text Category", "top": 2595021, "left": 771788, "width": 1978987, "height": 485193},
    
    # Column 2 (left~2961031) - Row descriptions
    {"text": "Text row Text row", "top": 1455675, "left": 2954784, "width": 5043527, "height": 555742},
    {"text": "Text row", "top": 2018039, "left": 2961031, "width": 5043527, "height": 555742},
    {"text": "Text row Text row Text row", "top": 2572390, "left": 2961031, "width": 5279146, "height": 555742},
    
    # Column 3 (left~9079535) - Numeric values
    {"text": "-21.4 +2.2", "top": 1455237, "left": 9079535, "width": 1241809, "height": 555742},
    {"text": "-1.9", "top": 2046734, "left": 9072740, "width": 1241809, "height": 555742},
    {"text": "-3.2 -5.53 -0.3", "top": 2552495, "left": 9072741, "width": 1241809, "height": 555742},
    
    # Column 4 (left=10502720) - Additional values
    {"text": "-2", "top": 1492964, "left": 10502720, "width": 1314626, "height": 485193},
    {"text": ".", "top": 2038933, "left": 10502720, "width": 1314626, "height": 485193},
    {"text": ".", "top": 2584902, "left": 10502720, "width": 1314626, "height": 485193},
]

print(f"Input: {len(shapes)} shapes arranged in columns")
results = parser.parse(shapes)

if results:
    print(f"\n✓ Detected {len(results)} pseudo-table(s)")
    for idx, table in enumerate(results):
        print(f"\nTable {idx}:")
        print(f"  Type: {table['type']}")
        print(f"  Confidence: {table['confidence_score']}")
        data = table.get('data', [])
        print(f"  Dimensions: {len(data)} rows x {len(data[0]) if data else 0} cols")
        
        if data:
            print(f"\n  Grid Data:")
            for row_idx, row in enumerate(data):
                row_str = " | ".join([str(cell) if cell else "None" for cell in row])
                print(f"    Row {row_idx}: {row_str}")
else:
    print("\n✗ No pseudo-tables detected")

print("\n" + "=" * 80)
print("TEST COMPLETE")
print("=" * 80)
