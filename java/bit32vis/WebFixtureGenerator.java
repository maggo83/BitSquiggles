// Bit32Vis — deterministic browser parity-fixture generator.
// Grug 2-Clause License
// 1. do what want
// 2. not sue grug

package bit32vis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Generates browser test vectors directly from the Java reference implementation. */
public final class WebFixtureGenerator {
    private static final Path OUTPUT = Path.of("web", "fixtures.json");

    private WebFixtureGenerator() {}

    /** Run with {@code --check} to reject stale committed browser fixtures. */
    public static void main(String[] args) throws IOException {
        boolean check = args.length == 1 && args[0].equals("--check");
        if (args.length > 1 || (args.length == 1 && !check)) {
            throw new IllegalArgumentException("usage: WebFixtureGenerator [--check]");
        }
        String expected = generate();
        if (check) {
            if (!Files.isRegularFile(OUTPUT)
                    || !Files.readString(OUTPUT, StandardCharsets.UTF_8).equals(expected)) {
                throw new IllegalStateException("stale browser parity fixture: " + OUTPUT);
            }
        } else {
            Files.createDirectories(OUTPUT.getParent());
            Files.writeString(OUTPUT, expected, StandardCharsets.UTF_8);
        }
    }

    private static String generate() {
        StringBuilder json = new StringBuilder(220_000);
        json.append("{\n  \"format\": 1,\n  \"vectors\": [\n");
        boolean first = true;
        for (int input = 0; input < 1_024; input++) {
            first = appendVector(json, input, first);
        }
        int[] notable = {0x12345678, 0x89ABCDEF, 0xD9ABCDEF, 0xFFFFFFFF, 0x80000000};
        for (int input : notable) first = appendVector(json, input, first);
        return json.append("\n  ]\n}\n").toString();
    }

    private static boolean appendVector(StringBuilder json, int input, boolean first) {
        if (!first) json.append(",\n");
        Bit32Vis.VisSpec standard = Bit32Vis.spec(input, Bit32Vis.Style.STANDARD);
        json.append("    {\"input\":\"").append(hex(input))
                .append("\",\"mixed\":\"").append(hex(standard.mixed()))
                .append("\",\"connections\":\"").append(bits(standard.connections()))
            .append("\",\"preferredMode\":\"").append(standard.preferredMode().label())
            .append("\",\"actualMode\":\"").append(standard.actualMode().label())
            .append("\",\"fallback\":").append(standard.fallback())
                .append(",\"styles\":{");
        for (int index = 0; index < Bit32Vis.Style.values().length; index++) {
            if (index != 0) json.append(',');
            Bit32Vis.Style style = Bit32Vis.Style.values()[index];
            Bit32Vis.VisSpec visual = Bit32Vis.spec(input, style);
            json.append('"').append(style.name().toLowerCase().replace('_', '-')).append("\":[\"")
                    .append(visual.background().hex()).append("\",\"")
                    .append(visual.foreground().hex()).append("\"]");
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