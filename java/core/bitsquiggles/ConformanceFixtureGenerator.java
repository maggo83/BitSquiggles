// BitSquiggles — deterministic cross-language conformance-fixture generator.
// Grug 2-Clause License
// 1. do what want
// 2. not sue grug

package bitsquiggles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Generates versioned test vectors directly from the Java reference implementation. */
public final class ConformanceFixtureGenerator {
    private static final Path OUTPUT = Path.of("fixtures", "v1.json");

    private ConformanceFixtureGenerator() {}

    /** Run with {@code --check} to reject stale committed conformance fixtures. */
    public static void main(String[] args) throws IOException {
        boolean check = args.length == 1 && args[0].equals("--check");
        if (args.length > 1 || (args.length == 1 && !check)) {
            throw new IllegalArgumentException("usage: ConformanceFixtureGenerator [--check]");
        }
        String expected = generate();
        if (check) {
            if (!Files.isRegularFile(OUTPUT)
                    || !Files.readString(OUTPUT, StandardCharsets.UTF_8).equals(expected)) {
                throw new IllegalStateException("stale conformance fixture: " + OUTPUT);
            }
        } else {
            Files.createDirectories(OUTPUT.getParent());
            Files.writeString(OUTPUT, expected, StandardCharsets.UTF_8);
        }
    }

    private static String generate() {
        StringBuilder json = new StringBuilder(300_000);
        json.append("{\n  \"schema\": \"bitsquiggles-conformance\",\n  \"version\": 1,\n")
                .append("  \"dimensions\": {\"rows\": ").append(BitSquiggle32.ROWS)
                .append(", \"columns\": ").append(BitSquiggle32.COLUMNS)
                .append(", \"edges\": ").append(BitSquiggle32.EDGE_COUNT)
                .append(", \"pixelWidth\": ").append(BitSquiggle32.PIXEL_WIDTH)
                .append(", \"pixelHeight\": ").append(BitSquiggle32.PIXEL_HEIGHT).append("},\n")
                .append("  \"styles\": [\"standard\", \"high-contrast\", \"monochrome\", \"black-and-white\"],\n")
                .append("  \"vectors\": [\n");
        boolean first = true;
        for (int input : corpus()) first = appendVector(json, input, first);
        return json.append("\n  ]\n}\n").toString();
    }

    /**
     * A compact, deterministic sample of the full 32-bit domain. LinkedHashSet
     * preserves fixture order while removing values shared by multiple groups.
     */
    private static Set<Integer> corpus() {
        Set<Integer> inputs = new LinkedHashSet<>();
        for (int input = 0; input < 1_024; input++) inputs.add(input);

        for (int bit = 0; bit < 32; bit++) {
            int oneBit = 1 << bit;
            inputs.add(oneBit);
            inputs.add(~oneBit);
        }

        int[] recognizable = {
            0xAAAAAAAA, 0x55555555, 0x7FFFFFFF, 0x80000000,
            0x12345678, 0x89ABCDEF, 0xD9ABCDEF, 0xFFFFFFFF
        };
        for (int input : recognizable) inputs.add(input);

        int state = 0x6D2B79F5;
        for (int index = 0; index < 4_096; index++) {
            state = xorshift32(state);
            inputs.add(state);
        }
        return inputs;
    }

    private static int xorshift32(int value) {
        value ^= value << 13;
        value ^= value >>> 17;
        return value ^ (value << 5);
    }

    private static boolean appendVector(StringBuilder json, int input, boolean first) {
        if (!first) json.append(",\n");
        BitSquiggle32.VisSpec standard = BitSquiggle32.spec(input, BitSquiggle32.Style.STANDARD);
        BitSquiggle32.PixelGrid raster = BitSquiggle32.pixels(input, BitSquiggle32.Style.STANDARD);
        json.append("    {\"input\":\"").append(hex(input))
                .append("\",\"mixed\":\"").append(hex(standard.mixed()))
                .append("\",\"connections\":\"").append(bits(standard.connections()))
                .append("\",\"pixels\":\"").append(bits(raster.pixels()))
                .append("\",\"preferredMode\":\"").append(standard.preferredMode().label())
                .append("\",\"actualMode\":\"").append(standard.actualMode().label())
                .append("\",\"fallback\":").append(standard.fallback())
                .append(",\"styles\":{");
        for (int index = 0; index < BitSquiggle32.Style.values().length; index++) {
            if (index != 0) json.append(',');
            BitSquiggle32.Style style = BitSquiggle32.Style.values()[index];
            BitSquiggle32.VisSpec visual = BitSquiggle32.spec(input, style);
            json.append('"').append(style.name().toLowerCase().replace('_', '-')).append("\":{\"background\":\"")
                    .append(visual.background().hex()).append("\",\"foreground\":\"")
                    .append(visual.foreground().hex()).append("\"}");
        }
        json.append("}}");
        return false;
    }

    private static String hex(int value) {
        return String.format("%08x", value);
    }

    private static String bits(byte[] values) {
        StringBuilder bits = new StringBuilder(values.length);
        for (byte value : values) bits.append(value);
        return bits.toString();
    }
}
