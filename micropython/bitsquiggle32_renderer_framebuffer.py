"""Optional exact-raster renderer for fill-rectangle framebuffer targets.

Use this module for a framebuffer target: call ``pixels`` to create the
canonical grid and ``render_raster`` to paint it. Targets need only expose
``fill_rect(x, y, width, height, color)``. Supply a ``color_mapper`` when the
target does not accept the core's ``#rrggbb`` colors directly, such as for a
one-bit or RGB565 framebuffer.

Grug 2-Clause License: do what want; not sue grug.
"""

import bitsquiggle32
from bitsquiggle32 import *

__all__ = bitsquiggle32.__all__ + ("render_raster",)


def _require_scale(scale):
    if not isinstance(scale, int) or scale < 1:
        raise ValueError("scale must be a positive integer")


def _require_grid(grid):
    try:
        width = grid["width"]
        height = grid["height"]
        pixel_data = grid["pixels"]
        background = grid["background"]["hex"]
        foreground = grid["foreground"]["hex"]
    except (KeyError, TypeError):
        raise ValueError("grid must be a BitSquiggles pixel grid")

    if (width != bitsquiggle32.PIXEL_WIDTH
            or height != bitsquiggle32.PIXEL_HEIGHT):
        raise ValueError("grid must use the canonical BitSquiggles raster dimensions")
    if len(pixel_data) != width * height:
        raise ValueError("grid has an invalid pixel buffer")
    for pixel in pixel_data:
        if pixel != 0 and pixel != 1:
            raise ValueError("grid pixels must contain only zeroes and ones")
    return width, height, pixel_data, background, foreground


def _identity(color):
    return color


def render_raster(target, grid, x=0, y=0, scale=1, color_mapper=None):
    """Paint an exact whole-pixel or integer-scaled BitSquiggles raster.

    ``grid`` must be the canonical output of ``bitsquiggle32.pixels``. Every
    source pixel becomes a ``scale`` by ``scale`` target rectangle; no
    interpolation, antialiasing, clipping, or geometry reconstruction occurs.
    """
    _require_scale(scale)
    if not callable(getattr(target, "fill_rect", None)):
        raise ValueError("target must provide fill_rect")
    if color_mapper is None:
        color_mapper = _identity
    if not callable(color_mapper):
        raise ValueError("color_mapper must be callable")

    width, height, pixel_data, background, foreground = _require_grid(grid)
    background = color_mapper(background)
    foreground = color_mapper(foreground)
    target.fill_rect(x, y, width * scale, height * scale, background)

    for row in range(height):
        for column in range(width):
            if pixel_data[row * width + column]:
                target.fill_rect(x + column * scale, y + row * scale,
                                 scale, scale, foreground)
