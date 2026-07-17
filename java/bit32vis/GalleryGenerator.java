// Bit32Vis — deterministic README example gallery generator.
//
// Grug 2-Clause License
// 1. do what want
// 2. not sue grug

package bit32vis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Generates the checked-in SVG example sheets embedded by the README. */
public final class GalleryGenerator {
    private static final Path OUTPUT_DIRECTORY = Path.of("docs", "examples");
    private static final int SMOOTH_WIDTH = 80;
    private static final int SMOOTH_HEIGHT = 110;
    private static final int CARD_WIDTH = 96;
    private static final int SHEET_WIDTH = CARD_WIDTH * Bit32Vis.Style.values().length;
    private static final int SHEET_HEIGHT = 140;
    private static final int PIXEL_Y = 116;

    private record Example(int input, String fileName) {}

    private static final Example[] EXAMPLES = {
            new Example(0x00000000, "00000000"),
            new Example(0x00000001, "00000001"),
            new Example(0x00000003, "00000003"),
            new Example(0x00000004, "00000004"),
            new Example(0x12345678, "12345678"),
            new Example(0xFFFFFFFF, "ffffffff")
    };

    private GalleryGenerator() {}

    /** Run with {@code --check} to fail when committed gallery files are stale. */
    public static void main(String[] args) throws IOException {
        boolean check = args.length == 1 && args[0].equals("--check");
        if (args.length > 1 || (args.length == 1 && !check)) {
            throw new IllegalArgumentException("usage: GalleryGenerator [--check]");
        }

        List<Path> stale = new ArrayList<>();
        Set<Path> expectedFiles = new HashSet<>();
        for (Example example : EXAMPLES) {
            Path output = OUTPUT_DIRECTORY.resolve(example.fileName() + ".svg");
            expectedFiles.add(output);
            String expected = renderSheet(example.input());
            if (check) {
                if (!Files.isRegularFile(output)
                        || !Files.readString(output, StandardCharsets.UTF_8).equals(expected)) {
                    stale.add(output);
                }
            } else {
                Files.createDirectories(OUTPUT_DIRECTORY);
                Files.writeString(output, expected, StandardCharsets.UTF_8);
            }
        }

        if (Files.isDirectory(OUTPUT_DIRECTORY)) {
            try (var files = Files.list(OUTPUT_DIRECTORY)) {
                List<Path> obsolete = files
                        .filter(path -> path.getFileName().toString().endsWith(".svg"))
                        .filter(path -> !expectedFiles.contains(path))
                        .toList();
                if (check) {
                    stale.addAll(obsolete);
                } else {
                    for (Path path : obsolete) Files.delete(path);
                }
            }
        }

        if (!stale.isEmpty()) {
            throw new IllegalStateException("stale generated gallery files: " + stale);
        }
    }

    private static String renderSheet(int input) {
        StringBuilder svg = new StringBuilder(16_384);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(SHEET_WIDTH).append("\" height=\"").append(SHEET_HEIGHT)
                .append("\" viewBox=\"0 0 ").append(SHEET_WIDTH).append(' ')
                .append(SHEET_HEIGHT).append("\">\n");
        for (int index = 0; index < Bit32Vis.Style.values().length; index++) {
            Bit32Vis.Style style = Bit32Vis.Style.values()[index];
            Bit32Vis.VisSpec visual = Bit32Vis.spec(input, style);
            Bit32Vis.PixelGrid pixels = Bit32Vis.pixels(input, style);
            int x = index * CARD_WIDTH + 8;
            svg.append("  <g id=\"").append(style.name().toLowerCase())
                    .append("\">\n");
            appendSmooth(svg, visual, x, 0);
            appendPixels(svg, pixels, x + 32, PIXEL_Y);
            svg.append("  </g>\n");
        }
        return svg.append("</svg>\n").toString();
    }

    private static void appendSmooth(StringBuilder svg, Bit32Vis.VisSpec visual, int x, int y) {
        String foreground = visual.foreground().hex();
        svg.append("    <rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"").append(SMOOTH_WIDTH).append("\" height=\"")
                .append(SMOOTH_HEIGHT).append("\" rx=\"10\" fill=\"")
                .append(visual.background().hex()).append("\"/>\n");
        svg.append("    <g fill=\"").append(foreground).append("\">\n");
        int[][] cells = visual.cells();
        for (int row = 0; row < Bit32Vis.ROWS; row++) {
            for (int column = 0; column < Bit32Vis.COLUMNS; column++) {
                if (cells[row][column] == 0) continue;
                appendRect(svg, x + 5 + column * 15, y + 5 + row * 15, 10, 10, 5);
            }
        }

        byte[] connections = visual.connections();
        for (int index = 0; index < Bit32Vis.edges().length; index++) {
            if (connections[index] == 0) continue;
            Bit32Vis.Edge edge = Bit32Vis.edges()[index];
            int edgeX = x + 5 + edge.startColumn() * 15;
            int edgeY = y + 5 + edge.startRow() * 15;
            if (edge.startRow() == edge.endRow()) {
                appendRect(svg, edgeX + 5, edgeY, 15, 10, 0);
            } else {
                appendRect(svg, edgeX, edgeY + 5, 10, 15, 0);
            }
        }

        for (int row = 0; row < Bit32Vis.ROWS - 1; row++) {
            for (int column = 0; column < Bit32Vis.COLUMNS - 1; column++) {
                if (edge(visual, row, column, row, column + 1) == 1
                        && edge(visual, row + 1, column, row + 1, column + 1) == 1
                        && edge(visual, row, column, row + 1, column) == 1
                        && edge(visual, row, column + 1, row + 1, column + 1) == 1) {
                    appendRect(svg, x + 15 + column * 15, y + 15 + row * 15, 5, 5, 0);
                }
            }
        }
        svg.append("    </g>\n");
    }

    private static void appendPixels(StringBuilder svg, Bit32Vis.PixelGrid pixels, int x, int y) {
        svg.append("    <rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"16\" height=\"22\" fill=\"")
                .append(pixels.background().hex()).append("\"/>\n");
        svg.append("    <g fill=\"").append(pixels.foreground().hex()).append("\">\n");
        byte[] raster = pixels.pixels();
        for (int row = 0; row < pixels.height(); row++) {
            for (int column = 0; column < pixels.width(); column++) {
                if (raster[row * pixels.width() + column] == 1) {
                    appendRect(svg, x + column, y + row, 1, 1, 0);
                }
            }
        }
        svg.append("    </g>\n");
    }

    private static int edge(
            Bit32Vis.VisSpec visual, int startRow, int startColumn,
            int endRow, int endColumn) {
        Bit32Vis.Edge[] edges = Bit32Vis.edges();
        for (int index = 0; index < edges.length; index++) {
            Bit32Vis.Edge edge = edges[index];
            if (edge.startRow() == startRow && edge.startColumn() == startColumn
                    && edge.endRow() == endRow && edge.endColumn() == endColumn) {
                return visual.connections()[index];
            }
        }
        throw new IllegalArgumentException("not a canonical edge");
    }

    private static void appendRect(StringBuilder svg, int x, int y, int width, int height, int radius) {
        svg.append("      <rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"").append(width).append("\" height=\"")
                .append(height).append('"');
        if (radius != 0) svg.append(" rx=\"").append(radius).append('"');
        svg.append("/>\n");
    }
}