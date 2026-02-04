"""
Direct test of PseudoTableParser with actual demo file shape coordinates.
"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'backend'))

from app.services.parsers.ppt_shapes import PseudoTableParser

print("=" * 80)
print("TESTING WITH REAL DEMO FILE COORDINATES")
print("=" * 80)

# Create parser with tolerances appropriate for PowerPoint units (EMU - English Metric Units)
# Default tolerance of 10.0 is too small for PPT coordinates (which are in the millions)
parser = PseudoTableParser(
    x_tolerance=100000.0,  # ~0.1 inch tolerance for column alignment
    y_tolerance=50000.0,   # ~0.05 inch tolerance for row alignment
    min_rows=3,
    min_cols=3
)

# Real coordinates from demo file analysis - representing the table data
# Column 1: Text Category (shapes 12-19 at left~771788)
# Column 2: Descriptions (shapes 60, 59, 58, etc at left~2961031)
# Column 3: Numeric values (shapes 67, 66, 65, etc at left~9079535)
# Column 4: Additional values (shapes 30-37 at left~10502720)

shapes = [
    # Header row (around top=1108515)
    {"text": "Cost Category", "top": 1108515, "left": 798971, "width": 1978434, "height": 336322},
    {"text": "Main saving initiatives description", "top": 1108515, "left": 3026139, "width": 4380974, "height": 336322},
    {"text": "Net saving 2024 (M€)", "top": 1182081, "left": 8920753, "width": 1485146, "height": 153888},
    {"text":  "Additional net saving 2024 (M€)", "top": 1133765, "left": 10431796, "width": 1279177, "height": 307777},
    
    # Data rows
    # Row 1: ~top=1455237-1503083
    {"text": "Text Category", "top": 1503083, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row Text row", "top": 1455675, "left": 2954784, "width": 5043527, "height": 555742},
    {"text": "-21.4 +2.2", "top": 1455237, "left": 9079535, "width": 1241809, "height": 555742},
    {"text": "-2", "top": 1492964, "left": 10502720, "width": 1314626, "height": 485193},
    
    # Row 2: ~top=2018039-2049052
    {"text": "Text Category", "top": 2049052, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row", "top": 2018039, "left": 2961031, "width": 5043527, "height": 555742},
    {"text": "-1.9", "top": 2046734, "left": 9072740, "width": 1241809, "height": 555742},
    {"text": ".", "top": 2038933, "left": 10502720, "width": 1314626, "height": 485193},
    
    # Row 3: ~top=2552495-2595021
    {"text": "Text Category", "top": 2595021, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row Text row Text row", "top": 2572390, "left": 2961031, "width": 5279146, "height": 555742},
    {"text": "-3.2 -5.53 -0.3", "top": 2552495, "left": 9072741, "width": 1241809, "height": 555742},
    {"text": ".", "top": 2584902, "left": 10502720, "width": 1314626, "height": 485193},
]

print(f"\nInput: {len(shapes)} shapes")
print("Expected: 4 columns x 4 rows (1 header + 3 data rows)")

results = parser.parse(shapes)

if results:
    print(f"\n✓ SUCCESS: Detected {len(results)} pseudo-table(s)\n")
    for idx, table in enumerate(results):
        print(f"Table {idx}:")
        print(f"  Type: {table['type']}")
        print(f"  Confidence: {table['confidence_score']}")
        data = table.get('data', [])
        print(f"  Dimensions: {len(data)} rows x {len(data[0]) if data else 0} cols\n")
        
        if data:
            print("  Grid Data:")
            for row_idx, row in enumerate(data):
                row_str = " | ".join([str(cell)[:30] if cell else "None" for cell in row])
                print(f"    Row {row_idx}: {row_str}")
                
            # Verify expected structure
            print("\n  Verification:")
            if len(data) >= 4:
                print(f"    ✓ Has at least 4 rows")
            else:
                print(f"    ✗ Expected 4+ rows, got {len(data)}")
                
            if len(data[0]) >= 4:
                print(f"    ✓ Has 4 columns")
            else:
                print(f"    ✗ Expected 4 columns, got {len(data[0])}")
                
            if data[0][0] and "Cost" in data[0][0]:
                print(f"    ✓ Header row detected correctly")
            else:
                print(f"    ✗ Header row issue: {data[0][0]}")
else:
    print("\n✗ FAILED: No pseudo-tables detected")
    print("This means the column-based detection did not find a valid table")

print("\n" + "=" * 80)
print("TEST COMPLETE")
print("=" * 80)
