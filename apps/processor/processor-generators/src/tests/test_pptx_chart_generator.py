"""Tests for PPTX chart_generator module."""

from __future__ import annotations

import pytest

from src.generators.pptx.service.chart_generator import ChartData, ChartSeriesData, render_chart


class TestRenderChart:
    """Test render_chart function."""

    def test_bar_chart(self):
        data = ChartData(
            chart_type="BAR",
            labels=["Q1", "Q2", "Q3", "Q4"],
            series=[
                ChartSeriesData(name="Revenue", values=[100.0, 150.0, 120.0, 180.0]),
            ],
        )
        png_bytes = render_chart(data)

        assert len(png_bytes) > 0
        # PNG magic bytes
        assert png_bytes[:8] == b"\x89PNG\r\n\x1a\n"

    def test_line_chart(self):
        data = ChartData(
            chart_type="LINE",
            labels=["Jan", "Feb", "Mar"],
            series=[
                ChartSeriesData(name="Sales", values=[10.0, 20.0, 15.0]),
                ChartSeriesData(name="Costs", values=[8.0, 12.0, 11.0]),
            ],
        )
        png_bytes = render_chart(data)
        assert png_bytes[:8] == b"\x89PNG\r\n\x1a\n"

    def test_pie_chart(self):
        data = ChartData(
            chart_type="PIE",
            labels=["IT", "HR", "Marketing"],
            series=[
                ChartSeriesData(name="Budget", values=[45.0, 25.0, 30.0]),
            ],
        )
        png_bytes = render_chart(data)
        assert png_bytes[:8] == b"\x89PNG\r\n\x1a\n"

    def test_pie_chart_no_series(self):
        data = ChartData(
            chart_type="PIE",
            labels=["A", "B"],
            series=[],
        )
        png_bytes = render_chart(data)
        assert len(png_bytes) > 0

    def test_unsupported_chart_type(self):
        data = ChartData(
            chart_type="RADAR",
            labels=["A"],
            series=[ChartSeriesData(name="S", values=[1.0])],
        )
        with pytest.raises(ValueError, match="Unsupported chart type"):
            render_chart(data)

    def test_multiple_series_bar(self):
        data = ChartData(
            chart_type="BAR",
            labels=["2023", "2024", "2025"],
            series=[
                ChartSeriesData(name="OPEX", values=[500.0, 480.0, 460.0]),
                ChartSeriesData(name="CAPEX", values=[200.0, 250.0, 220.0]),
            ],
        )
        png_bytes = render_chart(data)
        assert len(png_bytes) > 1000  # Reasonable PNG size
