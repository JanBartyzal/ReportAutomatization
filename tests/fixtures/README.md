# Test Fixtures

This directory contains sample test files for testing the report platform parsers and atomizers.

## Directory Structure

```
tests/fixtures/
├── pptx/           # Sample PPTX files
├── xlsx/           # Sample XLSX files
├── pdf/            # Sample PDF files
└── csv/            # Sample CSV files
```

## Test Files

### CSV Files (already provided)
- `csv/sample_comma_utf8.csv` - Comma-delimited, UTF-8 encoding
- `csv/sample_semicolon_windows1250.csv` - Semicolon-delimited, Windows-1250 encoding (Czech)
- `csv/sample_tab_tsv.txt` - Tab-delimited TSV file

### Creating Sample PPTX Files
Use Python to create sample PPTX files with tables, charts, and notes:

```python
from pptx import Presentation
from pptx.util import Inches, Pt

prs = Presentation()
slide = prs.slides.add_slide(prs.slide_layouts[1])  # Title and Content

# Add title
title = slide.shapes.title
title.text = "Sample Presentation"

# Add content with bullet points
content = slide.placeholders[1]
tf = content.text_frame
tf.text = "First bullet point"
p = tf.add_paragraph()
p.text = "Second bullet point"

prs.save('sample.pptx')
```

### Creating Sample XLSX Files
Use Python with openpyxl:

```python
from openpyxl import Workbook

wb = Workbook()
ws = wb.active
ws.title = "Sheet1"

# Add data
ws['A1'] = 'Name'
ws['B1'] = 'Value'
ws['A2'] = 'Item 1'
ws['B2'] = 100

wb.save('sample.xlsx')
```

### Creating Sample PDF Files
For text PDFs, use Python with reportlab or save from other applications.
For OCR test files, you may need to scan documents or use image-to-PDF tools.

### EICAR Test File
For antivirus testing, create a file with the EICAR test string:

```
X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*
```

This string is recognized by all antivirus software as a test virus.

## Usage in Tests

Reference these files in tests like:

```python
import pytest

@pytest.fixture
def sample_csv():
    with open('tests/fixtures/csv/sample_comma_utf8.csv', 'rb') as f:
        return f.read()
```
