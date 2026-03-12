"""Unit tests for PptxParser using mocked python-pptx objects."""

from __future__ import annotations

from unittest.mock import MagicMock, PropertyMock, patch

import pytest

from src.atomizers.pptx.service.pptx_parser import PptxParser


# ---------------------------------------------------------------------------
# Helpers to build mock python-pptx objects
# ---------------------------------------------------------------------------


def _make_text_frame(text: str) -> MagicMock:
    tf = MagicMock()
    tf.text = text
    return tf


def _make_placeholder(idx: int, text: str) -> MagicMock:
    shape = MagicMock()
    shape.has_text_frame = True
    shape.text_frame = _make_text_frame(text)
    shape.placeholder_format.idx = idx
    shape.name = f"Placeholder {idx}"
    shape.left = 100000
    shape.top = 200000
    shape.width = 500000
    shape.height = 300000
    shape.has_table = False
    shape.has_chart = False
    shape.shape_type = 14  # PLACEHOLDER
    return shape


def _make_table_shape(headers: list[str], rows: list[list[str]]) -> MagicMock:
    shape = MagicMock()
    shape.has_text_frame = False
    shape.has_table = True
    shape.has_chart = False
    shape.shape_type = 19  # TABLE
    shape.name = "Table 1"
    shape.left = 0
    shape.top = 0

    table = MagicMock()

    all_rows = [headers] + rows
    mock_rows = []
    for row_cells in all_rows:
        mock_row = MagicMock()
        cells = []
        for cell_text in row_cells:
            cell = MagicMock()
            cell.text = cell_text
            cells.append(cell)
        mock_row.cells = cells
        mock_rows.append(mock_row)

    table.rows = mock_rows
    shape.table = table
    return shape


def _make_slide(
    title: str | None = None,
    shapes: list[MagicMock] | None = None,
    layout_name: str = "Title Slide",
    notes_text: str = "",
) -> MagicMock:
    slide = MagicMock()
    slide.slide_layout.name = layout_name

    all_shapes = list(shapes or [])

    if title is not None:
        title_shape = _make_placeholder(0, title)
        all_shapes.insert(0, title_shape)
        slide.shapes.title = title_shape
        slide.placeholders = [title_shape]
    else:
        slide.shapes.title = None
        slide.placeholders = []

    slide.shapes.__iter__ = lambda self: iter(all_shapes)

    if notes_text:
        slide.has_notes_slide = True
        slide.notes_slide.notes_text_frame.text = notes_text
    else:
        slide.has_notes_slide = False

    return slide


def _make_presentation(slides: list[MagicMock]) -> MagicMock:
    prs = MagicMock()
    prs.slides = slides
    prs.slides.__len__ = lambda self: len(slides)
    prs.slides.__getitem__ = lambda self, idx: slides[idx]
    prs.core_properties.author = "Test Author"
    prs.core_properties.title = "Test Title"
    prs.core_properties.subject = "Test Subject"
    prs.core_properties.keywords = ""
    prs.core_properties.comments = ""
    prs.core_properties.category = ""
    prs.core_properties.last_modified_by = ""
    prs.core_properties.created = None
    prs.core_properties.modified = None
    prs.slide_width = 9144000
    prs.slide_height = 6858000
    return prs


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


class TestExtractStructure:
    """Tests for PptxParser.extract_structure."""

    def test_empty_presentation(self) -> None:
        prs = _make_presentation([])
        parser = PptxParser()
        structure = parser.extract_structure(prs)

        assert structure.total_slides == 0
        assert structure.slides == []

    def test_single_slide_with_title(self) -> None:
        slide = _make_slide(title="Introduction", layout_name="Title Slide")
        prs = _make_presentation([slide])
        parser = PptxParser()
        structure = parser.extract_structure(prs)

        assert structure.total_slides == 1
        assert len(structure.slides) == 1
        assert structure.slides[0].title == "Introduction"
        assert structure.slides[0].layout_name == "Title Slide"
        assert structure.slides[0].slide_index == 0

    def test_slide_with_table_detected(self) -> None:
        table_shape = _make_table_shape(["A", "B"], [["1", "2"]])
        slide = _make_slide(title="Data", shapes=[table_shape])
        prs = _make_presentation([slide])
        parser = PptxParser()
        structure = parser.extract_structure(prs)

        assert structure.slides[0].has_tables is True

    def test_slide_with_notes_detected(self) -> None:
        slide = _make_slide(title="Noted", notes_text="Speaker notes here")
        prs = _make_presentation([slide])
        parser = PptxParser()
        structure = parser.extract_structure(prs)

        assert structure.slides[0].has_notes is True

    def test_document_properties(self) -> None:
        prs = _make_presentation([_make_slide(title="P")])
        parser = PptxParser()
        structure = parser.extract_structure(prs)

        assert structure.document_properties.get("author") == "Test Author"
        assert structure.document_properties.get("title") == "Test Title"

    def test_multiple_slides(self) -> None:
        slides = [
            _make_slide(title="Slide 1"),
            _make_slide(title="Slide 2"),
            _make_slide(title="Slide 3"),
        ]
        prs = _make_presentation(slides)
        parser = PptxParser()
        structure = parser.extract_structure(prs)

        assert structure.total_slides == 3
        assert [s.slide_index for s in structure.slides] == [0, 1, 2]


class TestExtractSlideContent:
    """Tests for PptxParser.extract_slide_content."""

    def test_extract_text_blocks(self) -> None:
        text_shape = _make_placeholder(2, "Body text here")
        slide = _make_slide(title="Title", shapes=[text_shape])
        prs = _make_presentation([slide])
        parser = PptxParser()
        content = parser.extract_slide_content(prs, 0)

        assert len(content.texts) >= 1
        body_texts = [t for t in content.texts if not t.is_title]
        assert any("Body text" in t.text for t in body_texts)

    def test_extract_tables(self) -> None:
        table_shape = _make_table_shape(
            headers=["Name", "Value"],
            rows=[["Alpha", "100"], ["Beta", "200"]],
        )
        slide = _make_slide(title="Data", shapes=[table_shape])
        prs = _make_presentation([slide])
        parser = PptxParser()
        content = parser.extract_slide_content(prs, 0)

        assert len(content.tables) == 1
        tbl = content.tables[0]
        assert tbl.headers == ["Name", "Value"]
        assert len(tbl.rows) == 2
        assert tbl.rows[0].cells == ["Alpha", "100"]
        assert tbl.confidence == 1.0

    def test_extract_notes(self) -> None:
        slide = _make_slide(title="Noted", notes_text="Important note")
        prs = _make_presentation([slide])
        parser = PptxParser()
        content = parser.extract_slide_content(prs, 0)

        assert content.notes == "Important note"

    def test_out_of_range_raises(self) -> None:
        prs = _make_presentation([_make_slide(title="Only")])
        parser = PptxParser()

        with pytest.raises(IndexError):
            parser.extract_slide_content(prs, 5)

    def test_negative_index_raises(self) -> None:
        prs = _make_presentation([_make_slide(title="Only")])
        parser = PptxParser()

        with pytest.raises(IndexError):
            parser.extract_slide_content(prs, -1)

    def test_empty_slide(self) -> None:
        slide = _make_slide(title=None)
        prs = _make_presentation([slide])
        parser = PptxParser()
        content = parser.extract_slide_content(prs, 0)

        assert content.texts == []
        assert content.tables == []
        assert content.notes == ""

    def test_title_is_flagged(self) -> None:
        slide = _make_slide(title="My Title")
        prs = _make_presentation([slide])
        parser = PptxParser()
        content = parser.extract_slide_content(prs, 0)

        title_blocks = [t for t in content.texts if t.is_title]
        assert len(title_blocks) == 1
        assert title_blocks[0].text == "My Title"

    def test_position_data_present(self) -> None:
        slide = _make_slide(title="Pos")
        prs = _make_presentation([slide])
        parser = PptxParser()
        content = parser.extract_slide_content(prs, 0)

        for t in content.texts:
            assert isinstance(t.position_x, int)
            assert isinstance(t.position_y, int)
