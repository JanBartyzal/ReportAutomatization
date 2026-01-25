"""Debug script to test row clustering"""
from pseudo_table_parser import PseudoTableParser

shapes = [
    {"text": "H1", "top": 100.0, "left": 50.0, "width": 150.0, "height": 25.0},
    {"text": "H2", "top": 102.0, "left": 205.0, "width": 150.0, "height": 25.0},
    {"text": "R1C1", "top": 135.0, "left": 52.0, "width": 150.0, "height": 25.0},
    {"text": "R1C2", "top": 136.0, "left": 207.0, "width": 150.0, "height": 25.0},
    {"text": "R2C1", "top": 170.0, "left": 48.0, "width": 150.0, "height": 25.0},
    {"text": "R2C2", "top": 171.0, "left": 203.0, "width": 150.0, "height": 25.0},
]

parser = PseudoTableParser()
sorted_shapes = parser._sort_shapes(shapes)
rows = parser._cluster_rows(sorted_shapes)

print(f'Number of rows detected: {len(rows)}')
for i, row in enumerate(rows):
    texts = [s["text"] for s in row]
    tops = [s["top"] for s in row]
    print(f'  Row {i}: {texts} (tops: {tops})')

columns = parser._detect_columns(rows)
print(f'\\nNumber of columns detected: {len(columns)}')
print(f'Column positions: {columns}')

result = parser.parse(shapes)
print(f'\\nFinal result: {result}')
