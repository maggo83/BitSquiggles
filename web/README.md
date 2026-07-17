# BitSquiggle32 ESM package

This directory is both the source for the BitSquiggles playground and a
publishable dependency-free ESM package. It has no runtime dependencies and no
build step.

## Install

After publication, install the package with:

```bash
npm install bitsquiggles
```

For local integration before publication, install this directory directly:

```bash
npm install /path/to/BitSquiggles/web
```

Run `npm pack --dry-run` to inspect the exact package artifact before publishing.

## Use

```js
import { formatHex, parseHex, visualize } from "bitsquiggles";

const visual = visualize(parseHex("12345678"));
console.log(formatHex(visual.mixed), visual.actualMode);
```

`visualize()` returns the mixed value, selected connections, active cells,
exact raster, preferred and rendered modes, fallback state, and foreground and
background colors. Use `pixels(connections)` when only the exact raster is
needed. `parseHex()` accepts one to eight hexadecimal digits; `formatHex()`
returns eight uppercase hexadecimal digits.

The included TypeScript declarations are optional editor support. JavaScript is
the executable source of truth.

## Render the exact raster

The `pixels` field is a row-major $16\times22$ `Uint8Array`: `0` is background
and `1` is foreground. Render each value as a whole pixel; do not smooth or
interpolate it.

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

For larger output, scale with nearest-neighbor rendering. Smooth renderings
are presentation options only and do not redefine the encoded value.

## Test

```bash
npm test
```

The test suite reads the versioned Java-generated conformance fixture and
checks every shared vector. See the repository-level README and specification
for the full algorithm and conformance contract.

## License

Grug 2-Clause License. See [LICENSE](LICENSE).
