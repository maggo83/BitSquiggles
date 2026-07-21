"""Focused tests for the optional PyQt6 BitSquiggles renderer."""

import os
import unittest

os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")

from PyQt6.QtGui import QGuiApplication

import bitsquiggle32
import bitsquiggles_renderer_pyqt6 as renderer


class PyQt6RendererTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.application = QGuiApplication.instance() or QGuiApplication([])

    def test_complete_core_facade(self):
        self.assertEqual(
            renderer.__all__,
            bitsquiggle32.__all__ + ("render_raster", "render_smooth"),
        )
        for name in bitsquiggle32.__all__:
            self.assertIs(getattr(renderer, name), getattr(bitsquiggle32, name))

    def test_exact_raster(self):
        grid = renderer.pixels(0x836DA7F8, renderer.HIGH_CONTRAST)
        image = renderer.render_raster(grid, scale=2).toImage()
        self.assertEqual((image.width(), image.height()), (32, 44))
        for row in range(renderer.PIXEL_HEIGHT):
            for column in range(renderer.PIXEL_WIDTH):
                expected = grid["foreground" if grid["pixels"][row * 16 + column] else "background"]["hex"]
                self.assertEqual(image.pixelColor(column * 2, row * 2).name(), expected)

    def test_smooth_rendering(self):
        visual = renderer.spec(0x836DA7F8, renderer.HIGH_CONTRAST)
        image = renderer.render_smooth(visual, scale=4).toImage()
        self.assertEqual((image.width(), image.height()), (64, 88))
        self.assertEqual(image.pixelColor(0, 0).alpha(), 0)
        self.assertGreater(image.pixelColor(32, 44).alpha(), 0)


if __name__ == "__main__":
    unittest.main()
