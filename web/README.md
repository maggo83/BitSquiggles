# BitSquiggle32 for JavaScript and TypeScript

## 1. Status and scope

This directory contains both the BitSquiggles playground and the dependency-free
BitSquiggle32 ESM core. The core produces the canonical visual specification and
exact $16\times22$ binary raster. It does not derive fingerprints or make
security decisions from them.

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

## 3. Create a BitSquiggle

```js
import { formatHex, parseHex, visualize } from "bitsquiggles";

const visual = visualize(parseHex("12345678"));
console.log(formatHex(visual.mixed), visual.actualMode);
```

`visualize()` returns the input, mixed value, connections, active cells, exact
raster, mode/fallback metadata, and foreground/background colors.
`pixels(connections)` returns an exact raster for a canonical connection mask.
`parseHex()` accepts one to eight hexadecimal digits; `formatHex()` returns
eight uppercase hexadecimal digits.

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

Do not smooth, interpolate, or alter raster pixels when exact comparison is
required. For larger output, use nearest-neighbor scaling.

## 5. Optional smooth rendering

The playground demonstrates a smooth browser presentation derived from the
connection and cell data. Smooth rendering is optional and must not redefine
the canonical connection mask or exact raster.

## 6. Test conformance

```bash
npm test
```

The test suite reads the versioned Java-generated conformance fixture and
checks every shared vector.

## 7. Package / release notes

Run `npm pack --dry-run` to inspect the exact package artifact before
publishing. The public package name is `bitsquiggles`; package metadata exports
the ESM core and its optional TypeScript declarations.

## 8. Limitations and compatibility

The core expects JavaScript numbers carrying 32-bit bit patterns. Use
`parseHex()` for external hexadecimal input and `formatHex()` for unsigned
display. The core is standard ESM; consumers without ESM support need a
target-specific integration layer. See the repository specification for the
interoperable algorithm contract.

## 9. License

Grug 2-Clause License. See [LICENSE](LICENSE).
