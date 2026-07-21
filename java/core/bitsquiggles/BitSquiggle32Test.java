// Grug 2-Clause License
// 1. do what want
// 2. not sue grug

package bitsquiggles;

import bitsquiggles.renderer.swing.BitSquiggle32RendererSwing;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Dependency-free test harness. Run with: java -cp out bitsquiggles.BitSquiggle32Test */
public final class BitSquiggle32Test {
    private static int checks;

    private BitSquiggle32Test() {}

    public static void main(String[] args) {
        testCanonicalEdgesAndModeCapacities();
        testEveryModeAndCanonicalPriority();
        testInputBitAvalanche();
        testSampledMonochromeInjectivity();
        testBlackAndWhiteColorsAndInjectivity();
        testPixelRendererIsLossless();
        testColorsAndParity();
        testSlashWrapRegression();
        testGoldenVector();
        testDemoHexParsing();
        testSmoothBlobReferenceImplementation();
        testSwingRendererFacade();
        testSwingRenderer();
        System.out.println("BitSquiggles Java tests passed (" + checks + " checks)");
    }

    private static void testCanonicalEdgesAndModeCapacities() {
        BitSquiggle32.Edge[] edges = BitSquiggle32.edges();
        check(edges.length == 58, "58 canonical edges");
        String previous = "";
        for (BitSquiggle32.Edge edge : edges) {
            check(edge.endRow() == edge.startRow() || edge.endRow() == edge.startRow() + 1,
                    "edge row adjacency");
            check(edge.endColumn() == edge.startColumn()
                            || edge.endColumn() == edge.startColumn() + 1,
                    "edge column adjacency");
            check((edge.endRow() - edge.startRow())
                            + (edge.endColumn() - edge.startColumn()) == 1,
                    "orthogonal unit edge");
            String key = String.format("%d%d%d%d", edge.startRow(), edge.startColumn(),
                    edge.endRow(), edge.endColumn());
            check(previous.compareTo(key) < 0, "lexical edge order");
            previous = key;
        }

        int[] expected = {32, 31, 29, 33};
        for (int i = 0; i < expected.length; i++) {
            check(BitSquiggle32.freeConnectionCount(BitSquiggle32.Mode.values()[i]) == expected[i],
                    "free connection count for " + BitSquiggle32.Mode.values()[i]);
        }
    }

    private static void testEveryModeAndCanonicalPriority() {
        boolean[] seenPreferred = new boolean[BitSquiggle32.Mode.values().length];
        boolean sawFallback = false;
        boolean sawHalfTurnCapacityFallback = false;
        boolean sawAcceptedHalfTurn = false;
        for (int input = 0; input < 250_000; input++) {
            BitSquiggle32.VisSpec visual = BitSquiggle32.spec(input);
            int preferredIndex = visual.preferredMode().ordinal();
            seenPreferred[preferredIndex] = true;
            sawFallback |= visual.fallback();
            if (visual.preferredMode() == BitSquiggle32.Mode.HALF_TURN) {
                if ((visual.mixed() & 1) == 1) {
                    check(visual.fallback(), "half-turn omitted bit uses fallback");
                    sawHalfTurnCapacityFallback = true;
                } else if (!visual.fallback()) {
                    sawAcceptedHalfTurn = true;
                }
            }

            check(BitSquiggle32.matchesMode(visual.connections(), visual.actualMode()),
                    "visual matches actual mode");
            if (!visual.fallback()) {
                for (int earlier = 0; earlier < preferredIndex; earlier++) {
                    check(!BitSquiggle32.matchesMode(
                                    visual.connections(), BitSquiggle32.Mode.values()[earlier]),
                            "accepted mode is canonical");
                }
            } else {
                check(visual.actualMode() == BitSquiggle32.Mode.LEFT_RIGHT,
                        "fallback uses default mode");
            }
        }
        for (int mode = 0; mode < seenPreferred.length; mode++) {
            check(seenPreferred[mode], "preferred mode observed: " + mode);
        }
        check(sawFallback, "fallback observed");
        check(sawHalfTurnCapacityFallback, "half-turn capacity fallback observed");
        check(sawAcceptedHalfTurn, "injective half-turn candidate observed");
    }

    private static void testInputBitAvalanche() {
        byte[] base = BitSquiggle32.spec(0).connections();
        int totalDistance = 0;
        for (int bit = 0; bit < 32; bit++) {
            byte[] changed = BitSquiggle32.spec(1 << bit).connections();
            int distance = distance(base, changed);
            check(distance > 0, "input bit changes monochrome visual: " + bit);
            check(distance >= 20, "input bit has useful edge avalanche: " + bit);
            totalDistance += distance;
        }
        check(totalDistance / 32.0 >= 26.0, "average edge avalanche");
    }

    private static void testSampledMonochromeInjectivity() {
        Set<Long> masks = new HashSet<>();
        for (int input = 0; input < 500_000; input++) {
            BitSquiggle32.VisSpec visual = BitSquiggle32.spec(input, BitSquiggle32.Style.MONOCHROME);
            check(masks.add(pack(visual.connections())),
                    "unique sampled monochrome visual: " + input);
        }
    }

    private static void testBlackAndWhiteColorsAndInjectivity() {
        Set<String> bitmaps = new HashSet<>();
        for (int input = 0; input < 500_000; input++) {
            BitSquiggle32.VisSpec visual = BitSquiggle32.spec(
                    input, BitSquiggle32.Style.BLACK_AND_WHITE);
            BitSquiggle32.PixelGrid grid = BitSquiggle32.pixels(
                    input, BitSquiggle32.Style.BLACK_AND_WHITE);
            check(visual.foreground().hex().matches("#(?:000000|ffffff)"),
                    "black-and-white foreground is binary");
            check(visual.background().hex().matches("#(?:000000|ffffff)"),
                    "black-and-white background is binary");
            check(!visual.foreground().hex().equals(visual.background().hex()),
                    "black-and-white colors differ");
            check((visual.foreground().hex().equals("#000000")) == visual.swapped(),
                    "swap controls black-and-white foreground polarity");
            StringBuilder bitmap = new StringBuilder(grid.pixels().length);
            boolean foregroundIsWhite = visual.foreground().hex().equals("#ffffff");
            boolean backgroundIsWhite = visual.background().hex().equals("#ffffff");
            for (byte pixel : grid.pixels()) bitmap.append(pixel == 1
                    ? (foregroundIsWhite ? '1' : '0')
                    : (backgroundIsWhite ? '1' : '0'));
            check(bitmaps.add(bitmap.toString()), "unique sampled black-and-white bitmap: " + input);
        }
    }

    private static void testPixelRendererIsLossless() {
        for (int input = 0; input < 2_000; input++) {
            BitSquiggle32.VisSpec visual = BitSquiggle32.spec(input);
            BitSquiggle32.PixelGrid grid = BitSquiggle32.pixels(input);
            check(grid.width() == 16 && grid.height() == 22, "pixel dimensions");
            check(grid.pixels().length == 352, "pixel storage");

            for (int x = 0; x < grid.width(); x++) {
                check(grid.pixels()[x] == 0, "top border");
                check(grid.pixels()[(grid.height() - 1) * grid.width() + x] == 0,
                        "bottom border");
            }
            for (int y = 0; y < grid.height(); y++) {
                check(grid.pixels()[y * grid.width()] == 0, "left border");
                check(grid.pixels()[y * grid.width() + grid.width() - 1] == 0,
                        "right border");
            }

            BitSquiggle32.Edge[] edges = BitSquiggle32.edges();
            for (int edgeIndex = 0; edgeIndex < edges.length; edgeIndex++) {
                BitSquiggle32.Edge edge = edges[edgeIndex];
                int x = 1 + edge.startColumn() * 3;
                int y = 1 + edge.startRow() * 3;
                int bridge = edge.startRow() == edge.endRow()
                        ? grid.pixels()[y * 16 + x + 2]
                        : grid.pixels()[(y + 2) * 16 + x];
                check(bridge == visual.connections()[edgeIndex],
                        "edge recoverable from pixel raster");
            }
        }
    }

    private static void testColorsAndParity() {
        int input = 0x89ABCDEF;
        BitSquiggle32.VisSpec standard = BitSquiggle32.spec(input, BitSquiggle32.Style.STANDARD);
        BitSquiggle32.VisSpec contrast = BitSquiggle32.spec(input, BitSquiggle32.Style.HIGH_CONTRAST);
        BitSquiggle32.VisSpec mono = BitSquiggle32.spec(input, BitSquiggle32.Style.MONOCHROME);
        BitSquiggle32.VisSpec blackAndWhite = BitSquiggle32.spec(input, BitSquiggle32.Style.BLACK_AND_WHITE);
        check(Arrays.equals(standard.connections(), contrast.connections()),
                "style preserves connections");
        check(Arrays.equals(standard.connections(), mono.connections()),
                "monochrome preserves unique connections");
        check(Arrays.equals(standard.connections(), blackAndWhite.connections()),
            "black-and-white preserves unique connections");
        check(contrast.foreground().C() > standard.foreground().C(),
                "high-contrast chroma");
        close(0.0, mono.foreground().C(), "monochrome foreground chroma");
        close(0.0, mono.background().C(), "monochrome background chroma");
        check(blackAndWhite.background().hex().matches("#(?:000000|ffffff)"),
                "black-and-white background is binary");
        check(blackAndWhite.foreground().hex().matches("#(?:000000|ffffff)"),
                "black-and-white foreground is binary");

        BitSquiggle32.VisSpec even = BitSquiggle32.spec(0);
        BitSquiggle32.VisSpec odd = BitSquiggle32.spec(1);
        check(!even.swapped(), "even parity is unswapped");
        check(odd.swapped(), "odd parity is swapped");
    }

    private static void testSlashWrapRegression() {
        BitSquiggle32.VisSpec visual = BitSquiggle32.spec(0xD9ABCDEF);
        check(visual.actualMode() == BitSquiggle32.Mode.DIAGONAL_SLASH,
                "slash wrap regression mode");
        check(BitSquiggle32.connection(visual.connections(), 1, 1, 2, 1)
                        == BitSquiggle32.connection(visual.connections(), 4, 3, 4, 4),
                "slash copy maps upper vertical to lower horizontal edge");
        check(BitSquiggle32.connection(visual.connections(), 1, 2, 2, 2)
                        == BitSquiggle32.connection(visual.connections(), 3, 3, 3, 4),
                "slash copy preserves adjacent diagonal edge relation");
    }

    private static void testGoldenVector() {
        BitSquiggle32.VisSpec visual = BitSquiggle32.spec(0x89ABCDEF, BitSquiggle32.Style.STANDARD);
        check(visual.mixed() == 0x47AC5876, "golden mixed value");
        check(visual.preferredMode() == BitSquiggle32.Mode.TOP_BOTTOM, "golden mode");
        check(!visual.fallback(), "golden no fallback");
        check(flatten(visual.connections()).equals(
                "0001111010110001011000011101010010101100001010011011010011"),
                "golden connections");
        check(visual.background().hex().equals("#140040"), "golden background");
        check(visual.foreground().hex().equals("#8d9200"), "golden foreground");
    }

    private static void testDemoHexParsing() {
        check(BitSquigglesDemo.parseBits("89abcdef") == 0x89ABCDEF, "parse lowercase hex");
        check(BitSquigglesDemo.parseBits("0xFFFFFFFF") == -1, "parse unsigned max with prefix");
        expectFailure(() -> BitSquigglesDemo.parseBits("123"), "reject short input");
        expectFailure(() -> BitSquigglesDemo.parseBits("zzzzzzzz"), "reject non-hex input");
    }

    private static void testSmoothBlobReferenceImplementation() {
        check(BitSquiggle32.extractSmoothBlobs(new byte[BitSquiggle32.EDGE_COUNT]).length == 0,
                "empty edge mask has no smooth blobs");

        byte[] singleEdge = connections(0, 0, 0, 1);
        BitSquiggle32.SmoothBlob[] singleBlobs = BitSquiggle32.extractSmoothBlobs(singleEdge);
        check(Arrays.equals(singleBlobs, new BitSquiggle32.SmoothBlob[]{
            new BitSquiggle32.SmoothBlob(0, 0, 0, 1)}),
                "one edge has one 1x2 blob");
        assertBlobCoverage(singleEdge, singleBlobs);

        byte[] row = connections(0, 0, 0, 1, 0, 1, 0, 2);
        BitSquiggle32.SmoothBlob[] rowBlobs = BitSquiggle32.extractSmoothBlobs(row);
        check(Arrays.equals(rowBlobs, new BitSquiggle32.SmoothBlob[]{
            new BitSquiggle32.SmoothBlob(0, 0, 0, 2)}),
                "connected row merges into one blob");
        assertBlobCoverage(row, rowBlobs);

        byte[] square = connections(
                0, 0, 0, 1,
                1, 0, 1, 1,
                0, 0, 1, 0,
                0, 1, 1, 1);
        BitSquiggle32.SmoothBlob[] squareBlobs = BitSquiggle32.extractSmoothBlobs(square);
        check(Arrays.equals(squareBlobs, new BitSquiggle32.SmoothBlob[]{
            new BitSquiggle32.SmoothBlob(0, 0, 1, 1)}),
                "four-edge junction merges into one 2x2 blob");
        assertBlobCoverage(square, squareBlobs);

        byte[] complete = new byte[BitSquiggle32.EDGE_COUNT];
        Arrays.fill(complete, (byte) 1);
        BitSquiggle32.SmoothBlob[] completeBlobs = BitSquiggle32.extractSmoothBlobs(complete);
        check(Arrays.equals(completeBlobs, new BitSquiggle32.SmoothBlob[]{
            new BitSquiggle32.SmoothBlob(0, 0, 6, 4)}),
                "complete grid merges into one blob");
        assertBlobCoverage(complete, completeBlobs);

        for (int input = 0; input < 2_000; input++) {
            byte[] mask = BitSquiggle32.spec(input).connections();
            BitSquiggle32.SmoothBlob[] blobs = BitSquiggle32.extractSmoothBlobs(mask);
            assertBlobCoverage(mask, blobs);
        }

        expectFailure(() -> BitSquiggle32.extractSmoothBlobs(new byte[57]),
                "reject short smooth edge mask");
        byte[] invalid = new byte[BitSquiggle32.EDGE_COUNT];
        invalid[0] = 2;
        expectFailure(() -> BitSquiggle32.extractSmoothBlobs(invalid),
                "reject non-binary smooth edge mask");
    }

    private static byte[] connections(int... endpoints) {
        byte[] result = new byte[BitSquiggle32.EDGE_COUNT];
        for (int i = 0; i < endpoints.length; i += 4) {
            for (int edgeIndex = 0; edgeIndex < BitSquiggle32.EDGE_COUNT; edgeIndex++) {
                BitSquiggle32.Edge edge = BitSquiggle32.edges()[edgeIndex];
                if (edge.startRow() == endpoints[i] && edge.startColumn() == endpoints[i + 1]
                        && edge.endRow() == endpoints[i + 2]
                        && edge.endColumn() == endpoints[i + 3]) {
                    result[edgeIndex] = 1;
                    break;
                }
            }
        }
        return result;
    }

        private static void assertBlobCoverage(
            byte[] connections, BitSquiggle32.SmoothBlob[] blobs) {
        long selectedEdges = 0;
        long activeCells = 0;
        BitSquiggle32.Edge[] edges = BitSquiggle32.edges();
        for (int edgeIndex = 0; edgeIndex < edges.length; edgeIndex++) {
            if (connections[edgeIndex] == 0) continue;
            selectedEdges |= 1L << edgeIndex;
            activeCells |= cellBit(edges[edgeIndex].startRow(), edges[edgeIndex].startColumn());
            activeCells |= cellBit(edges[edgeIndex].endRow(), edges[edgeIndex].endColumn());
        }

        long coveredEdges = 0;
        int requiredJunctions = 0;
        int coveredJunctions = 0;
        for (int row = 0; row < BitSquiggle32.ROWS - 1; row++) {
            for (int column = 0; column < BitSquiggle32.COLUMNS - 1; column++) {
                if (BitSquiggle32.connection(connections, row, column, row, column + 1) == 1
                        && BitSquiggle32.connection(
                                connections, row + 1, column, row + 1, column + 1) == 1
                        && BitSquiggle32.connection(connections, row, column, row + 1, column) == 1
                        && BitSquiggle32.connection(
                                connections, row, column + 1, row + 1, column + 1) == 1) {
                    requiredJunctions |= 1 << junctionIndex(row, column);
                }
            }
        }

        for (BitSquiggle32.SmoothBlob blob : blobs) {
            check(blob.topRow() >= 0 && blob.leftColumn() >= 0
                            && blob.bottomRow() < BitSquiggle32.ROWS
                            && blob.rightColumn() < BitSquiggle32.COLUMNS,
                    "blob coordinates are in range");
            for (int row = blob.topRow(); row <= blob.bottomRow(); row++) {
                for (int column = blob.leftColumn(); column <= blob.rightColumn(); column++) {
                    check((activeCells & cellBit(row, column)) != 0,
                            "blob contains active cells only");
                }
            }
            for (int edgeIndex = 0; edgeIndex < edges.length; edgeIndex++) {
                BitSquiggle32.Edge edge = edges[edgeIndex];
                if (inside(blob, edge)) {
                    check(connections[edgeIndex] == 1, "blob internal edge is selected");
                    coveredEdges |= 1L << edgeIndex;
                }
            }
            for (int row = blob.topRow(); row < blob.bottomRow(); row++) {
                for (int column = blob.leftColumn(); column < blob.rightColumn(); column++) {
                    coveredJunctions |= 1 << junctionIndex(row, column);
                }
            }
        }
        check(coveredEdges == selectedEdges, "blobs cover every selected edge");
        check((coveredJunctions & requiredJunctions) == requiredJunctions,
                "blobs cover every required junction");
    }

    private static boolean inside(BitSquiggle32.SmoothBlob blob, BitSquiggle32.Edge edge) {
        return edge.startRow() >= blob.topRow() && edge.startRow() <= blob.bottomRow()
                && edge.startColumn() >= blob.leftColumn() && edge.startColumn() <= blob.rightColumn()
                && edge.endRow() >= blob.topRow() && edge.endRow() <= blob.bottomRow()
                && edge.endColumn() >= blob.leftColumn() && edge.endColumn() <= blob.rightColumn();
    }

    private static long cellBit(int row, int column) {
        return 1L << (row * BitSquiggle32.COLUMNS + column);
    }

    private static int junctionIndex(int row, int column) {
        return row * (BitSquiggle32.COLUMNS - 1) + column;
    }

    private static void testSwingRenderer() {
        BufferedImage image = new BufferedImage(160, 220, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            BitSquiggle32RendererSwing.renderSmooth(
                    graphics, BitSquiggle32.spec(0x12345678), image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        check(image.getRGB(image.getWidth() / 2, 1) != 0, "Swing renderer paints background");

        BitSquiggle32.PixelGrid grid = BitSquiggle32.pixels(0x12345678);
        BufferedImage rasterImage = new BufferedImage(
                grid.width() * 4, grid.height() * 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D rasterGraphics = rasterImage.createGraphics();
        try {
            BitSquiggle32RendererSwing.renderRaster(rasterGraphics, grid, 4);
        } finally {
            rasterGraphics.dispose();
        }
        check(rasterImage.getRGB(0, 0) != 0, "Swing renderer paints exact raster background");
    }

        private static void testSwingRendererFacade() {
        int input = 0x89ABCDEF;
        check(BitSquiggle32RendererSwing.ROWS == BitSquiggle32.ROWS,
            "Swing facade rows");
        check(BitSquiggle32RendererSwing.COLUMNS == BitSquiggle32.COLUMNS,
            "Swing facade columns");
        check(BitSquiggle32RendererSwing.EDGE_COUNT == BitSquiggle32.EDGE_COUNT,
            "Swing facade edge count");
        check(BitSquiggle32RendererSwing.PIXEL_WIDTH == BitSquiggle32.PIXEL_WIDTH,
            "Swing facade pixel width");
        check(BitSquiggle32RendererSwing.PIXEL_HEIGHT == BitSquiggle32.PIXEL_HEIGHT,
            "Swing facade pixel height");
        check(BitSquiggle32RendererSwing.BLACK_AND_WHITE == BitSquiggle32.Style.BLACK_AND_WHITE,
            "Swing facade style");
        check(BitSquiggle32RendererSwing.DIAGONAL_SLASH == BitSquiggle32.Mode.DIAGONAL_SLASH,
            "Swing facade mode");
        check(BitSquiggle32RendererSwing.mix32(input) == BitSquiggle32.mix32(input),
            "Swing facade mixer");
        check(Arrays.equals(BitSquiggle32RendererSwing.edges(), BitSquiggle32.edges()),
            "Swing facade edges");
        check(BitSquiggle32RendererSwing.freeConnectionCount(
            BitSquiggle32RendererSwing.TOP_BOTTOM)
            == BitSquiggle32.freeConnectionCount(BitSquiggle32.Mode.TOP_BOTTOM),
            "Swing facade free connection count");
        BitSquiggle32.VisSpec visual = BitSquiggle32RendererSwing.spec(
            input, BitSquiggle32RendererSwing.BLACK_AND_WHITE);
        BitSquiggle32.PixelGrid grid = BitSquiggle32RendererSwing.pixels(
            input, BitSquiggle32RendererSwing.BLACK_AND_WHITE);
        check(Arrays.equals(visual.connections(), BitSquiggle32.spec(
            input, BitSquiggle32.Style.BLACK_AND_WHITE).connections()),
            "Swing facade spec");
        check(Arrays.equals(grid.pixels(), BitSquiggle32.pixels(
            input, BitSquiggle32.Style.BLACK_AND_WHITE).pixels()),
            "Swing facade pixels");
        check(BitSquiggle32RendererSwing.matchesMode(
            visual.connections(), visual.actualMode()), "Swing facade mode match");
        check(Arrays.equals(BitSquiggle32RendererSwing.extractSmoothBlobs(
            visual.connections()), BitSquiggle32.extractSmoothBlobs(visual.connections())),
            "Swing facade smooth blobs");
        }

    private static long pack(byte[] connections) {
        long result = 0;
        for (byte connection : connections) result = (result << 1) | connection;
        return result;
    }

    private static int distance(byte[] first, byte[] second) {
        int result = 0;
        for (int i = 0; i < first.length; i++) result += first[i] ^ second[i];
        return result;
    }

    private static String flatten(byte[] connections) {
        StringBuilder result = new StringBuilder(connections.length);
        for (byte connection : connections) result.append(connection);
        return result.toString();
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void close(double expected, double actual, String message) {
        check(Math.abs(expected - actual) < 1e-12,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void expectFailure(Runnable action, String message) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }
}
