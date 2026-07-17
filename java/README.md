# BitSquiggle32 for Java 17+

← Back to the [BitSquiggles project overview](../README.md). Read the
[normative specification](../SPEC.md) for behavior shared by every port.

## 1. Status and scope

This guide explains how to integrate the Java 17+ reference implementation.
Project purpose, safety boundaries, and release status live in the
[project overview](../README.md); the shared encoding contract lives in the
[specification](../SPEC.md).

The public core is `bitsquiggles.BitSquiggle32`. The optional
`BitSquiggle32RendererSwing` and `BitSquiggle32RendererJavaFX` render
Swing/Java2D and JavaFX output respectively, while `BitSquigglesDemo` is a
separate Swing application that wires controls, views, and both renderers
together.

## 2. Include / install

This port is currently source-only. For a dependency-free JPMS core, compile
`core/module-info.java` with `bitsquiggles/BitSquiggle32.java`. Copy that core
source into an application when JPMS is not required.

The Java module is `io.github.maggo83.bitsquiggles`. It has no third-party
or desktop dependency. The optional
`io.github.maggo83.bitsquiggles.renderer.swing` module and Swing demo use the
standard JDK `java.desktop` module. The optional
`io.github.maggo83.bitsquiggles.renderer.javafx` module requires JavaFX
`javafx.graphics`.

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

Both methods also accept an optional `BitSquiggle32.Style`; the one-argument
overloads use `STANDARD`. The shared output contract and every other public
core symbol are defined in the [Java API mapping](../SPEC.md#121-java-17).
Arrays held by the returned records are mutable; treat them as immutable outputs
or copy them before sharing them.

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

The exact dimensions and rendering rules are defined in the
[normative raster specification](../SPEC.md#10-exact-binary-renderer).

## 5. Optional smooth rendering

Use a standalone framework-qualified renderer when an application needs smooth
desktop output. Swing/Java2D applications use `BitSquiggle32RendererSwing`:

```java
import bitsquiggles.renderer.swing.BitSquiggle32RendererSwing;

BitSquiggle32RendererSwing.renderSmooth(graphics, visual, width, height);
BitSquiggle32RendererSwing.renderRaster(graphics, raster, pixelSize);
```

JavaFX applications use `BitSquiggle32RendererJavaFX` with a canvas graphics
context:

```java
import bitsquiggles.renderer.javafx.BitSquiggle32RendererJavaFX;

BitSquiggle32RendererJavaFX.renderSmooth(graphics, visual, width, height);
BitSquiggle32RendererJavaFX.renderRaster(graphics, raster, pixelSize);
```

The renderers depend on their respective desktop toolkit but the
`BitSquiggle32` core does not. The separate `BitSquigglesDemo` application
demonstrates Swing renderer use alongside input controls and exact raster views.
The shared smooth-rendering constraints are in the
[specification](../SPEC.md#11-smooth-renderer); the shared renderer convention
is in the [API mapping](../SPEC.md#12-reference-api-mapping).

## 6. Test conformance

From the repository root:

```bash
mkdir -p out/core out/renderer-swing out/test
javac -d out/core java/core/module-info.java \
    java/bitsquiggles/BitSquiggle32.java \
    java/bitsquiggles/GalleryGenerator.java \
    java/bitsquiggles/ConformanceFixtureGenerator.java
javac -d out/renderer-swing --module-path out/core java/renderer-swing/module-info.java \
    java/renderer-swing/bitsquiggles/renderer/swing/BitSquiggle32RendererSwing.java
javac -d out/test -cp out/core:out/renderer-swing \
    java/bitsquiggles/BitSquiggle32Test.java \
    java/bitsquiggles/BitSquigglesDemo.java
java -cp out/core:out/renderer-swing:out/test bitsquiggles.BitSquiggle32Test
java --module-path out/core --module io.github.maggo83.bitsquiggles/bitsquiggles.GalleryGenerator --check
java --module-path out/core --module io.github.maggo83.bitsquiggles/bitsquiggles.ConformanceFixtureGenerator --check
```

To compile the optional JavaFX module, point `JAVAFX_LIB` at the JavaFX SDK's
`lib` directory and compile it independently from the headless core:

```bash
javac -d out/renderer-javafx \
    --module-path "out/core:$JAVAFX_LIB" \
    java/renderer-javafx/module-info.java \
    java/renderer-javafx/bitsquiggles/renderer/javafx/BitSquiggle32RendererJavaFX.java
```

The last two commands verify generated README SVGs and the shared versioned
conformance fixture. The complete conformance requirements are in the
[specification](../SPEC.md#13-conformance-requirements).

## 7. Package / release notes

Consume this port as source today: copy the core source into an application or
add the source module to its build. The core has no third-party runtime
dependency; the optional Java2D renderer and Swing demo use standard JDK APIs,
and the JavaFX renderer uses JavaFX Graphics.

## 8. Limitations and compatibility

This implementation requires Java 17 or later because it uses records and
modern switch syntax. It accepts every `int` bit pattern as an unsigned 32-bit
input; see the [shared input contract](../SPEC.md#1-scope-and-contract).

| Surface | Verified target | Notes |
| --- | --- | --- |
| Core module | Java 17+ | JPMS dependency closure is `java.base` only. |
| Swing renderer and demo | Java 17+ with `java.desktop` | Optional `renderer-swing` module. |
| JavaFX renderer | Java 17+ with JavaFX Graphics | Optional `renderer-javafx` module. |
| Reference tests | Java 17+ | Runs on the CI JDK and checks generated artifacts. |

## 9. License

Grug 2-Clause License. See the repository-level [LICENSE](../LICENSE).
