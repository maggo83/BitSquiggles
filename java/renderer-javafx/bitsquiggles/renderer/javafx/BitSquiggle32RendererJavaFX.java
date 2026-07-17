// BitSquiggles — optional JavaFX renderer.
//
// Grug 2-Clause License
// 1. do what want
// 2. not sue grug

package bitsquiggles.renderer.javafx;

import bitsquiggles.BitSquiggle32;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Optional JavaFX renderer for {@link BitSquiggle32.VisSpec}. */
public final class BitSquiggle32RendererJavaFX {

    private BitSquiggle32RendererJavaFX() {}

    /**
     * Paint a smooth presentation using the canonical 16×22 coordinate space.
     * This method does not alter the canonical connection mask or exact raster.
     */
    public static void renderSmooth(
            GraphicsContext graphics, BitSquiggle32.VisSpec spec, double width, double height) {
        if (width <= 0 || height <= 0) return;

        double scale = Math.min(width / BitSquiggle32.PIXEL_WIDTH,
                height / BitSquiggle32.PIXEL_HEIGHT);
        double scaledWidth = BitSquiggle32.PIXEL_WIDTH * scale;
        double scaledHeight = BitSquiggle32.PIXEL_HEIGHT * scale;
        double offsetX = (width - scaledWidth) / 2.0;
        double offsetY = (height - scaledHeight) / 2.0;

        graphics.setFill(parseHex(spec.background().hex()));
        graphics.fillRoundRect(offsetX, offsetY, scaledWidth, scaledHeight,
                2.0 * scale, 2.0 * scale);

        graphics.setFill(parseHex(spec.foreground().hex()));
        double cellSize = 2.0 * scale;
        double bridgeGap = scale;
        // Extend each bridge a whole canonical pixel into both endpoint cells.
        // The redundant foreground coverage prevents Canvas antialiasing from
        // leaving a background hairline between separately painted primitives.
        double overlap = scale;

        BitSquiggle32.Edge[] edges = BitSquiggle32.edges();
        for (int index = 0; index < edges.length; index++) {
            if (spec.connections()[index] == 0) continue;
            BitSquiggle32.Edge edge = edges[index];
            double x = offsetX + (1 + edge.startColumn() * 3) * scale;
            double y = offsetY + (1 + edge.startRow() * 3) * scale;
            if (edge.startRow() == edge.endRow()) {
                graphics.fillRect(x + cellSize - overlap, y,
                        bridgeGap + 2.0 * overlap, cellSize);
            } else {
                graphics.fillRect(x, y + cellSize - overlap,
                        cellSize, bridgeGap + 2.0 * overlap);
            }
        }

        for (int row = 0; row < BitSquiggle32.ROWS - 1; row++) {
            for (int column = 0; column < BitSquiggle32.COLUMNS - 1; column++) {
                if (edgeValue(spec, row, column, row, column + 1) == 1
                        && edgeValue(spec, row + 1, column, row + 1, column + 1) == 1
                        && edgeValue(spec, row, column, row + 1, column) == 1
                        && edgeValue(spec, row, column + 1, row + 1, column + 1) == 1) {
                    graphics.fillRect(offsetX + (3 + column * 3) * scale,
                            offsetY + (3 + row * 3) * scale, scale, scale);
                }
            }
        }

        for (int row = 0; row < BitSquiggle32.ROWS; row++) {
            for (int column = 0; column < BitSquiggle32.COLUMNS; column++) {
                if (spec.cells()[row][column] == 0) continue;
                double x = offsetX + (1 + column * 3) * scale;
                double y = offsetY + (1 + row * 3) * scale;
                graphics.fillRoundRect(x, y, cellSize, cellSize, cellSize, cellSize);
            }
        }
    }

    private static int edgeValue(
            BitSquiggle32.VisSpec spec, int startRow, int startColumn,
            int endRow, int endColumn) {
        BitSquiggle32.Edge[] edges = BitSquiggle32.edges();
        for (int index = 0; index < edges.length; index++) {
            BitSquiggle32.Edge edge = edges[index];
            if (edge.startRow() == startRow && edge.startColumn() == startColumn
                    && edge.endRow() == endRow && edge.endColumn() == endColumn) {
                return spec.connections()[index];
            }
        }
        return 0;
    }

    /** Paint an exact pixel grid using whole {@code pixelSize}-square target pixels. */
    public static void renderRaster(
            GraphicsContext graphics, BitSquiggle32.PixelGrid grid, int pixelSize) {
        if (pixelSize <= 0) {
            throw new IllegalArgumentException("pixelSize must be positive");
        }

        int width = grid.width();
        int height = grid.height();
        byte[] pixels = grid.pixels();
        graphics.setFill(parseHex(grid.background().hex()));
        graphics.fillRect(0, 0, width * pixelSize, height * pixelSize);
        graphics.setFill(parseHex(grid.foreground().hex()));
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                if (pixels[row * width + column] != 0) {
                    graphics.fillRect(column * pixelSize, row * pixelSize, pixelSize, pixelSize);
                }
            }
        }
    }

    private static Color parseHex(String hex) {
        return Color.web(hex);
    }
}
