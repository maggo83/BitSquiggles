# BitSquiggle32 for Java 17+

← Back to the [BitSquiggles project overview](../README.md).

## 1. Status and scope

This is the dependency-free Java 17+ reference implementation of BitSquiggle32.
It provides the canonical visual specification and exact $16\times22$ binary
raster. It does not derive fingerprints or make security decisions from them.

The public core is `bitsquiggles.BitSquiggle32`. The optional
`BitSquigglesSwingRenderer` renders smooth Java2D output, while
`BitSquigglesDemo` is a separate Swing application that wires controls, views,
and both renderers together.

## 2. Include / install

This port is currently source-only. Copy `module-info.java` and the
`bitsquiggles/` source directory into a Java 17+ project, or compile the source
alongside an application.

The Java module is `io.github.maggo83.bitsquiggles`. It has no third-party
runtime dependencies. The optional Swing renderer and demo use the standard JDK
`java.desktop` module. An application that needs only the core can include
`BitSquiggle32.java` without either optional class.

## 3. Create a BitSquiggle

Java accepts every unsigned 32-bit bit pattern through `int`. Parse external
hexadecimal values with a wider type, then cast to `int`:

```java
import bitsquiggles.BitSquiggle32;

int bits = (int) Long.parseLong("12345678", 16);
BitSquiggle32.VisSpec visual = BitSquiggle32.spec(bits);
BitSquiggle32.PixelGrid raster = BitSquiggle32.pixels(bits);

System.out.println(visual.actualMode().label());
System.out.println(raster.foreground().hex());
```

`spec()` returns the input, mixed value, connection mask, active cells, colors,
style, mode/fallback metadata, and polarity metadata. `pixels()` returns the
exact raster and its colors. Arrays held by the returned records are mutable;
treat them as immutable outputs or copy them before sharing them.

## 4. Render the exact raster

`raster.pixels()` is a row-major byte array. `0` means background and `1` means
foreground. Render every element as one whole pixel or an integer-scaled square:

```java
for (int y = 0; y < raster.height(); y++) {
    for (int x = 0; x < raster.width(); x++) {
        boolean on = raster.pixels()[y * raster.width() + x] != 0;
        // Fill one scale-by-scale rectangle in the foreground or background color.
    }
}
```

Do not antialias, interpolate, or alter raster pixels when exact comparison is
required.

## 5. Optional smooth rendering

Use the standalone `BitSquigglesSwingRenderer` when an application needs smooth
Java2D output:

```java
BitSquigglesSwingRenderer.paint(graphics, visual, width, height);
```

The renderer depends on Java2D but the `BitSquiggle32` core does not. The
separate `BitSquigglesDemo` application demonstrates renderer use alongside
input controls and exact raster views. Smooth rendering is a presentation
option only; the canonical identity is the connection mask and exact raster.

## 6. Test conformance

From the repository root:

```bash
mkdir -p out
javac -d out java/module-info.java java/bitsquiggles/*.java
java --module-path out --module io.github.maggo83.bitsquiggles/bitsquiggles.BitSquiggle32Test
java -cp out bitsquiggles.GalleryGenerator --check
java -cp out bitsquiggles.ConformanceFixtureGenerator --check
```

The last two commands verify generated README SVGs and the shared versioned
conformance fixture.

## 7. Package / release notes

Consume this port as source today: copy the core source into an application or
add the source module to its build. The core has no third-party runtime
dependency; the optional Java2D renderer and Swing demo use standard JDK APIs.

## 8. Limitations and compatibility

This implementation requires Java 17 or later because it uses records and
modern switch syntax. It accepts `int` bit patterns only; callers are
responsible for deriving and displaying any unsigned fingerprint correctly.
See the repository specification for the interoperable algorithm contract.

## 9. License

Grug 2-Clause License. See the repository-level [LICENSE](../LICENSE).
