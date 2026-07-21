"""Optional PyQt6 renderers for the dependency-free BitSquiggles core.

Use this module as the application entry point. It re-exports the complete core
API and adds exact-raster and smooth QPixmap renderers.

Grug 2-Clause License: do what want; not sue grug.
"""

from PyQt6.QtCore import QRectF, Qt
from PyQt6.QtGui import QColor, QPainter, QPainterPath, QPixmap

import bitsquiggle32
from bitsquiggle32 import *

__all__ = bitsquiggle32.__all__ + ("render_raster", "render_smooth")


def _require_scale(scale):
    if not isinstance(scale, int) or scale < 1:
        raise ValueError("scale must be a positive integer")


def _pixmap(width, height, background):
    pixmap = QPixmap(width, height)
    pixmap.fill(QColor(background))
    return pixmap


def render_raster(grid, scale=1):
    """Return an exact integer-scaled QPixmap for a canonical pixel grid."""
    _require_scale(scale)
    width = grid["width"]
    height = grid["height"]
    pixel_data = grid["pixels"]
    if width != bitsquiggle32.PIXEL_WIDTH or height != bitsquiggle32.PIXEL_HEIGHT:
        raise ValueError("grid must use the canonical BitSquiggles raster dimensions")
    if len(pixel_data) != width * height:
        raise ValueError("grid has an invalid pixel buffer")

    pixmap = _pixmap(width * scale, height * scale, grid["background"]["hex"])
    painter = QPainter(pixmap)
    painter.setPen(Qt.PenStyle.NoPen)
    painter.setBrush(QColor(grid["foreground"]["hex"]))
    for index, pixel in enumerate(pixel_data):
        if pixel != 0 and pixel != 1:
            painter.end()
            raise ValueError("grid pixels must contain only zeroes and ones")
        if pixel:
            row, column = divmod(index, width)
            painter.drawRect(column * scale, row * scale, scale, scale)
    painter.end()
    return pixmap


def render_smooth(visual, scale=4):
    """Return an antialiased QPixmap for a canonical visual specification."""
    _require_scale(scale)
    width = bitsquiggle32.PIXEL_WIDTH * scale
    height = bitsquiggle32.PIXEL_HEIGHT * scale
    pixmap = QPixmap(width, height)
    pixmap.fill(Qt.GlobalColor.transparent)

    painter = QPainter(pixmap)
    painter.setRenderHint(QPainter.RenderHint.Antialiasing)
    painter.setPen(Qt.PenStyle.NoPen)
    painter.setBrush(QColor(visual["background"]["hex"]))
    painter.drawRoundedRect(QRectF(0, 0, width, height), scale, scale)

    foreground = QPainterPath()
    foreground.setFillRule(Qt.FillRule.WindingFill)
    for top, left, bottom, right in bitsquiggle32.smooth_blobs(visual["connections"]):
        x = (1 + 3 * left) * scale
        y = (1 + 3 * top) * scale
        blob_width = (2 + 3 * (right - left)) * scale
        blob_height = (2 + 3 * (bottom - top)) * scale
        foreground.addRoundedRect(
            QRectF(x, y, blob_width, blob_height), scale, scale
        )

    painter.setBrush(QColor(visual["foreground"]["hex"]))
    painter.drawPath(foreground)
    painter.end()
    return pixmap
