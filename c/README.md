# BitSquiggle32 for C99

← Back to the [BitSquiggles project overview](../README.md). Read the
[normative specification](../SPEC.md) for behavior shared by every port.

## 1. Status and scope

This is a dependency-free C99 core. It provides canonical specifications,
exact grids, and smooth blobs, but **does not currently bundle a renderer
façade**. Renderer-first integration is therefore the intended target
architecture but is not yet available in this port. Use the core-only option
only until a target-specific renderer is supplied.

The core depends only on the C standard library and the standard math library
for color conversion. Project purpose, safety boundaries, and release status
live in the [project overview](../README.md); shared behavior is in
[SPEC.md](../SPEC.md).

## 2. Include / install

### 2.1 Renderer-first integration (primary)

No C renderer is bundled yet. A target-specific C renderer should expose the
complete core surface and at least one rendering operation as required by the
[renderer contract](../spec/06-api.md#renderer-contract). When available,
include that renderer entry point rather than calling this core directly.

### 2.2 Core-only option

Until a renderer is available, copy [bitsquiggle32.h](bitsquiggle32.h) and
[bitsquiggle32.c](bitsquiggle32.c) into a C99 project, then link with `-lm`:

```sh
cc -std=c99 -Wall -Wextra -Werror -pedantic bitsquiggle32.c app.c -lm -o app
```

No allocator, operating-system API, renderer, or third-party library is
required by the core.

## 3. Render a BitSquiggle

A selected renderer should consume output from `bitsquiggle32_pixels()` for
exact raster drawing or `bitsquiggle32_spec()` for smooth drawing. It must not
accept an identity input directly.

### 3.1 Renderer availability

No renderer is bundled for C99. Add a separately named framework-specific
renderer that follows the [renderer contract](../spec/06-api.md#renderer-contract).
This port cannot yet provide a renderer-first code example.

### 3.2 Core-only output

Core-only integrations can produce exact raster output for a target-owned
drawing layer:

```c
#include "bitsquiggle32.h"

Bitsquiggle32PixelGrid raster;
if (bitsquiggle32_pixels(UINT32_C(0x12345678),
        BITSQUIGGLE32_BLACK_AND_WHITE, &raster) == 0) {
    /* Render raster.pixels using raster.background and raster.foreground. */
}
```

The target layer must draw every source pixel as a whole pixel or integer-scaled
square according to the [exact raster](../spec/04-exact-raster.md). For smooth
output, obtain `Bitsquiggle32Spec` and pass `visual.connections` to
`bitsquiggle32_smooth_blobs()`.

## 4. Exposed API

Shared API semantics are defined in the [API contract](../spec/06-api.md). The
[presentation chapter](../spec/03-presentation.md) owns shared style, color,
and polarity rules.

### 4.1 Renderer entry point

| Renderer entry point | Status | Rendering operations |
| --- | --- | --- |
| None bundled | Not available | A future target-specific façade must expose all core operations and at least `renderRaster()` or `renderSmooth()`. |

### 4.2 Core API

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
writes inclusive-coordinate `Bitsquiggle32SmoothBlob` values. Its caller-owned
array must contain `BITSQUIGGLE32_MAX_SMOOTH_BLOBS` entries (82). It returns the
blob count, or `-1` for invalid arguments; no allocation is performed.

## 5. Test conformance

From this directory, run:

```sh
make test
```

The harness compiles with strict C99 warnings and checks edge ordering, family
capacity, the golden vector, style-independent geometry, lossless raster
bridges, and every vector/style in `fixtures/v1.json`. See
[conformance](../spec/07-conformance.md) for common requirements.

## 6. Package / release notes

The C99 port is source-only. It follows the shared version policy in
[RELEASING.md](../RELEASING.md) and is not yet published as a package or archive.

## 7. Limitations and compatibility

The API uses caller-provided output structs. Every `uint32_t` value is valid;
only style and output-pointer validity can fail. See the
[shared input contract](../spec/01-overview.md).

| Surface | Verified target | Notes |
| --- | --- | --- |
| Core | ISO C99 compiler with C math library | No allocation or non-standard dependency. |
| Test harness | C99 compiler with `make` | CI uses strict warnings and the shared fixture. |
| Renderer | None bundled | Add a separately named target-specific façade. |

## 8. License

Grug 2-Clause License. See the repository-level [LICENSE](../LICENSE).
