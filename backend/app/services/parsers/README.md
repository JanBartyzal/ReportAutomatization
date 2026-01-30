# Shape Parsers Module

## Purpose
This module contains parsers for extracting structured data from PowerPoint shapes that are not native PowerPoint objects.

## Key Components
- `ppt_shapes.py`: Contains `PseudoTableParser` which detects grid-like arrangements of separate text boxes and converts them into structured tables.

## Usage
```python
from app.services.parsers.ppt_shapes import PseudoTableParser

parser = PseudoTableParser(x_tolerance=10, y_tolerance=10)
shapes = [
    {"text": "Val 1", "top": 100, "left": 100, "width": 50, "height": 20},
    {"text": "Val 2", "top": 100, "left": 160, "width": 50, "height": 20},
    # ...
]
results = parser.parse(shapes)
# Returns a list of pseudo-table dictionaries
```
