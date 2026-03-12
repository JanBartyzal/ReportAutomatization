"""Slide image rendering using LibreOffice headless with python-pptx fallback.

Renders individual slides as PNG images at configurable dimensions.
"""

from __future__ import annotations

import asyncio
import logging
import shutil
import tempfile
from pathlib import Path
from typing import TYPE_CHECKING, Final

if TYPE_CHECKING:
    from src.common.config import Settings

logger = logging.getLogger(__name__)

_LIBREOFFICE_TIMEOUT: Final[int] = 60  # seconds


class ImageRenderer:
    """Render PPTX slides to PNG images.

    Primary strategy: LibreOffice headless conversion.
    Fallback: basic python-pptx shape rendering with Pillow (limited fidelity).

    Usage::

        renderer = ImageRenderer(settings)
        png_bytes = await renderer.render_slide(pptx_path, slide_index=0)
    """

    def __init__(self, settings: "Settings") -> None:
        self._settings = settings
        self._libreoffice_available: bool | None = None

    async def render_slide(self, pptx_path: str | Path, slide_index: int) -> bytes:
        """Render a single slide as a PNG image.

        Args:
            pptx_path: Path to the local PPTX file.
            slide_index: Zero-based index of the slide to render.

        Returns:
            PNG image bytes.

        Raises:
            RuntimeError: If rendering fails with both strategies.
        """
        pptx_path = Path(pptx_path)

        if await self._is_libreoffice_available():
            try:
                return await self._render_with_libreoffice(pptx_path, slide_index)
            except Exception:
                logger.warning(
                    "LibreOffice rendering failed for slide %d, falling back to Pillow",
                    slide_index,
                    exc_info=True,
                )

        return self._render_fallback(pptx_path, slide_index)

    # -- LibreOffice rendering ---------------------------------------------

    async def _render_with_libreoffice(self, pptx_path: Path, slide_index: int) -> bytes:
        """Render via LibreOffice headless conversion."""
        with tempfile.TemporaryDirectory(prefix="pptx_render_") as tmpdir:
            cmd = [
                self._settings.libreoffice_bin,
                "--headless",
                "--norestore",
                "--convert-to", "png",
                "--outdir", tmpdir,
                str(pptx_path),
            ]

            logger.debug("Running LibreOffice: %s", " ".join(cmd))

            proc = await asyncio.create_subprocess_exec(
                *cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )
            stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout=_LIBREOFFICE_TIMEOUT)

            if proc.returncode != 0:
                raise RuntimeError(
                    f"LibreOffice exited with code {proc.returncode}: {stderr.decode(errors='replace')}"
                )

            tmpdir_path = Path(tmpdir)
            png_files = sorted(tmpdir_path.glob("*.png"))

            if not png_files:
                raise RuntimeError("LibreOffice produced no PNG output")

            if len(png_files) == 1:
                target = png_files[0]
            elif slide_index < len(png_files):
                target = png_files[slide_index]
            else:
                raise RuntimeError(
                    f"Slide index {slide_index} out of range; LibreOffice produced {len(png_files)} images"
                )

            raw_bytes = target.read_bytes()

            return self._resize_png(
                raw_bytes, self._settings.render_width, self._settings.render_height
            )

    # -- Fallback rendering ------------------------------------------------

    def _render_fallback(self, pptx_path: Path, slide_index: int) -> bytes:
        """Basic fallback rendering using Pillow."""
        from io import BytesIO

        from PIL import Image, ImageDraw, ImageFont
        from pptx import Presentation

        prs = Presentation(str(pptx_path))
        if slide_index < 0 or slide_index >= len(prs.slides):
            raise IndexError(f"Slide index {slide_index} out of range")

        slide = prs.slides[slide_index]
        title = ""
        if slide.shapes.title is not None:
            title = slide.shapes.title.text

        width = self._settings.render_width
        height = self._settings.render_height

        img = Image.new("RGB", (width, height), color=(255, 255, 255))
        draw = ImageDraw.Draw(img)

        label = f"Slide {slide_index + 1}"
        if title:
            label += f": {title}"

        try:
            font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 28)
        except (OSError, IOError):
            font = ImageFont.load_default()

        draw.text((40, 40), label, fill=(0, 0, 0), font=font)

        for shape in slide.shapes:
            if shape.left is None or shape.top is None:
                continue
            if shape.width is None or shape.height is None:
                continue

            slide_width = prs.slide_width or 9144000
            slide_height = prs.slide_height or 6858000

            x = int(shape.left / slide_width * width)
            y = int(shape.top / slide_height * height)
            w = int(shape.width / slide_width * width)
            h = int(shape.height / slide_height * height)

            draw.rectangle([x, y, x + w, y + h], outline=(200, 200, 200), width=1)

        buf = BytesIO()
        img.save(buf, format="PNG")
        return buf.getvalue()

    # -- Utilities ---------------------------------------------------------

    @staticmethod
    def _resize_png(data: bytes, width: int, height: int) -> bytes:
        """Resize PNG bytes to the target dimensions."""
        from io import BytesIO

        from PIL import Image

        img = Image.open(BytesIO(data))
        if img.size != (width, height):
            img = img.resize((width, height), Image.LANCZOS)

        buf = BytesIO()
        img.save(buf, format="PNG")
        return buf.getvalue()

    async def _is_libreoffice_available(self) -> bool:
        """Check whether LibreOffice is available on the system."""
        if self._libreoffice_available is not None:
            return self._libreoffice_available

        if shutil.which(self._settings.libreoffice_bin) is not None:
            self._libreoffice_available = True
        else:
            try:
                proc = await asyncio.create_subprocess_exec(
                    self._settings.libreoffice_bin, "--version",
                    stdout=asyncio.subprocess.PIPE,
                    stderr=asyncio.subprocess.PIPE,
                )
                await asyncio.wait_for(proc.communicate(), timeout=10)
                self._libreoffice_available = proc.returncode == 0
            except Exception:
                self._libreoffice_available = False

        if not self._libreoffice_available:
            logger.warning("LibreOffice not available; slide rendering will use fallback")

        return self._libreoffice_available
