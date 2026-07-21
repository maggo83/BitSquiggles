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

Styles are `BITSQUIGGLE32_STANDARD`, `BITSQUIGGLE32_HIGH_CONTRAST`,
`BITSQUIGGLE32_MONOCHROME`, and `BITSQUIGGLE32_BLACK_AND_WHITE`. The
black-and-white style emits only `#000000` and `#ffffff` after the
parity-derived polarity swap. Its complete bordered bitmap is unique by itself;
color and intermediate luminance are optional comparison aids.

### 3.1 Core API

| Function | Result |
| --- | --- |
| `bitsquiggle32_mix32(value)` | Mixed `uint32_t` value. |
| `bitsquiggle32_edges(output)` | Fills 58 canonical edges in caller-owned storage. |
| `bitsquiggle32_free_connection_count(mode)` | Independent class count; zero for an invalid mode. |
| `bitsquiggle32_matches_mode(connections, mode)` | Complete-family membership. |
| `bitsquiggle32_spec(input, style, output)` | Fills a canonical visual specification. |
| `bitsquiggle32_pixels(input, style, output)` | Fills the exact raster. |
| `bitsquiggle32_smooth_blobs(connections, output)` | Returns ordered canonical smooth blobs. |

`bitsquiggle32_smooth_blobs()` consumes a binary 58-entry connection array and
writes `Bitsquiggle32SmoothBlob` values with inclusive cell coordinates. Its
caller-owned output array must contain `BITSQUIGGLE32_MAX_SMOOTH_BLOBS` entries
(82). The return value is the blob count, or `-1` for null pointers or a
non-binary connection array. No allocation is performed.

## 4. Render the exact raster

`bitsquiggle32_pixels()` fills a caller-owned `Bitsquiggle32PixelGrid`. It is
the normative $16\times22$ binary raster: render its row-major `pixels` values
as whole target pixels or integer-scaled squares; `0` is background and `1` is
foreground. See the complete [exact-raster rules](../spec/04-exact-raster.md).

```c
Bitsquiggle32PixelGrid raster;
bitsquiggle32_pixels(value, BITSQUIGGLE32_STANDARD, &raster);
```

## 5. Optional smooth rendering

Smooth rendering is presentation only: it must preserve selected and
unselected connections without changing the exact-raster identity. Follow the
[smooth-output constraints](../spec/05-smooth-output.md) and the renderer
naming convention in the [core API contract](../spec/06-api.md).

This port bundles no renderer. Add a framework-specific adapter only when the
target requires one. Use `bitsquiggle32_smooth_blobs()` as the normal path:
draw each returned rectangle with the canonical geometry from the
[smooth-blob specification](../spec/05-smooth-output.md#canonical-smooth-blobs).

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
