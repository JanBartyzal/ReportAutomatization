import sys
import os
import statistics
sys.path.append("backend")
from app.services.parsers.ppt_shapes import PseudoTableParser

print("Starting debug test...")
p = PseudoTableParser()
shapes = [
    {"text": "R1C1", "top": 100.0, "left": 100.0, "width": 50.0, "height": 20.0},
    {"text": "R1C2", "top": 100.0, "left": 160.0, "width": 50.0, "height": 20.0},
    {"text": "R2C1", "top": 130.0, "left": 100.0, "width": 50.0, "height": 20.0},
    {"text": "R2C2", "top": 130.0, "left": 160.0, "width": 50.0, "height": 20.0},
    {"text": "R3C1", "top": 160.0, "left": 100.0, "width": 50.0, "height": 20.0},
    {"text": "R3C2", "top": 160.0, "left": 160.0, "width": 50.0, "height": 20.0},
]
print(f"Input shapes count: {len(shapes)}")
try:
    results = p.parse(shapes)
    print(f"Results: {results}")
except Exception as e:
    print(f"CRASHED: {e}")
    import traceback
    traceback.print_exc()
print("Debug test finished.")
