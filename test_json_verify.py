"""
JSON-based verification test for column-based table detection.
Writes results to JSON file to avoid terminal encoding issues.
"""
import sys
import os
import json
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'backend'))

from app.services.parsers.ppt_shapes import PseudoTableParser

# Create parser with appropriate tolerances
parser = PseudoTableParser(
    x_tolerance=100000.0,  
    y_tolerance=50000.0,
    min_rows=3,
    min_cols=3
)

# Test data from demo file
shapes = [
    # Header row
    {"text": "Cost Category", "top": 1108515, "left": 798971, "width": 1978434, "height": 336322},
    {"text": "Main saving initiatives", "top": 1108515, "left": 3026139, "width": 4380974, "height": 336322},
    {"text": "Net saving 2024", "top": 1182081, "left": 8920753, "width": 1485146, "height": 153888},
    {"text":  "Additional net saving 2024", "top": 1133765, "left": 10431796, "width": 1279177, "height": 307777},
    
    # Row 1
    {"text": "Text Category", "top": 1503083, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row Text row", "top": 1455675, "left": 2954784, "width": 5043527, "height": 555742},
    {"text": "-21.4 +2.2", "top": 1455237, "left": 9079535, "width": 1241809, "height": 555742},
    {"text": "-2", "top": 1492964, "left": 10502720, "width": 1314626, "height": 485193},
    
    # Row 2
    {"text": "Text Category", "top": 2049052, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row", "top": 2018039, "left": 2961031, "width": 5043527, "height": 555742},
    {"text": "-1.9", "top": 2046734, "left": 9072740, "width": 1241809, "height": 555742},
    {"text": ".", "top": 2038933, "left": 10502720, "width": 1314626, "height": 485193},
    
    # Row 3
    {"text": "Text Category", "top": 2595021, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row Text row Text row", "top": 2572390, "left": 2961031, "width": 5279146, "height": 555742},
    {"text": "-3.2 -5.53 -0.3", "top": 2552495, "left": 9072741, "width": 1241809, "height": 555742},
    {"text": ".", "top": 2584902, "left": 10502720, "width": 1314626, "height": 485193},
]

results = parser.parse(shapes)

output = {
    "test_name": "Column-Based Table Detection with Real Demo Coordinates",
    "input_shapes_count": len(shapes),
    "expected_structure": "4 columns x 4 rows (header + 3 data)",
    "detected_tables": len(results),
    "success": len(results) > 0,
    "results": results
}

# Write to JSON file
with open('test_verification.json', 'w', encoding='utf-8') as f:
    json.dump(output, f, indent=2, ensure_ascii=False)

print(f"Test complete. Results written to test_verification.json")
print(f"Detected {len(results)} table(s)")
if results:
    data = results[0].get('data', [])
    print(f"Table shape: {len(data)} rows x {len(data[0]) if data else 0} cols")
    print(f"Confidence: {results[0].get('confidence_score')}")
