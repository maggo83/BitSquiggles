# BitSquiggle32 for JavaScript and TypeScript

← Back to the [BitSquiggles project overview](../README.md). Read the
[normative specification](../SPEC.md) for behavior shared by every port.

## 1. Status and scope

This guide covers the dependency-free ESM core and the bundled Canvas 2D
renderer. **Import the Canvas renderer entry point for a browser visual**: it
re-exports the complete core API and is the normal application entry point.
Importing only the core is an option for consumers that deliberately provide a
different renderer or do not render.

Project purpose, safety boundaries, and release status live in the
[project overview](../README.md). Shared behavior is defined by the
[specification](../SPEC.md).

## 2. Include / install

### 2.1 Renderer-first integration (primary)

After publication, install the package:

```bash
npm install bitsquiggles
```

For a local checkout before publication:

```bash
npm install /path/to/BitSquiggles/web
```

Import the Canvas entry point, not the bare core. It provides `spec()`,
`pixels()`, constants, helpers, `renderRaster()`, and `renderSmooth()` from one
module:

```js
import { pixels, renderRaster, renderSmooth, spec } from "bitsquiggles/renderer-canvas";
```

The package has no runtime dependencies or build step. TypeScript declarations
are included. The live playground is available at
[https://maggo83.github.io/BitSquiggles/](https://maggo83.github.io/BitSquiggles/).

### 2.2 Core-only option

Use the bare `bitsquiggles` entry point only when an application does not draw
or supplies a different renderer. It exposes no Canvas APIs:

```js
import { pixels, spec } from "bitsquiggles";
```

## 3. Render a BitSquiggle

A renderer consumes canonical output: call `pixels()` before `renderRaster()`,
or `spec()` before `renderSmooth()`. Do not pass an identity input directly to
a renderer. The renderer entry point is also the core façade.

### 3.1 Canvas 2D renderer

The Canvas renderer supports both required drawing modes:

```js
import {
  BLACK_AND_WHITE,
  HIGH_CONTRAST,
  pixels,
  renderRaster,
  renderSmooth,
  spec,
} from "bitsquiggles/renderer-canvas";

const input = 0x12345678;

const raster = pixels(input, BLACK_AND_WHITE);
renderRaster(rasterCanvas, raster);

const visual = spec(input, HIGH_CONTRAST);
renderSmooth(smoothCanvas, visual);
```

`renderRaster()` paints the exact $16\times22$ grid at native Canvas pixels.
`renderSmooth()` renders the canonical smooth-blob presentation. The latter is
presentation only; the [exact raster](../spec/04-exact-raster.md) remains the
lossless baseline.

## 4. Exposed API

Shared API semantics are defined in the [API contract](../spec/06-api.md).
`STYLES` includes `standard`, `high-contrast`, `monochrome`, and
`black-and-white`. The [presentation chapter](../spec/03-presentation.md)
owns their shared color and polarity rules.

### 4.1 Renderer entry point

| Export from `bitsquiggles/renderer-canvas` | Use |
| --- | --- |
| All core exports | Derive specs and grids through the selected renderer import. |
| `renderRaster(canvas, grid)` | Draw an exact Canvas raster from `pixels()` output. |
| `renderSmooth(canvas, visual)` | Draw a smooth Canvas presentation from `spec()` output. |

### 4.2 Core API

| Function | JavaScript result |
| --- | --- |
| `mix32(input)` | Mixed signed 32-bit bit pattern. |
| `freeConnectionCount(mode)` | Independent class count for a mode label. |
| `matchesMode(connections, mode)` | Whether a connection mask belongs to the mode family. |
| `spec(input[, style])` | Canonical visual object. |
| `pixels(input[, style])` | Exact raster object. |
| `smoothBlobs(connections)` | Ordered `{ topRow, leftColumn, bottomRow, rightColumn }` objects. |

The core also exports `ROWS`, `COLUMNS`, `EDGE_COUNT`, `PIXEL_WIDTH`,
`PIXEL_HEIGHT`, `EDGES`, `STYLES`, and `MODES`. Inputs use JavaScript signed
32-bit bit patterns; use `parseHex()` and `formatHex()` at unsigned hexadecimal
boundaries. `smoothBlobs()` accepts a binary 58-entry `Uint8Array` and throws
`RangeError` for invalid masks.

## 5. Test conformance

```bash
npm test
```

The suite reads the Java-generated versioned fixture and checks every shared
vector. See [conformance](../spec/07-conformance.md) for shared requirements.

## 6. Package / release notes

The package exposes the core at `bitsquiggles` and the renderer façade at
`bitsquiggles/renderer-canvas`; both include TypeScript declarations. Use the
renderer façade for Canvas applications. Use the core-only entry point only for
a non-Canvas or non-rendering integration.

## 7. Limitations and compatibility

The core is standard ESM; consumers without ESM support need a target-specific
integration layer. See the [shared input contract](../spec/01-overview.md) for
32-bit value semantics.

| Surface | Verified target | Notes |
| --- | --- | --- |
| ESM core and package tests | Node.js 22 in CI | Uses native ESM and checks every shared fixture vector. |
| Canvas renderer | Modern browser with Canvas 2D and `roundRect()` | Renderer-first subpath. |
| Playground | Modern browser with ESM, Canvas 2D, Web Crypto, and Clipboard APIs | Node smoke tests cover DOM wiring with fakes. |

## 8. License

Grug 2-Clause License. See [LICENSE](LICENSE).
