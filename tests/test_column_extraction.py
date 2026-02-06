"""
Test script to verify column-based table extraction from demo file.
"""
import sys
sys.path.append("backend")

from pptx import Presentation
from app.services.pptx_service import PowerpointManager
import json

# Load the demo presentation
demo_path = "docs/demo/DemoPage.pptx"

print("=" * 80)
print("TESTING COLUMN-BASED TABLE EXTRACTION")
print("=" * 80)

manager = PowerpointManager()
prs = manager.load_powerpoint(demo_path)
slides = manager.extract_slides(prs)

for slide_data in slides:
    print(f"\nSlide {slide_data.slide_index}: {slide_data.title}")
    print(f"  Table data rows: {len(slide_data.table_data)}")
    print(f"  Pseudo tables: {len(slide_data.pseudo_tables)}")
    print(f"  Images: {len(slide_data.image_data)}")
    print(f"  Text elements: {len(slide_data.text_content)}")
    
    if slide_data.pseudo_tables:
        for idx, pt in enumerate(slide_data.pseudo_tables):
            print(f"\n  Pseudo Table {idx}:")
            print(f"    Type: {pt.get('type')}")
            print(f"    Confidence: {pt.get('confidence_score')}")
            data = pt.get('data', [])
            print(f"    Dimensions: {len(data)} rows x {len(data[0]) if data else 0} cols")
            
            if data:
                print(f"\n    Grid Data:")
                # Show first 10 rows
                for row_idx, row in enumerate(data[:10]):
                    row_str = " | ".join([str(cell) if cell else "None" for cell in row])
                    print(f"      Row {row_idx}: {row_str}")
                
                if len(data) > 10:
                    print(f"      ... ({len(data) - 10} more rows)")
    
    if slide_data.table_data:
        print(f"\n  Native table data ({len(slide_data.table_data)} rows):")
        for row_idx, row in enumerate(slide_data.table_data[:5]):
            print(f"    Row {row_idx}: {row}")

print("\n" + "=" * 80)
print("TEST COMPLETE")
print("=" * 80)
