"""
Test with much larger tolerances to handle real PPT variance.
"""
import sys
import os
import json
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'backend'))

from app.services.parsers.ppt_shapes import PseudoTableParser

# PowerPoint coordinates use EMU (English Metric Units) where 914400 EMU = 1 inch
# The demo file has significant variance in alignment, so use larger tolerances

parser = PseudoTableParser(
    x_tolerance=300000.0,  # ~0.33 inches - columns can vary horizontally  
    y_tolerance=300000.0,  # ~0.33 inches - rows can vary vertically
    min_rows=3,
    min_cols=3
)

shapes = [
    # Header row (top ~ 1108515-1182081, variance of ~73566 EMU)
    {"text": "Cost Category", "top": 1108515, "left": 798971, "width": 1978434, "height": 336322},
    {"text": "Main desc", "top": 1108515, "left": 3026139, "width": 4380974, "height": 336322},
    {"text": "Net saving", "top": 1182081, "left": 8920753, "width": 1485146, "height": 153888},
    {"text":  "Additional saving", "top": 1133765, "left": 10431796, "width": 1279177, "height": 307777},
    
    # Row 1 (top variance: 1455237-1503083 = 47846 EMU)
    {"text": "Text Category", "top": 1503083, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row Text row", "top": 1455675, "left": 2954784, "width": 5043527, "height": 555742},
    {"text": "-21.4", "top": 1455237, "left": 9079535, "width": 1241809, "height": 555742},
    {"text": "-2", "top": 1492964, "left": 10502720, "width": 1314626, "height": 485193},
    
    # Row 2
    {"text": "Text Category", "top": 2049052, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row", "top": 2018039, "left": 2961031, "width": 5043527, "height": 555742},
    {"text": "-1.9", "top": 2046734, "left": 9072740, "width": 1241809, "height": 555742},
    {"text": ".", "top": 2038933, "left": 10502720, "width": 1314626, "height": 485193},
    
    # Row 3
    {"text": "Text Category", "top": 2595021, "left": 771788, "width": 1978987, "height": 485193},
    {"text": "Text row Text row Text row", "top": 2572390, "left": 2961031, "width": 5279146, "height": 555742},
    {"text": "-3.2", "top": 2552495, "left": 9072741, "width": 1241809, "height": 555742},
    {"text": ".", "top": 2584902, "left": 10502720, "width": 1314626, "height": 485193},
]

print(f"Testing with {len(shapes)} shapes")
print(f"Tolerance config: x={parser.x_tolerance}, y={parser.y_tolerance}")

results = parser.parse(shapes)

output = {
    "tables_detected": len(results),
    "results": results
}

with open('test_large_tolerance.json', 'w', encoding='utf-8') as f:
    json.dump(output, f, indent=2, ensure_ascii=False)

print(f"Detected: {len(results)} table(s)")
if results:
    data = results[0].get('data', [])
    print(f"Shape: {len(data)} rows x {len(data[0]) if data else 0} cols")
    print(f"Confidence: {results[0].get('confidence_score')}")
    print("\nFirst few rows:")
    for i, row in enumerate(data[:3]):
        print(f"  Row {i}: {row}")
else:
    print("No tables detected - check test_large_tolerance.json for details")
