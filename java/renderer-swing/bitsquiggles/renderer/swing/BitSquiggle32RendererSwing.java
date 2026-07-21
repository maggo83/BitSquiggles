// BitSquiggles — optional Swing/Java2D renderer.
//
// Grug 2-Clause License
// 1. do what want
// 2. not sue grug

package bitsquiggles.renderer.swing;

import bitsquiggles.BitSquiggle32;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

/** Swing/Java2D BitSquiggle32 entry point. */
public final class BitSquiggle32RendererSwing {

    private BitSquiggle32RendererSwing() {}

    public static final int ROWS = BitSquiggle32.ROWS;
    public static final int COLUMNS = BitSquiggle32.COLUMNS;
    public static final int EDGE_COUNT = BitSquiggle32.EDGE_COUNT;
    public static final int PIXEL_WIDTH = BitSquiggle32.PIXEL_WIDTH;
    public static final int PIXEL_HEIGHT = BitSquiggle32.PIXEL_HEIGHT;

    public static final BitSquiggle32.Style STANDARD = BitSquiggle32.Style.STANDARD;
    public static final BitSquiggle32.Style HIGH_CONTRAST = BitSquiggle32.Style.HIGH_CONTRAST;
    public static final BitSquiggle32.Style MONOCHROME = BitSquiggle32.Style.MONOCHROME;
    public static final BitSquiggle32.Style BLACK_AND_WHITE = BitSquiggle32.Style.BLACK_AND_WHITE;

    public static final BitSquiggle32.Mode LEFT_RIGHT = BitSquiggle32.Mode.LEFT_RIGHT;
    public static final BitSquiggle32.Mode TOP_BOTTOM = BitSquiggle32.Mode.TOP_BOTTOM;
    public static final BitSquiggle32.Mode HALF_TURN = BitSquiggle32.Mode.HALF_TURN;
    public static final BitSquiggle32.Mode DIAGONAL_SLASH = BitSquiggle32.Mode.DIAGONAL_SLASH;

    public static BitSquiggle32.Edge[] edges() {
        return BitSquiggle32.edges();
    }

    public static int freeConnectionCount(BitSquiggle32.Mode mode) {
        return BitSquiggle32.freeConnectionCount(mode);
    }

    public static int mix32(int input) {
        return BitSquiggle32.mix32(input);
    }

    public static boolean matchesMode(byte[] connections, BitSquiggle32.Mode mode) {
        return BitSquiggle32.matchesMode(connections, mode);
    }

    public static BitSquiggle32.VisSpec spec(int input, BitSquiggle32.Style style) {
        return BitSquiggle32.spec(input, style);
    }

    public static BitSquiggle32.VisSpec spec(int input) {
        return BitSquiggle32.spec(input);
    }

    public static BitSquiggle32.PixelGrid pixels(int input, BitSquiggle32.Style style) {
        return BitSquiggle32.pixels(input, style);
    }

    public static BitSquiggle32.PixelGrid pixels(int input) {
        return BitSquiggle32.pixels(input);
    }

    public static BitSquiggle32.SmoothBlob[] extractSmoothBlobs(byte[] connections) {
        return BitSquiggle32.extractSmoothBlobs(connections);
    }

    /**
     * Paint a smooth presentation using the canonical 16×22 coordinate space.
     * This method does not alter the canonical connection mask or exact raster.
     */
    public static void renderSmooth(
            Graphics2D graphics, BitSquiggle32.VisSpec spec, int width, int height) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double scale = Math.min(width / 16.0, height / 22.0);
        double scaledWidth = 16 * scale;
        double scaledHeight = 22 * scale;
        double offsetX = (width - scaledWidth) / 2.0;
        double offsetY = (height - scaledHeight) / 2.0;

        graphics.setColor(parseHex(spec.background().hex()));
        graphics.fill(new RoundRectangle2D.Double(
                offsetX, offsetY, scaledWidth, scaledHeight, 2.0 * scale, 2.0 * scale));

        Area foreground = new Area();
        for (BitSquiggle32.SmoothBlob blob : BitSquiggle32.extractSmoothBlobs(
                spec.connections())) {
            double x = offsetX + (1 + blob.leftColumn() * 3) * scale;
            double y = offsetY + (1 + blob.topRow() * 3) * scale;
            double blobWidth = (2 + 3 * (blob.rightColumn() - blob.leftColumn())) * scale;
            double blobHeight = (2 + 3 * (blob.bottomRow() - blob.topRow())) * scale;
            foreground.add(new Area(new RoundRectangle2D.Double(
                    x, y, blobWidth, blobHeight, 2 * scale, 2 * scale)));
        }

        graphics.setColor(parseHex(spec.foreground().hex()));
        graphics.fill(foreground);
    }

    /** Paint an exact pixel grid using whole {@code pixelSize}-square target pixels. */
    public static void renderRaster(
            Graphics2D graphics, BitSquiggle32.PixelGrid grid, int pixelSize) {
        int width = grid.width();
        int height = grid.height();
        byte[] pixels = grid.pixels();
        graphics.setColor(parseHex(grid.background().hex()));
        graphics.fillRect(0, 0, width * pixelSize, height * pixelSize);
        graphics.setColor(parseHex(grid.foreground().hex()));
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                if (pixels[row * width + column] != 0) {
                    graphics.fillRect(column * pixelSize, row * pixelSize, pixelSize, pixelSize);
                }
            }
        }
    }

    private static Color parseHex(String hex) {
        return new Color(
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16));
    }
}
