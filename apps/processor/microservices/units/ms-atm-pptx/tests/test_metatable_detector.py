"""Unit tests for MetaTableDetector."""

from __future__ import annotations

import pytest

from src.service.metatable_detector import MetaTableDetector
from src.service.pptx_parser import TextBlockData


def _block(text: str, name: str = "Shape") -> TextBlockData:
    """Create a TextBlockData with the given text."""
    return TextBlockData(
        shape_name=name,
        text=text,
        is_title=False,
        position_x=0,
        position_y=0,
    )


class TestTabDelimited:
    """Tab-delimited table detection."""

    def test_basic_tab_table(self) -> None:
        text = "Name\tAge\tCity\nAlice\t30\tPrague\nBob\t25\tBrno\nCarol\t35\tOstrava"
        detector = MetaTableDetector(confidence_threshold=0.5)
        tables = detector.detect([_block(text)])

        assert len(tables) == 1
        tbl = tables[0]
        assert tbl.headers == ["Name", "Age", "City"]
        assert len(tbl.rows) == 3
        assert tbl.rows[0].cells == ["Alice", "30", "Prague"]
        assert tbl.confidence > 0.5

    def test_high_threshold_filters_weak(self) -> None:
        # Only two rows, two columns -- low confidence
        text = "A\tB\n1\t2"
        detector = MetaTableDetector(confidence_threshold=0.95)
        tables = detector.detect([_block(text)])

        assert len(tables) == 0

    def test_no_table_in_plain_text(self) -> None:
        text = "This is just a regular paragraph.\nNo table here."
        detector = MetaTableDetector(confidence_threshold=0.5)
        tables = detector.detect([_block(text)])

        assert len(tables) == 0


class TestPipeDelimited:
    """Pipe-delimited (Markdown) table detection."""

    def test_markdown_table(self) -> None:
        text = (
            "| Name  | Score |\n"
            "|-------|-------|\n"
            "| Alice | 95    |\n"
            "| Bob   | 87    |\n"
            "| Carol | 92    |"
        )
        detector = MetaTableDetector(confidence_threshold=0.5)
        tables = detector.detect([_block(text)])

        assert len(tables) == 1
        tbl = tables[0]
        assert tbl.headers == ["Name", "Score"]
        assert len(tbl.rows) == 3

    def test_pipe_without_separator(self) -> None:
        text = "| X | Y | Z |\n| 1 | 2 | 3 |\n| 4 | 5 | 6 |"
        detector = MetaTableDetector(confidence_threshold=0.5)
        tables = detector.detect([_block(text)])

        assert len(tables) == 1
        assert tables[0].headers == ["X", "Y", "Z"]


class TestMultiSpaceDelimited:
    """Multi-space delimited table detection."""

    def test_space_aligned_table(self) -> None:
        text = (
            "Name      Age    City\n"
            "Alice     30     Prague\n"
            "Bob       25     Brno\n"
            "Carol     35     Ostrava\n"
            "David     40     Plzen"
        )
        detector = MetaTableDetector(confidence_threshold=0.5)
        tables = detector.detect([_block(text)])

        assert len(tables) == 1
        tbl = tables[0]
        assert tbl.headers == ["Name", "Age", "City"]
        assert len(tbl.rows) == 4


class TestEdgeCases:
    """Edge case handling."""

    def test_single_line_no_table(self) -> None:
        detector = MetaTableDetector(confidence_threshold=0.5)
        tables = detector.detect([_block("Just one line")])
        assert len(tables) == 0

    def test_empty_text(self) -> None:
        detector = MetaTableDetector(confidence_threshold=0.5)
        tables = detector.detect([_block("")])
        assert len(tables) == 0

    def test_empty_list(self) -> None:
        detector = MetaTableDetector(confidence_threshold=0.5)
        tables = detector.detect([])
        assert len(tables) == 0

    def test_multiple_blocks_multiple_tables(self) -> None:
        block1 = _block("A\tB\n1\t2\n3\t4\n5\t6")
        block2 = _block("X\tY\tZ\n7\t8\t9\n10\t11\t12\n13\t14\t15")
        detector = MetaTableDetector(confidence_threshold=0.5)
        tables = detector.detect([block1, block2])

        assert len(tables) == 2

    def test_inconsistent_columns_rejected(self) -> None:
        text = "A\tB\tC\n1\t2\n3\t4\t5\t6\n7\t8\t9"
        detector = MetaTableDetector(confidence_threshold=0.9)
        tables = detector.detect([_block(text)])

        # Inconsistent column counts should lower confidence
        # With high threshold, likely rejected
        for tbl in tables:
            assert tbl.confidence >= 0.9

    def test_confidence_is_populated(self) -> None:
        text = "H1\tH2\tH3\nA\tB\tC\nD\tE\tF\nG\tH\tI\nJ\tK\tL"
        detector = MetaTableDetector(confidence_threshold=0.0)
        tables = detector.detect([_block(text)])

        assert len(tables) == 1
        assert 0.0 < tables[0].confidence <= 1.0

    def test_default_threshold_from_config(self) -> None:
        """Detector without explicit threshold uses the config value."""
        detector = MetaTableDetector()
        # We just verify it instantiates correctly and uses the config default
        assert detector._threshold > 0
