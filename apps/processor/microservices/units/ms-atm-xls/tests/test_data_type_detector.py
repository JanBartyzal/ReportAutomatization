"""Unit tests for DataTypeDetector."""

from __future__ import annotations

import pytest

from src.service.data_type_detector import (
    TYPE_CURRENCY,
    TYPE_DATE,
    TYPE_NUMBER,
    TYPE_PERCENTAGE,
    TYPE_STRING,
    DataTypeDetector,
)


@pytest.fixture
def detector() -> DataTypeDetector:
    return DataTypeDetector()


# ---------------------------------------------------------------------------
# NUMBER detection
# ---------------------------------------------------------------------------


class TestNumberDetection:
    """Tests for NUMBER type detection."""

    def test_standard_integers(self, detector: DataTypeDetector) -> None:
        values = ["1", "2", "3", "100", "999"]
        assert detector.detect_column_type(values) == TYPE_NUMBER

    def test_standard_floats(self, detector: DataTypeDetector) -> None:
        values = ["1.5", "2.75", "3.14", "100.0"]
        assert detector.detect_column_type(values) == TYPE_NUMBER

    def test_negative_numbers(self, detector: DataTypeDetector) -> None:
        values = ["-1", "-2.5", "3", "-100"]
        assert detector.detect_column_type(values) == TYPE_NUMBER

    def test_czech_number_format(self, detector: DataTypeDetector) -> None:
        """Czech locale: space as thousands separator, comma as decimal."""
        values = ["1 234,56", "5 678,90", "100", "1 000"]
        assert detector.detect_column_type(values) == TYPE_NUMBER

    def test_czech_number_no_decimal(self, detector: DataTypeDetector) -> None:
        values = ["1 000", "2 500", "10 000", "100 000"]
        assert detector.detect_column_type(values) == TYPE_NUMBER


# ---------------------------------------------------------------------------
# PERCENTAGE detection
# ---------------------------------------------------------------------------


class TestPercentageDetection:
    """Tests for PERCENTAGE type detection."""

    def test_simple_percentages(self, detector: DataTypeDetector) -> None:
        values = ["10%", "20%", "30%", "50%"]
        assert detector.detect_column_type(values) == TYPE_PERCENTAGE

    def test_decimal_percentages(self, detector: DataTypeDetector) -> None:
        values = ["10.5%", "20.3%", "99.9%"]
        assert detector.detect_column_type(values) == TYPE_PERCENTAGE

    def test_czech_decimal_percentages(self, detector: DataTypeDetector) -> None:
        values = ["10,5%", "20,3%", "99,9%"]
        assert detector.detect_column_type(values) == TYPE_PERCENTAGE

    def test_percentage_with_space(self, detector: DataTypeDetector) -> None:
        values = ["10 %", "20 %", "30 %"]
        assert detector.detect_column_type(values) == TYPE_PERCENTAGE

    def test_negative_percentages(self, detector: DataTypeDetector) -> None:
        values = ["-10%", "-5.5%", "3%", "-1%"]
        assert detector.detect_column_type(values) == TYPE_PERCENTAGE


# ---------------------------------------------------------------------------
# CURRENCY detection
# ---------------------------------------------------------------------------


class TestCurrencyDetection:
    """Tests for CURRENCY type detection."""

    def test_dollar_sign(self, detector: DataTypeDetector) -> None:
        values = ["$100", "$200.50", "$1,000"]
        assert detector.detect_column_type(values) == TYPE_CURRENCY

    def test_euro_sign(self, detector: DataTypeDetector) -> None:
        values = ["€100", "€200.50", "€1,000"]
        assert detector.detect_column_type(values) == TYPE_CURRENCY

    def test_czk_suffix(self, detector: DataTypeDetector) -> None:
        values = ["100 CZK", "200 CZK", "1 000 CZK"]
        assert detector.detect_column_type(values) == TYPE_CURRENCY

    def test_kc_suffix(self, detector: DataTypeDetector) -> None:
        values = ["100 Kč", "200 Kč", "1 000 Kč"]
        assert detector.detect_column_type(values) == TYPE_CURRENCY

    def test_usd_suffix(self, detector: DataTypeDetector) -> None:
        values = ["100 USD", "200 USD", "1 000 USD"]
        assert detector.detect_column_type(values) == TYPE_CURRENCY

    def test_eur_suffix(self, detector: DataTypeDetector) -> None:
        values = ["100 EUR", "200 EUR", "1 000 EUR"]
        assert detector.detect_column_type(values) == TYPE_CURRENCY


# ---------------------------------------------------------------------------
# DATE detection
# ---------------------------------------------------------------------------


class TestDateDetection:
    """Tests for DATE type detection."""

    def test_iso_dates(self, detector: DataTypeDetector) -> None:
        values = ["2024-01-15", "2024-02-28", "2024-12-31"]
        assert detector.detect_column_type(values) == TYPE_DATE

    def test_european_dot_dates(self, detector: DataTypeDetector) -> None:
        values = ["15.01.2024", "28.02.2024", "31.12.2024"]
        assert detector.detect_column_type(values) == TYPE_DATE

    def test_slash_dates(self, detector: DataTypeDetector) -> None:
        values = ["15/01/2024", "28/02/2024", "31/12/2024"]
        assert detector.detect_column_type(values) == TYPE_DATE

    def test_short_year_dates(self, detector: DataTypeDetector) -> None:
        values = ["15.01.24", "28.02.24", "31.12.24"]
        assert detector.detect_column_type(values) == TYPE_DATE


# ---------------------------------------------------------------------------
# STRING detection (fallback)
# ---------------------------------------------------------------------------


class TestStringDetection:
    """Tests for STRING type detection (default fallback)."""

    def test_plain_text(self, detector: DataTypeDetector) -> None:
        values = ["Alice", "Bob", "Charlie"]
        assert detector.detect_column_type(values) == TYPE_STRING

    def test_mixed_types_below_threshold(self, detector: DataTypeDetector) -> None:
        """When no single type exceeds 60% threshold, return STRING."""
        values = ["Alice", "100", "2024-01-15", "50%", "Bob"]
        assert detector.detect_column_type(values) == TYPE_STRING

    def test_empty_values(self, detector: DataTypeDetector) -> None:
        values = ["", "", "", ""]
        assert detector.detect_column_type(values) == TYPE_STRING

    def test_empty_list(self, detector: DataTypeDetector) -> None:
        values: list[str] = []
        assert detector.detect_column_type(values) == TYPE_STRING

    def test_mostly_numbers_with_some_text(self, detector: DataTypeDetector) -> None:
        """Numbers above 60% threshold should still be detected as NUMBER."""
        values = ["100", "200", "300", "N/A"]
        assert detector.detect_column_type(values) == TYPE_NUMBER

    def test_too_many_non_numbers_returns_string(self, detector: DataTypeDetector) -> None:
        """Below 60% threshold, returns STRING."""
        values = ["100", "N/A", "TBD", "unknown", "300"]
        assert detector.detect_column_type(values) == TYPE_STRING
