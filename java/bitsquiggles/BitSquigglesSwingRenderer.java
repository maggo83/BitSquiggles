// BitSquiggles — optional Swing smooth renderer.
//
// Grug 2-Clause License
// 1. do what want
// 2. not sue grug

package bitsquiggles;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** Optional Java2D presentation renderer for a {@link BitSquiggle32.VisSpec}. */
public final class BitSquigglesSwingRenderer {

    private BitSquigglesSwingRenderer() {}

    /**
     * Paint a smooth presentation using the canonical 16×22 coordinate space.
     * This method does not alter the canonical connection mask or exact raster.
     */
    public static void paint(
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
        for (int row = 0; row < BitSquiggle32.ROWS; row++) {
            for (int column = 0; column < BitSquiggle32.COLUMNS; column++) {
                if (spec.cells()[row][column] == 0) continue;
                double x = offsetX + (1 + column * 3) * scale;
                double y = offsetY + (1 + row * 3) * scale;
                foreground.add(new Area(new RoundRectangle2D.Double(
                        x, y, 2 * scale, 2 * scale, 2 * scale, 2 * scale)));
            }
        }

        BitSquiggle32.Edge[] edges = BitSquiggle32.edges();
        for (int index = 0; index < edges.length; index++) {
            if (spec.connections()[index] == 0) continue;
            BitSquiggle32.Edge edge = edges[index];
            double x = offsetX + (1 + edge.startColumn() * 3) * scale;
            double y = offsetY + (1 + edge.startRow() * 3) * scale;
            if (edge.startRow() == edge.endRow()) {
                foreground.add(new Area(new Rectangle2D.Double(x + scale, y, 3 * scale, 2 * scale)));
            } else {
                foreground.add(new Area(new Rectangle2D.Double(x, y + scale, 2 * scale, 3 * scale)));
            }
        }

        for (int row = 0; row < BitSquiggle32.ROWS - 1; row++) {
            for (int column = 0; column < BitSquiggle32.COLUMNS - 1; column++) {
                if (edgeValue(spec, row, column, row, column + 1) == 1
                        && edgeValue(spec, row + 1, column, row + 1, column + 1) == 1
                        && edgeValue(spec, row, column, row + 1, column) == 1
                        && edgeValue(spec, row, column, row + 1, column + 1) == 1) {
                    foreground.add(new Area(new Rectangle2D.Double(
                            offsetX + (3 + column * 3) * scale,
                            offsetY + (3 + row * 3) * scale,
                            scale, scale)));
                }
            }
        }

        graphics.setColor(parseHex(spec.foreground().hex()));
        graphics.fill(foreground);
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

    private static Color parseHex(String hex) {
        return new Color(
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16));
    }
}
