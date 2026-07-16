// Bit32Vis — injective connection-graph visualization of a 32-bit value.
//
// Grug 2-Clause License
// 1. do what want
// 2. not sue grug

package bit32vis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Bit32Vis {

    private Bit32Vis() {}

    public static final int ROWS = 7;
    public static final int COLUMNS = 5;
    public static final int EDGE_COUNT = 58;
    public static final int PIXEL_WIDTH = 16;
    public static final int PIXEL_HEIGHT = 22;

    public enum Style { STANDARD, HIGH_CONTRAST, MONOCHROME }

    public enum Mode {
        LEFT_RIGHT("A|"),
        TOP_BOTTOM("A-"),
        HALF_TURN("A+"),
        DIAGONAL_SLASH("A/");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String label() { return label; }
    }

    public record Edge(int startRow, int startColumn, int endRow, int endColumn) {}

    public record OklchColor(double L, double C, double h, String hex) {}

    /**
     * Canonical visual identity. Connections use the lexical order returned by
     * {@link #edges()}; cells are active exactly when incident to a connection.
     */
    public record VisSpec(
            int input,
            int mixed,
            byte[] connections,
            int[][] cells,
            OklchColor background,
            OklchColor foreground,
            Style style,
            Mode preferredMode,
            Mode actualMode,
            boolean fallback,
            int luminanceIndex,
            boolean swapped) {}

    public record PixelGrid(
            int width,
            int height,
            byte[] pixels,
            OklchColor background,
            OklchColor foreground,
            Style style) {}

    private static final double BASE_L_MIN = 0.5;
    private static final double BASE_L_MAX = 0.7;
    private static final double CHROMA_MIN = 0.05;
    private static final double CHROMA_MAX = 0.25;
    private static final double HC_L_ADD = 0.30;
    private static final double BG_L_SPREAD = 0.50;
    private static final double BG_L_EXTRA_HC = 0.30;
    private static final double CHROMA_HC_ADD = 0.10;

    private static final String[][] LEFT_RIGHT_TEMPLATE = rows(
            "A B C B' A'",
            "D E F E' D'",
            "G H I H' G'",
            "J K L K' J'",
            "M N O N' M'",
            "P Q R Q' P'",
            "S T U T' S'");

    private static final String[][] TOP_BOTTOM_TEMPLATE = rows(
            "A B C D E",
            "F G H I J",
            "K L M N O",
            "P Q R S T",
            "K' L' M' N' O'",
            "F' G' H' I' J'",
            "A' B' C' D' E'");

    private static final String[][] HALF_TURN_TEMPLATE = rows(
            "A B C D E",
            "F G H I J",
            "K L M N O",
            "P Q R Q' P'",
            "O' N' M' L' K'",
            "J' I' H' G' F'",
            "E' D' C' B' A'");

    private static final String[][] DIAGONAL_SLASH_TEMPLATE = rows(
            "A B C D E",
            "F G H I J",
            "K L M N I'",
            "O P Q M' H'",
            "R S P' L' A'",
            "T R' O' K' F'",
            "U V W X Y");

    private static final Edge[] EDGES = createEdges();
    private static final ModeDefinition[] MODE_DEFINITIONS = createModeDefinitions();

    private record Occurrence(int edgeIndex) {}
    private record ConnectionClass(int key, Occurrence[] occurrences) {}
    private record ModeDefinition(Mode mode, ConnectionClass[] classes) {}

    /** Return the 58 canonical edges in lexical `(i,j,k,l)` order. */
    public static Edge[] edges() {
        return EDGES.clone();
    }

    /** Number of independent connection variables in a copy mode. */
    public static int freeConnectionCount(Mode mode) {
        return MODE_DEFINITIONS[mode.ordinal()].classes().length;
    }

    /**
     * Bijective MurmurHash3 finalizer with a Weyl offset. Every unsigned 32-bit
     * input maps to a distinct output; multiplication constants are odd.
     */
    static int mix32(int value) {
        int mixed = value + 0x9E3779B9;
        mixed ^= mixed >>> 16;
        mixed *= 0x85EBCA6B;
        mixed ^= mixed >>> 13;
        mixed *= 0xC2B2AE35;
        mixed ^= mixed >>> 16;
        return mixed;
    }

    private static byte[] encodeConnections(int mixed, Mode preferredMode) {
        if (preferredMode == Mode.LEFT_RIGHT) return encodeDefault(mixed);

        ModeDefinition definition = MODE_DEFINITIONS[preferredMode.ordinal()];
        int payload = mixed & 0x3FFFFFFF;
        int spread = mix32(mixed ^ (0x6D2B79F5 * (preferredMode.ordinal() + 1)));
        byte[] values = new byte[definition.classes().length];
        for (int i = 0; i < values.length; i++) {
            values[i] = (byte) (i < 30
                    ? (payload >>> (29 - i)) & 1
                    : (spread >>> (31 - ((i - 30) & 31))) & 1);
        }
        return expand(definition, values);
    }

    private static boolean preferredModeHasCapacity(int mixed, Mode preferredMode) {
        int missingBits = 30 - MODE_DEFINITIONS[preferredMode.ordinal()].classes().length;
        if (missingBits <= 0) return true;
        int omittedMask = (1 << missingBits) - 1;
        return (mixed & omittedMask) == 0;
    }

    /** Default family is a one-to-one mapping of all 32 mixed bits. */
    private static byte[] encodeDefault(int mixed) {
        ModeDefinition definition = MODE_DEFINITIONS[Mode.LEFT_RIGHT.ordinal()];
        byte[] values = new byte[32];
        for (int i = 0; i < values.length; i++) {
            values[i] = (byte) ((mixed >>> (31 - i)) & 1);
        }
        return expand(definition, values);
    }

    private static byte[] expand(ModeDefinition definition, byte[] values) {
        byte[] result = new byte[EDGE_COUNT];
        for (int i = 0; i < definition.classes().length; i++) {
            for (Occurrence occurrence : definition.classes()[i].occurrences()) {
                result[occurrence.edgeIndex()] = values[i];
            }
        }
        return result;
    }

    /** Whether an edge mask belongs to the complete family represented by mode. */
    static boolean matchesMode(byte[] connections, Mode mode) {
        ModeDefinition definition = MODE_DEFINITIONS[mode.ordinal()];
        for (ConnectionClass connectionClass : definition.classes()) {
            Occurrence first = connectionClass.occurrences()[0];
            int expected = connections[first.edgeIndex()];
            for (int i = 1; i < connectionClass.occurrences().length; i++) {
                Occurrence occurrence = connectionClass.occurrences()[i];
                int actual = connections[occurrence.edgeIndex()];
                if (actual != expected) return false;
            }
        }
        return true;
    }

    private static boolean conflictsWithEarlierMode(byte[] connections, Mode mode) {
        for (int i = 0; i < mode.ordinal(); i++) {
            if (matchesMode(connections, Mode.values()[i])) return true;
        }
        return false;
    }

    private static int[][] activeCells(byte[] connections) {
        int[][] cells = new int[ROWS][COLUMNS];
        for (int i = 0; i < EDGES.length; i++) {
            if (connections[i] == 0) continue;
            Edge edge = EDGES[i];
            cells[edge.startRow()][edge.startColumn()] = 1;
            cells[edge.endRow()][edge.endColumn()] = 1;
        }
        return cells;
    }

    private static OklchColor[] deriveColors(
            int hueIndex, int chromaIndex, int luminanceIndex,
            boolean swap, Style style) {
        double hue = hueIndex * (360.0 / 16.0);
        double chroma = CHROMA_MIN
                + chromaIndex * ((CHROMA_MAX - CHROMA_MIN) / 15.0);
        double baseL = BASE_L_MIN
                + luminanceIndex * ((BASE_L_MAX - BASE_L_MIN) / 3.0);

        double foregroundL;
        double backgroundL;
        double foregroundC;
        double backgroundC;
        switch (style) {
            case STANDARD -> {
                foregroundL = baseL;
                backgroundL = foregroundL - BG_L_SPREAD;
                foregroundC = chroma;
                backgroundC = chroma;
            }
            case HIGH_CONTRAST -> {
                foregroundL = baseL + HC_L_ADD;
                backgroundL = foregroundL - BG_L_SPREAD - BG_L_EXTRA_HC;
                foregroundC = chroma + CHROMA_HC_ADD;
                backgroundC = chroma + CHROMA_HC_ADD;
            }
            case MONOCHROME -> {
                foregroundL = baseL + HC_L_ADD;
                backgroundL = foregroundL - BG_L_SPREAD - BG_L_EXTRA_HC;
                foregroundC = 0.0;
                backgroundC = 0.0;
            }
            default -> throw new IllegalStateException();
        }

        foregroundL = clamp01(foregroundL);
        backgroundL = clamp01(backgroundL);
        if (swap) {
            double temporary = foregroundL;
            foregroundL = backgroundL;
            backgroundL = temporary;
        }

        OklchColor foreground = makeColor(foregroundL, foregroundC, hue);
        OklchColor background = makeColor(
                backgroundL, backgroundC, (hue + 180.0) % 360.0);
        return new OklchColor[]{background, foreground};
    }

    private static double srgbEncode(double value) {
        value = clamp01(value);
        if (value <= 0.0031308) return 12.92 * value;
        return 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
    }

    private static OklchColor makeColor(double lightness, double chroma, double hue) {
        double radians = hue * Math.PI / 180.0;
        double a = chroma * Math.cos(radians);
        double b = chroma * Math.sin(radians);

        double l_ = lightness + 0.3963377774 * a + 0.2158037573 * b;
        double m_ = lightness - 0.1055613458 * a - 0.0638541728 * b;
        double s_ = lightness - 0.0894841775 * a - 1.2914855480 * b;
        double l3 = l_ * l_ * l_;
        double m3 = m_ * m_ * m_;
        double s3 = s_ * s_ * s_;

        double red = srgbEncode(
                4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3);
        double green = srgbEncode(
                -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3);
        double blue = srgbEncode(
                -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3);

        int redByte = Math.max(0, Math.min(255, (int) Math.round(red * 255)));
        int greenByte = Math.max(0, Math.min(255, (int) Math.round(green * 255)));
        int blueByte = Math.max(0, Math.min(255, (int) Math.round(blue * 255)));
        String hex = String.format("#%02x%02x%02x", redByte, greenByte, blueByte);
        return new OklchColor(lightness, chroma, hue, hex);
    }

    private static double clamp01(double value) {
        return value < 0.0 ? 0.0 : Math.min(value, 1.0);
    }

    /** Generate the exact 16×22 one-bit raster, including a one-pixel border. */
    static byte[] generatePixels(byte[] connections) {
        byte[] pixels = new byte[PIXEL_WIDTH * PIXEL_HEIGHT];
        int[][] cells = activeCells(connections);

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                if (cells[row][column] == 0) continue;
                int x = 1 + column * 3;
                int y = 1 + row * 3;
                setPixel(pixels, x, y);
                setPixel(pixels, x + 1, y);
                setPixel(pixels, x, y + 1);
                setPixel(pixels, x + 1, y + 1);
            }
        }

        for (int i = 0; i < EDGES.length; i++) {
            if (connections[i] == 0) continue;
            Edge edge = EDGES[i];
            int x = 1 + edge.startColumn() * 3;
            int y = 1 + edge.startRow() * 3;
            if (edge.startRow() == edge.endRow()) {
                setPixel(pixels, x + 2, y);
                setPixel(pixels, x + 2, y + 1);
            } else {
                setPixel(pixels, x, y + 2);
                setPixel(pixels, x + 1, y + 2);
            }
        }

        for (int row = 0; row < ROWS - 1; row++) {
            for (int column = 0; column < COLUMNS - 1; column++) {
                if (connection(connections, row, column, row, column + 1) == 1
                        && connection(connections, row + 1, column, row + 1, column + 1) == 1
                        && connection(connections, row, column, row + 1, column) == 1
                        && connection(connections, row, column + 1, row + 1, column + 1) == 1) {
                    setPixel(pixels, 3 + column * 3, 3 + row * 3);
                }
            }
        }
        return pixels;
    }

    private static void setPixel(byte[] pixels, int x, int y) {
        pixels[y * PIXEL_WIDTH + x] = 1;
    }

    static int connection(byte[] connections, int r1, int c1, int r2, int c2) {
        for (int i = 0; i < EDGES.length; i++) {
            Edge edge = EDGES[i];
            if (edge.startRow() == r1 && edge.startColumn() == c1
                    && edge.endRow() == r2 && edge.endColumn() == c2) {
                return connections[i];
            }
        }
        throw new IllegalArgumentException("not a canonical edge");
    }

    public static VisSpec spec(int input, Style style) {
        int mixed = mix32(input);
        Mode preferredMode = Mode.values()[mixed >>> 30];
        byte[] candidate = encodeConnections(mixed, preferredMode);
        boolean fallback = preferredMode != Mode.LEFT_RIGHT
                && (!preferredModeHasCapacity(mixed, preferredMode)
                        || conflictsWithEarlierMode(candidate, preferredMode));
        Mode actualMode = fallback ? Mode.LEFT_RIGHT : preferredMode;
        byte[] connections = fallback ? encodeDefault(mixed) : candidate;

        int hueIndex = (mixed >>> 12) & 0xF;
        int chromaIndex = (mixed >>> 8) & 0xF;
        int luminanceIndex = mixed & 0x3;
        boolean swapped = (Integer.bitCount(input) & 1) == 1;
        OklchColor[] colors = deriveColors(
                hueIndex, chromaIndex, luminanceIndex, swapped, style);
        return new VisSpec(
                input, mixed, connections, activeCells(connections),
                colors[0], colors[1], style, preferredMode, actualMode,
                fallback, luminanceIndex, swapped);
    }

    public static VisSpec spec(int input) {
        return spec(input, Style.STANDARD);
    }

    public static PixelGrid pixels(int input, Style style) {
        VisSpec visual = spec(input, style);
        return new PixelGrid(
                PIXEL_WIDTH, PIXEL_HEIGHT, generatePixels(visual.connections()),
                visual.background(), visual.foreground(), style);
    }

    public static PixelGrid pixels(int input) {
        return pixels(input, Style.STANDARD);
    }

    private static String[][] rows(String... rows) {
        String[][] result = new String[rows.length][];
        for (int i = 0; i < rows.length; i++) result[i] = rows[i].split(" ");
        return result;
    }

    private static Edge[] createEdges() {
        List<Edge> result = new ArrayList<>(EDGE_COUNT);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                if (column + 1 < COLUMNS) {
                    result.add(new Edge(row, column, row, column + 1));
                }
                if (row + 1 < ROWS) {
                    result.add(new Edge(row, column, row + 1, column));
                }
            }
        }
        return result.toArray(Edge[]::new);
    }

    private static ModeDefinition[] createModeDefinitions() {
        return new ModeDefinition[]{
            createModeDefinition(Mode.LEFT_RIGHT, LEFT_RIGHT_TEMPLATE),
            createModeDefinition(Mode.TOP_BOTTOM, TOP_BOTTOM_TEMPLATE),
            createModeDefinition(Mode.HALF_TURN, HALF_TURN_TEMPLATE),
            createModeDefinition(Mode.DIAGONAL_SLASH, DIAGONAL_SLASH_TEMPLATE)
        };
    }

    private static ModeDefinition createModeDefinition(Mode mode, String[][] template) {
        String[][] references = new String[ROWS][COLUMNS];
        Map<String, Integer> sourcePositions = new HashMap<>();
        for (int row = 0; row < ROWS; row++) {
            if (template[row].length != COLUMNS) throw new IllegalStateException("bad template");
            for (int column = 0; column < COLUMNS; column++) {
                String token = template[row][column];
                boolean copied = token.endsWith("'");
                String name = copied ? token.substring(0, token.length() - 1) : token;
                references[row][column] = name;
                if (!copied) sourcePositions.put(name, row * COLUMNS + column);
            }
        }

        TreeMap<Integer, List<Occurrence>> classes = new TreeMap<>();
        for (int edgeIndex = 0; edgeIndex < EDGES.length; edgeIndex++) {
            Edge edge = EDGES[edgeIndex];
            String start = references[edge.startRow()][edge.startColumn()];
            String end = references[edge.endRow()][edge.endColumn()];
            Integer startSource = sourcePositions.get(start);
            Integer endSource = sourcePositions.get(end);
            if (startSource == null || endSource == null || startSource.equals(endSource)) {
                throw new IllegalStateException("invalid template edge in " + mode);
            }
            int first = Math.min(startSource, endSource);
            int second = Math.max(startSource, endSource);
            int key = first * 64 + second;
            classes.computeIfAbsent(key, ignored -> new ArrayList<>()).add(
                    new Occurrence(edgeIndex));
        }

        ConnectionClass[] result = new ConnectionClass[classes.size()];
        int index = 0;
        for (Map.Entry<Integer, List<Occurrence>> entry : classes.entrySet()) {
            result[index++] = new ConnectionClass(
                    entry.getKey(), entry.getValue().toArray(Occurrence[]::new));
        }
        return new ModeDefinition(mode, result);
    }
}
