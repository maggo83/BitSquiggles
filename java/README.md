# BitSquiggle32 for Java 17+

← Back to the [BitSquiggles project overview](../README.md). Read the
[normative specification](../SPEC.md) for behavior shared by every port.

## 1. Status and scope

This guide covers the Java 17+ core and the bundled Swing/Java2D and JavaFX
renderers. **Use the selected renderer class as the normal application entry
point**: it statically delegates the complete core API and adds rendering.
Import `bitsquiggles.BitSquiggle32` directly only for a non-rendering or
separately rendered integration.

Project purpose, safety boundaries, and release status live in the
[project overview](../README.md); shared behavior is in [SPEC.md](../SPEC.md).

## 2. Include / install

### 2.1 Renderer-first integration (primary)

This port is source-only. Compile the dependency-free core module and the
renderer selected by the application. The Swing renderer uses standard JDK
`java.desktop`; the JavaFX renderer needs the JavaFX SDK `javafx.graphics`
module.

```bash
mkdir -p out/core out/renderer-swing
javac -d out/core java/core/module-info.java \
    java/core/bitsquiggles/BitSquiggle32.java
javac -d out/renderer-swing --module-path out/core \
    java/renderer-swing/module-info.java \
    java/renderer-swing/bitsquiggles/renderer/swing/BitSquiggle32RendererSwing.java
```

For JavaFX, replace the last command with the JavaFX compilation command in
[section 5](#5-test-conformance). Import only the chosen renderer class in
application code.

### 2.2 Core-only option

Compile `core/module-info.java` with `core/bitsquiggles/BitSquiggle32.java` and
import `bitsquiggles.BitSquiggle32` only when the application does not render
or owns a different renderer. The core requires only `java.base`.

## 3. Render a BitSquiggle

Pass canonical output to a renderer: call `pixels()` before `renderRaster()` or
`spec()` before `renderSmooth()`. Both renderer classes re-export the complete
core surface, so no separate core import is needed. Java accepts every unsigned
32-bit bit pattern through `int`.

### 3.1 Swing and Java2D renderer

```java
import static bitsquiggles.renderer.swing.BitSquiggle32RendererSwing.*;

int input = (int) Long.parseLong("12345678", 16);
var raster = pixels(input, BLACK_AND_WHITE);
renderRaster(graphics, raster, 4);

var visual = spec(input, HIGH_CONTRAST);
renderSmooth(graphics, visual, 160, 220);
```

`renderRaster()` paints whole `pixelSize` squares. `renderSmooth()` draws the
canonical smooth union into the available width and height.

### 3.2 JavaFX renderer

```java
import static bitsquiggles.renderer.javafx.BitSquiggle32RendererJavaFX.*;

int input = (int) Long.parseLong("12345678", 16);
var raster = pixels(input, BLACK_AND_WHITE);
renderRaster(graphics, raster, 4);

var visual = spec(input, HIGH_CONTRAST);
renderSmooth(graphics, visual, 160, 220);
```

The [exact raster](../spec/04-exact-raster.md) is the lossless baseline. Smooth
output is presentation-only and follows [smooth output](../spec/05-smooth-output.md).

## 4. Exposed API

Shared API semantics are defined in the [API contract](../spec/06-api.md). The
[presentation chapter](../spec/03-presentation.md) owns shared style, color,
and polarity rules.

### 4.1 Renderer entry points

| Renderer class | Core façade | Rendering operations |
| --- | --- | --- |
| `BitSquiggle32RendererSwing` | Complete static `BitSquiggle32` API and constants | `renderRaster(Graphics2D, PixelGrid, int)`, `renderSmooth(Graphics2D, VisSpec, int, int)` |
| `BitSquiggle32RendererJavaFX` | Complete static `BitSquiggle32` API and constants | `renderRaster(GraphicsContext, PixelGrid, int)`, `renderSmooth(GraphicsContext, VisSpec, int, int)` |

### 4.2 Core API

| Operation | Java result |
| --- | --- |
| `mix32(int)` | Mixed `int` bit pattern. |
| `edges()` | `Edge[]`. |
| `freeConnectionCount(Mode)` | Free-class count. |
| `matchesMode(byte[], Mode)` | Complete-family membership. |
| `spec(int[, Style])` | `VisSpec`. |
| `pixels(int[, Style])` | `PixelGrid`. |
| `extractSmoothBlobs(byte[])` | `SmoothBlob[]`. |

Public constants include `ROWS`, `COLUMNS`, `EDGE_COUNT`, `PIXEL_WIDTH`, and
`PIXEL_HEIGHT`; styles are `STANDARD`, `HIGH_CONTRAST`, `MONOCHROME`, and
`BLACK_AND_WHITE`. `Style` and `Mode` are public enums; public records are
`Edge`, `OklchColor`, `VisSpec`, `PixelGrid`, and `SmoothBlob`. Returned record
arrays are mutable; treat them as immutable or copy before sharing.

## 5. Test conformance

From the repository root:

```bash
mkdir -p out/core out/renderer-swing out/test
javac -d out/core java/core/module-info.java \
    java/core/bitsquiggles/BitSquiggle32.java \
    java/core/bitsquiggles/GalleryGenerator.java \
    java/core/bitsquiggles/ConformanceFixtureGenerator.java
javac -d out/renderer-swing --module-path out/core java/renderer-swing/module-info.java \
    java/renderer-swing/bitsquiggles/renderer/swing/BitSquiggle32RendererSwing.java
javac -d out/test -cp out/core:out/renderer-swing \
    java/core/bitsquiggles/BitSquiggle32Test.java \
    java/core/bitsquiggles/BitSquigglesDemo.java
java -cp out/core:out/renderer-swing:out/test bitsquiggles.BitSquiggle32Test
java --module-path out/core --module io.github.maggo83.bitsquiggles/bitsquiggles.GalleryGenerator --check
java --module-path out/core --module io.github.maggo83.bitsquiggles/bitsquiggles.ConformanceFixtureGenerator --check
```

Compile JavaFX independently by pointing `JAVAFX_LIB` at the SDK `lib` directory:

```bash
javac -d out/renderer-javafx --module-path "out/core:$JAVAFX_LIB" \
    java/renderer-javafx/module-info.java \
    java/renderer-javafx/bitsquiggles/renderer/javafx/BitSquiggle32RendererJavaFX.java
```

The generator checks validate README SVGs and the versioned fixture. See
[conformance](../spec/07-conformance.md) for common requirements.

## 6. Package / release notes

Consume this port as source today. The core has no third-party dependency; the
Swing renderer uses standard JDK APIs, and the JavaFX renderer uses JavaFX
Graphics. Select a renderer module for normal visual integrations.

## 7. Limitations and compatibility

This implementation requires Java 17 or later because it uses records and
modern switch syntax. See the [shared input contract](../spec/01-overview.md)
for unsigned-`int` value semantics.

| Surface | Verified target | Notes |
| --- | --- | --- |
| Core module | Java 17+ | JPMS dependency closure is `java.base` only. |
| Swing renderer and demo | Java 17+ with `java.desktop` | Renderer-first desktop option. |
| JavaFX renderer | Java 17+ with JavaFX Graphics | Optional target module. |
| Reference tests | Java 17+ | Checks generated artifacts and fixtures. |

## 8. License

Grug 2-Clause License. See the repository-level [LICENSE](../LICENSE).
