"""Tests for placeholder_parser module."""

from __future__ import annotations

from unittest.mock import MagicMock


from src.generators.pptx.service.placeholder_parser import (
    PlaceholderType,
    extract_placeholders,
    parse_placeholders_from_text,
)


class TestParseFromText:
    """Test parse_placeholders_from_text."""

    def test_text_placeholder(self):
        results = parse_placeholders_from_text("Hello {{name}}, welcome!")
        assert len(results) == 1
        assert results[0] == ("name", PlaceholderType.TEXT, "{{name}}")

    def test_table_placeholder(self):
        results = parse_placeholders_from_text("{{TABLE:expenses}}")
        assert len(results) == 1
        assert results[0] == ("expenses", PlaceholderType.TABLE, "{{TABLE:expenses}}")

    def test_chart_placeholder(self):
        results = parse_placeholders_from_text("{{CHART:revenue}}")
        assert len(results) == 1
        assert results[0] == ("revenue", PlaceholderType.CHART, "{{CHART:revenue}}")

    def test_multiple_placeholders(self):
        text = "{{name}} has {{TABLE:data}} and {{CHART:graph}}"
        results = parse_placeholders_from_text(text)
        assert len(results) == 3
        types = [r[1] for r in results]
        assert types == [PlaceholderType.TEXT, PlaceholderType.TABLE, PlaceholderType.CHART]

    def test_no_placeholders(self):
        results = parse_placeholders_from_text("Hello world, no tags here!")
        assert results == []

    def test_placeholder_with_spaces(self):
        results = parse_placeholders_from_text("{{ company_name }}")
        assert len(results) == 1
        assert results[0][0] == "company_name"

    def test_placeholder_with_underscores(self):
        results = parse_placeholders_from_text("{{it_costs_czk}}")
        assert len(results) == 1
        assert results[0][0] == "it_costs_czk"


class TestExtractPlaceholders:
    """Test extract_placeholders with mocked Presentation."""

    def _make_shape(self, name: str, text: str, has_table: bool = False) -> MagicMock:
        shape = MagicMock()
        shape.name = name
        shape.has_text_frame = not has_table
        shape.has_table = has_table

        if not has_table:
            tf = MagicMock()
            tf.text = text
            shape.text_frame = tf
        else:
            cell = MagicMock()
            cell.text_frame.text = text
            row = MagicMock()
            row.cells = [cell]
            table = MagicMock()
            table.rows = [row]
            shape.table = table

        return shape

    def _make_slide(self, shapes: list[MagicMock]) -> MagicMock:
        slide = MagicMock()
        slide.shapes = shapes
        return slide

    def _make_presentation(self, slides: list[MagicMock]) -> MagicMock:
        prs = MagicMock()
        prs.slides = slides
        return prs

    def test_extract_text_placeholder(self):
        shape = self._make_shape("TextBox 1", "{{company_name}}")
        slide = self._make_slide([shape])
        prs = self._make_presentation([slide])

        result = extract_placeholders(prs)
        assert len(result) == 1
        assert result[0].key == "company_name"
        assert result[0].placeholder_type == PlaceholderType.TEXT
        assert result[0].slide_index == 0
        assert result[0].shape_name == "TextBox 1"

    def test_extract_table_placeholder_from_table_shape(self):
        shape = self._make_shape("Table 1", "{{TABLE:expenses}}", has_table=True)
        slide = self._make_slide([shape])
        prs = self._make_presentation([slide])

        result = extract_placeholders(prs)
        assert len(result) == 1
        assert result[0].key == "expenses"
        assert result[0].placeholder_type == PlaceholderType.TABLE

    def test_deduplicate_same_placeholder(self):
        shape = self._make_shape("Box", "{{name}} and {{name}} again")
        slide = self._make_slide([shape])
        prs = self._make_presentation([slide])

        result = extract_placeholders(prs)
        # Both occurrences have the same dedup key so only one should appear
        assert len(result) == 1

    def test_multiple_slides(self):
        shape1 = self._make_shape("Title", "{{title}}")
        shape2 = self._make_shape("Content", "{{CHART:revenue}}")
        slide1 = self._make_slide([shape1])
        slide2 = self._make_slide([shape2])
        prs = self._make_presentation([slide1, slide2])

        result = extract_placeholders(prs)
        assert len(result) == 2
        assert result[0].slide_index == 0
        assert result[1].slide_index == 1
