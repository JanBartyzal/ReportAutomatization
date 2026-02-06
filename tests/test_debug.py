"""
Debug version with detailed logging.
"""
import sys
import os
import json
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'backend'))

from app.services.parsers.ppt_shapes import PseudoTableParser

# Patch the parser to add debug logging
original_try_row = PseudoTableParser._try_row_based_detection
original_try_col = PseudoTableParser._try_column_based_detection

def debug_try_row(self, shapes):
    print(f"  Trying row-based detection with {len(shapes)} shapes...")
    result = original_try_row(self, shapes)
    print(f"    Row-based result: {len(result)} tables")
    return result

def debug_try_col(self, shapes):
    print(f"  Trying column-based detection with {len(shapes)} shapes...")
    
    # Sort and cluster
    sorted_shapes = sorted(shapes, key=lambda s: (s.left, s.top))
    columns = self._cluster_columns(sorted_shapes)
    print(f"    Found {len(columns)} columns (min required: {self.min_cols})")
    
    if len(columns) >= self.min_cols:
        rows = self._detect_rows_in_columns(columns)
        print(f"    Found {len(rows)} rows (min required: {self.min_rows})")
        
        if len(rows) >= self.min_rows:
            valid = self._validate_column_grid(columns, rows)
            print(f"    Grid validation: {valid}")
    
    result = original_try_col(self, shapes)
    print(f"    Column-based result: {len(result)} tables")
    return result

PseudoTableParser._try_row_based_detection = debug_try_row
PseudoTableParser._try_column_based_detection = debug_try_col

# Create parser
parser = PseudoTableParser(
    x_tolerance=100000.0,
    y_tolerance=50000.0,
    min_rows=3,
    min_cols=3
)

shapes = [
    # Header
    {"text": "Cost Category", "top": 1108515, "left": 798971, "width": 1978434, "height": 336322},
    {"text": "Main saving initiatives", "top": 1108515, "left": 3026139, "width": 4380974, "height": 336322},
    {"text": "Net saving 2024", "top": 1182081, "left": 8920753, "width": 1485146, "height": 153888},
    {"text":  "Additional net saving 2024", "top": 1133765, "left": 10431796, "width": 1279177, "height": 307777},
    
    # Data rows
    {"text": "Text Category", "top": 1503083, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row Text row", "top": 1455675, "left": 2954784, "width": 5043527, "height": 555742},
    {"text": "-21.4", "top": 1455237, "left": 9079535, "width": 1241809, "height": 555742},
    {"text": "-2", "top": 1492964, "left": 10502720, "width": 1314626, "height": 485193},
    
    {"text": "Text Category", "top": 2049052, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row", "top": 2018039, "left": 2961031, "width": 5043527, "height": 555742},
    {"text": "-1.9", "top": 2046734, "left": 9072740, "width": 1241809, "height": 555742},
    {"text": ".", "top": 2038933, "left": 10502720, "width": 1314626, "height": 485193},
    
    {"text": "Text Category", "top": 2595021, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row Text row Text row", "top": 2572390, "left": 2961031, "width": 5279146, "height": 555742},
    {"text": "-3.2", "top": 2552495, "left": 9072741, "width": 1241809, "height": 555742},
    {"text": ".", "top": 2584902, "left": 10502720, "width": 1314626, "height": 485193},
]

print(f"Testing with {len(shapes)} shapes")
print(f"Parser config: x_tol={parser.x_tolerance}, y_tol={parser.y_tolerance}, min_rows={parser.min_rows}, min_cols={parser.min_cols}\n")

results = parser.parse(shapes)

print(f"\nFinal result: {len(results)} table(s) detected")

# Write to file
with open('debug_results.json', 'w', encoding='utf-8') as f:
    output = {
        "tables_detected": len(results),
        "results": results
    }
    json.dump(output, f, indent=2, ensure_ascii=False)
    print(f"Results written to debug_results.json")

if results:
    data = results[0].get('data', [])
    print(f"Shape: {len(data)} rows x {len(data[0]) if data else 0} cols")
    print(f"Confidence: {results[0].get('confidence_score')}")

