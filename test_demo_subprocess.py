"""
Test extraction with real demo file - using subprocess to avoid encoding issues.
"""
import subprocess
import sys

code = '''
import sys
sys.path.insert(0, "backend")

from pptx import Presentation
from app.services.pptx_service import PowerpointManager

demo_path = "docs/demo/DemoPage.pptx"
manager = PowerpointManager()
prs = manager.load_powerpoint(demo_path)
slides = manager.extract_slides(prs)

for slide_data in slides:
    print(f"Slide {slide_data.slide_index}: {slide_data.title}")
    print(f"  Pseudo tables: {len(slide_data.pseudo_tables)}")
    
    if slide_data.pseudo_tables:
        for idx, pt in enumerate(slide_data.pseudo_tables):
            data = pt.get("data", [])
            print(f"  Table {idx}: {len(data)} rows x {len(data[0]) if data else 0} cols, confidence={pt.get('confidence_score')}")
            
            # Show first 5 rows
            for i, row in enumerate(data[:5]):
                print(f"    Row {i}: {row}")
'''

result = subprocess.run(
    [sys.executable, "-c", code],
    capture_output=True,
    text=True,
    cwd=".",
    encoding='utf-8',
    errors='replace'
)

print("STDOUT:")
print(result.stdout)

if result.stderr:
    print("\nSTDERR:")
    print(result.stderr)

print(f"\nReturn code: {result.returncode}")
