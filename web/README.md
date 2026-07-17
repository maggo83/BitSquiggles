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
handling are defined in the
[JavaScript API mapping](../SPEC.md#123-javascript-and-typescript).

## 4. Render the exact raster

`visual.pixels` is a row-major $16\times22$ `Uint8Array`: `0` is background and
`1` is foreground. Render each value as one whole pixel:

```js
const context = canvas.getContext("2d");
for (let y = 0; y < 22; y += 1) {
  for (let x = 0; x < 16; x += 1) {
    context.fillStyle = visual.pixels[y * 16 + x]
      ? visual.foreground : visual.background;
    context.fillRect(x, y, 1, 1);
  }
}
```

The exact dimensions and rendering rules are defined in the
[normative raster specification](../SPEC.md#10-exact-binary-renderer). For
larger output, use nearest-neighbor scaling.

## 5. Optional smooth rendering

The optional Canvas 2D renderer is separate from both the core and the
playground application. Import it when a browser application needs the bundled
smooth presentation:

```js
import { renderRaster, renderSmooth } from "bitsquiggles/renderer";

renderSmooth(canvas, visual);
renderRaster(rasterCanvas, raster);
```

`playground.js` is only the live-demo application: it owns the form, URL state,
and page updates, then delegates drawing to the Canvas renderer. Follow the
shared [smooth-rendering constraints](../SPEC.md#11-smooth-renderer).
`renderSmooth()` paints the smooth presentation; `renderRaster()` paints the
exact native raster. `PIXEL_WIDTH` and `PIXEL_HEIGHT` are also exported by this
optional renderer.

## 6. Test conformance

```bash
npm test
```

The test suite reads the versioned Java-generated conformance fixture and
checks every shared vector. The complete conformance requirements are in the
[specification](../SPEC.md#13-conformance-requirements).

## 7. Package / release notes

The `bitsquiggles` package exposes the core at `bitsquiggles` and the optional
Canvas renderer at `bitsquiggles/renderer`. Both include TypeScript declarations.
Use the core alone when a different graphics toolkit is preferred.

## 8. Limitations and compatibility

The core is standard ESM; consumers without ESM support need a target-specific
integration layer. See the [shared input contract](../SPEC.md#1-scope-and-contract)
for 32-bit value semantics.

## 9. License

Grug 2-Clause License. See [LICENSE](LICENSE).
