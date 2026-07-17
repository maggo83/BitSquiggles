# BitSquiggle32 for C99

← Back to the [BitSquiggles project overview](../README.md). Read the
[normative specification](../SPEC.md) for behavior shared by every port.

## 1. Status and scope

This is a dependency-free C99 core implementation. It provides the canonical
`spec()` and `pixels()` operations as `bitsquiggle32_spec()` and
`bitsquiggle32_pixels()`. The core performs no drawing and depends only on the
C standard library plus the standard math library for color conversion.

Project purpose, safety boundaries, and release status live in the
[project overview](../README.md); the shared encoding contract lives in the
[specification](../SPEC.md).

## 2. Include / install

Copy [bitsquiggle32.h](bitsquiggle32.h) and [bitsquiggle32.c](bitsquiggle32.c)
into a C99 project, then link with `-lm`:

```sh
cc -std=c99 -Wall -Wextra -Werror -pedantic bitsquiggle32.c app.c -lm -o app
```

No allocator, operating-system API, renderer, or third-party library is
required by the core.

## 3. Create a BitSquiggle

C uses `uint32_t` for the full unsigned input domain. Supply a caller-owned
`Bitsquiggle32Spec`; the function returns zero on success.

```c
#include "bitsquiggle32.h"

Bitsquiggle32Spec visual;
if (bitsquiggle32_spec(UINT32_C(0x12345678), BITSQUIGGLE32_STANDARD, &visual) != 0) {
    /* Invalid style or output pointer. */
}

printf("%s\n", visual.foreground.hex);
```

`Bitsquiggle32Spec` includes the mixed value, connection mask, active cells,
colors, requested style, preferred and actual modes, fallback, luminance index,
and polarity metadata. Use `bitsquiggle32_mode_label()` to obtain `A|`, `A-`,
`A+`, or `A/` for a mode enum.

## 4. Render the exact raster

`bitsquiggle32_pixels()` fills a caller-owned `Bitsquiggle32PixelGrid`. Its
352 `pixels` values are row-major bytes: `0` for background and `1` for
foreground. Draw every value as one target pixel or an integer-scaled square.

```c
Bitsquiggle32PixelGrid raster;
bitsquiggle32_pixels(value, BITSQUIGGLE32_STANDARD, &raster);
for (uint8_t y = 0; y < raster.height; ++y) {
    for (uint8_t x = 0; x < raster.width; ++x) {
        bool on = raster.pixels[y * raster.width + x] != 0;
        /* Draw an integer-scaled foreground or background square. */
    }
}
```

The dimensions and exact rendering rules are defined in the
[normative raster specification](../SPEC.md#10-exact-binary-renderer).

## 5. Optional smooth rendering

No renderer is bundled. A framework-specific renderer remains optional and
must use the shared `renderSmooth()`/`renderRaster()` semantics and a
framework-qualified name, as defined in the
[reference API mapping](../SPEC.md#12-reference-api-mapping).

## 6. Test conformance

From this directory, run:

```sh
make test
```

The harness compiles with strict C99 warnings, tests edge ordering, family
capacity, the normative golden vector, style-independent geometry, lossless
raster bridges, and every vector/style in `fixtures/v1.json`.

## 7. Package / release notes

The C99 port is source-only. It follows the project’s shared version policy;
see [RELEASING.md](../RELEASING.md). It is not yet published as a package or
archive.

## 8. Limitations and compatibility

| Surface | Verified target | Notes |
| --- | --- | --- |
| Core | ISO C99 compiler with C math library | No allocation or non-standard dependency. |
| Test harness | C99 compiler with `make` | CI builds with strict warnings and runs the shared fixture. |
| Renderer | None bundled | Add only a separately named framework adapter. |

The API uses caller-provided output structs. All input values represent the
full `uint32_t` domain; only style and output-pointer validity can fail.

## 9. License

Grug 2-Clause License. See the repository-level [LICENSE](../LICENSE).
