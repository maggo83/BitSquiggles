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
        for (BitSquiggle32.SmoothBlob blob : BitSquiggle32.extractSmoothBlobs(
                spec.connections())) {
            double x = offsetX + (1 + blob.leftColumn() * 3) * scale;
            double y = offsetY + (1 + blob.topRow() * 3) * scale;
            double blobWidth = (2 + 3 * (blob.rightColumn() - blob.leftColumn())) * scale;
            double blobHeight = (2 + 3 * (blob.bottomRow() - blob.topRow())) * scale;
            graphics.fillRoundRect(x, y, blobWidth, blobHeight, 2.0 * scale, 2.0 * scale);
        }
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
