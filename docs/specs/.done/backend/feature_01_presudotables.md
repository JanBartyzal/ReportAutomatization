# Feature Specification: Pseudo-Table Detection

**Feature ID:** FEAT-01
**Parent Epic:** Table Extraction Engine
**Related DoD:** `dod_criteria.md`

---

## 1. Problem Statement
Many PowerPoint presentations contain data that visually looks like a table but is constructed using separate Text Box objects (`AutoShape` with text) rather than a native PowerPoint Table object.
Currently, our parser skips these or reads them as unstructured text. We need to detect these visual grids and convert them into structured JSON, just like native tables.

## 2. Technical Requirements

### Input Data
- A list of `Shape` objects from a single slide.
- Each shape has:
  - `text`: String content.
  - `geometry`: `top`, `left`, `width`, `height` (coordinates).

### Detection Logic (Heuristics)
The algorithm should identify a "Pseudo-Table" if:
1.  **Grouping:** There is a cluster of at least 6 text boxes in close proximity.
2.  **Row Alignment:** Text boxes share similar `top` coordinates (within a `y_tolerance` of ~10px).
3.  **Column Alignment:** Text boxes share similar `left` coordinates (within an `x_tolerance` of ~10px).
4.  **Density:** The arrangement forms a grid of at least 2 columns and 3 rows.

### Algorithm Steps
1.  **Filter:** Select only text-containing shapes from the slide.
2.  **Sort:** Sort shapes by `top` (Y-axis), then `left` (X-axis).
3.  **Row Clustering:** Group shapes into "candidate rows" based on vertical overlap.
4.  **Column Detection:** Analyze "candidate rows" to determine common column boundaries.
5.  **Grid Validation:** Check if the structure forms a coherent grid.
    - *Edge Case:* Handling merged cells (a shape that spans multiple column boundaries).
    - *Edge Case:* Handling empty cells (gaps in the grid).
6.  **Extraction:** Convert the grid into a List of Lists (or List of Dicts if a header row is detected).

## 3. Output Format (JSON)
The output must match the existing schema for native tables:

```json
{
  "type": "pseudo_table",
  "confidence_score": 0.92, // Calculated based on alignment perfection
  "bbox": [100, 200, 500, 400], // Bounding box of the whole table
  "data": [
    ["Header 1", "Header 2", "Header 3"],
    ["Row 1 Col 1", "Row 1 Col 2", "Row 1 Col 3"],
    ["Row 2 Col 1", null, "Row 2 Col 3"] // Note null for empty cell
  ]
}