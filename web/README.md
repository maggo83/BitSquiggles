# BitSquiggle32 for JavaScript and TypeScript

← Back to the [BitSquiggles project overview](../README.md). Read the
[normative specification](../SPEC.md) for behavior shared by every port.

## 1. Status and scope

This guide explains how to use the dependency-free ESM core, its optional Canvas
renderer, and the live playground. Project purpose, safety boundaries, and
release status live in the [project overview](../README.md); the shared encoding
contract lives in the [specification](../SPEC.md).

## 2. Include / install

After publication, install the package with:

```bash
npm install bitsquiggles
```

For local integration before publication, install this directory directly:

```bash
npm install /path/to/BitSquiggles/web
```

The package has no runtime dependencies or build step. TypeScript declarations
are optional editor support; JavaScript is the executable source of truth.
The live playground is available at
[https://maggo83.github.io/BitSquiggles/](https://maggo83.github.io/BitSquiggles/).

## 3. Create a BitSquiggle

```js
import { formatHex, parseHex, pixels, spec } from "bitsquiggles";

const input = parseHex("12345678");
const visual = spec(input);
const raster = pixels(input);
console.log(formatHex(visual.mixed), visual.actualMode);
```

Both `spec()` and `pixels()` accept the same signed 32-bit bit pattern and
optional style. The API return values, styles, constants, and hexadecimal
handling are documented by this guide; shared semantics are in the
[core API contract](../SPEC.md#12-core-api-contract).

### 3.1 Core API

The core exports `ROWS`, `COLUMNS`, `EDGE_COUNT`, `PIXEL_WIDTH`,
`PIXEL_HEIGHT`, `EDGES`, `STYLES`, and `MODES`.

| Function | JavaScript result |
| --- | --- |
| `mix32(input)` | Mixed signed 32-bit bit pattern. |
| `freeConnectionCount(mode)` | Independent class count for a mode label. |
| `matchesMode(connections, mode)` | Whether a connection mask belongs to a mode family. |
| `spec(input[, style])` | Canonical visual object. |
| `pixels(input[, style])` | Exact raster object. |
| `smoothBlobs(connections)` | Ordered `{ topRow, leftColumn, bottomRow, rightColumn }` objects. |

Inputs use JavaScript signed 32-bit bit patterns; use `parseHex()` and
`formatHex()` at unsigned hexadecimal boundaries. `smoothBlobs()` accepts a
binary, 58-entry `Uint8Array` and throws `RangeError` for invalid masks. Blob
coordinates are inclusive cell coordinates, and output contains at most 82
objects. TypeScript declarations include `SmoothBlob`.

## 4. Render the exact raster

The `pixels()` result is the normative $16\times22$ binary raster. Render its
row-major values as whole target pixels or nearest-neighbor integer-scaled
squares; `0` is background and `1` is foreground. See the complete
[exact-raster rules](../SPEC.md#10-exact-binary-renderer).

### 4.1 Canvas 2D renderer

```js
import { renderRaster } from "bitsquiggles/renderer-canvas";

renderRaster(rasterCanvas, raster);
```

## 5. Optional smooth rendering

Smooth rendering is presentation only: it must preserve selected and
unselected connections without changing the exact-raster identity. Follow the
[smooth-rendering constraints](../SPEC.md#11-smooth-renderer) and the renderer
naming convention in the [core API contract](../SPEC.md#12-core-api-contract).

### 5.1 Canvas 2D renderer

The optional Canvas renderer is separate from both the core and playground
application. Import it when a browser application needs the bundled smooth
presentation:

```js
import { renderSmooth } from "bitsquiggles/renderer-canvas";

renderSmooth(canvas, visual);
```

The bundled renderer calls `smoothBlobs(visual.connections)` and paints each
canonical rounded rectangle using the geometry in the shared specification.

`playground.js` is only the live-demo application: it owns the form, URL state,
and page updates, then delegates drawing to the Canvas renderer. `PIXEL_WIDTH`
and `PIXEL_HEIGHT` are also exported by this optional renderer.

## 6. Test conformance

```bash
npm test
```

The test suite reads the versioned Java-generated conformance fixture and
checks every shared vector. The complete conformance requirements are in the
[specification](../SPEC.md#13-conformance-requirements).

## 7. Package / release notes

The `bitsquiggles` package exposes the core at `bitsquiggles` and the optional
Canvas renderer at `bitsquiggles/renderer-canvas`. Both include TypeScript declarations.
Use the core alone when a different graphics toolkit is preferred.

## 8. Limitations and compatibility

The core is standard ESM; consumers without ESM support need a target-specific
integration layer. See the [shared input contract](../SPEC.md#1-scope-and-contract)
for 32-bit value semantics.

| Surface | Verified target | Notes |
| --- | --- | --- |
| ESM core and package tests | Node.js 22 in CI | Uses native ESM and checks every shared fixture vector. |
| Canvas renderer | Modern browser with Canvas 2D and `roundRect()` | Optional `renderer-canvas` subpath. |
| Playground | Modern browser with ESM, Canvas 2D, Web Crypto, and Clipboard APIs | Node smoke tests cover DOM wiring with fakes; browser compatibility is not otherwise version-pinned. |

## 9. License

Grug 2-Clause License. See [LICENSE](LICENSE).
