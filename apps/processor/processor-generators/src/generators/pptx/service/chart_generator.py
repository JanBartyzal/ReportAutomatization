"""Generate chart images using matplotlib for insertion into PPTX slides.

Handles ``{{CHART:metric_name}}`` placeholders by rendering charts as PNG
and inserting them at the placeholder shape position.
"""

from __future__ import annotations

import io
import logging
from dataclasses import dataclass, field
from typing import Final

import matplotlib
matplotlib.use("Agg")  # Non-interactive backend
import matplotlib.pyplot as plt

from src.common.config import CHART_DPI, CHART_HEIGHT_INCHES, CHART_WIDTH_INCHES

logger = logging.getLogger(__name__)

# Default color palette matching common corporate themes
_COLORS: Final[list[str]] = [
    "#4472C4", "#ED7D31", "#A5A5A5", "#FFC000",
    "#5B9BD5", "#70AD47", "#264478", "#9B57A0",
]


@dataclass(frozen=True, slots=True)
class ChartSeriesData:
    """A single data series for a chart."""
    name: str
    values: list[float]


@dataclass(frozen=True, slots=True)
class ChartData:
    """Complete chart specification."""
    chart_type: str  # BAR, LINE, PIE
    labels: list[str]
    series: list[ChartSeriesData] = field(default_factory=list)


def render_chart(data: ChartData) -> bytes:
    """Render a chart to PNG bytes.

    Args:
        data: Chart specification including type, labels, and series.

    Returns:
        PNG image bytes.

    Raises:
        ValueError: If chart_type is not supported.
    """
    chart_type = data.chart_type.upper()

    fig, ax = plt.subplots(figsize=(CHART_WIDTH_INCHES, CHART_HEIGHT_INCHES))

    try:
        if chart_type == "BAR":
            _render_bar(ax, data)
        elif chart_type == "LINE":
            _render_line(ax, data)
        elif chart_type == "PIE":
            _render_pie(ax, data)
        else:
            raise ValueError(f"Unsupported chart type: {chart_type}")

        fig.tight_layout()

        buf = io.BytesIO()
        fig.savefig(buf, format="png", dpi=CHART_DPI, bbox_inches="tight")
        buf.seek(0)
        png_bytes = buf.getvalue()

        logger.info("Rendered %s chart (%d bytes, %d series)", chart_type, len(png_bytes), len(data.series))
        return png_bytes
    finally:
        plt.close(fig)


def _render_bar(ax: plt.Axes, data: ChartData) -> None:
    """Render a grouped bar chart."""
    import numpy as np

    x = np.arange(len(data.labels))
    num_series = max(len(data.series), 1)
    width = 0.8 / num_series

    for idx, series in enumerate(data.series):
        offset = (idx - (num_series - 1) / 2) * width
        values = series.values[: len(data.labels)]
        ax.bar(x + offset, values, width, label=series.name, color=_COLORS[idx % len(_COLORS)])

    ax.set_xticks(x)
    ax.set_xticklabels(data.labels, rotation=45, ha="right")
    if len(data.series) > 1:
        ax.legend()
    ax.grid(axis="y", alpha=0.3)


def _render_line(ax: plt.Axes, data: ChartData) -> None:
    """Render a line chart."""
    for idx, series in enumerate(data.series):
        values = series.values[: len(data.labels)]
        ax.plot(
            data.labels,
            values,
            marker="o",
            label=series.name,
            color=_COLORS[idx % len(_COLORS)],
            linewidth=2,
        )

    ax.tick_params(axis="x", rotation=45)
    if len(data.series) > 1:
        ax.legend()
    ax.grid(alpha=0.3)


def _render_pie(ax: plt.Axes, data: ChartData) -> None:
    """Render a pie chart using the first series."""
    if not data.series:
        ax.text(0.5, 0.5, "No data", ha="center", va="center")
        return

    values = data.series[0].values[: len(data.labels)]
    colors = _COLORS[: len(values)]

    ax.pie(
        values,
        labels=data.labels,
        colors=colors,
        autopct="%1.1f%%",
        startangle=90,
    )
    ax.set_aspect("equal")
