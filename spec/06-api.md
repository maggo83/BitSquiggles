# BitSquiggle32 API contract

**Normative.** This chapter defines the public core and renderer surface.
Shared semantics depend on [encoding](02-encoding.md), [presentation](03-presentation.md),
and the output chapters. Each port guide owns target-language names, signatures,
containers, validation, ownership, and helpers.

## Core API contract

Every core exposes these public operations using its language's conventional
naming, arguments, return values, and error reporting.

### Application-facing operations

| Operation | Result |
| --- | --- |
| `spec(input[, style])` | canonical visual specification |
| `pixels(input[, style])` | exact pixel grid and its colors |
| `smoothBlobs(connections)` | ordered canonical smooth blobs |

`spec()` is pure: it derives the mixed input, connections, active cells, colors,
style, preferred and actual modes, fallback state, luminance index, and polarity
metadata. `pixels()` is also pure and derives the exact 16×22 binary raster and
its colors from the same input and style. Neither operation draws anything.
`smoothBlobs()` is also pure; it consumes a canonical connection mask and
returns the presentation-only ordered blob decomposition from
[smooth output](05-smooth-output.md#canonical-blob-extraction).

### Public conformance helpers

| Operation | Result |
| --- | --- |
| `mix32(input)` | bijective 32-bit mixed value |
| `edges()` | 58 canonical edges in [encoding order](02-encoding.md#fixed-dimensions-values-and-ordering) |
| `freeConnectionCount(mode)` | independent connection-class count |
| `matchesMode(connections, mode)` | complete-family membership |

Every core also exposes `ROWS`, `COLUMNS`, `EDGE_COUNT`, `PIXEL_WIDTH`, and
`PIXEL_HEIGHT`; the four styles (Standard, High contrast, Monochrome, and Black
and white); and mode labels `A|`, `A-`, `A+`, and `A/`. These helpers support
diagnostics and conformance; applications normally use the preceding
application-facing operations.

## Renderer contract

Optional target renderers append a framework identifier to the
`BitSquiggle32Renderer` root, using the target language's normal naming style.
Examples are Java `BitSquiggle32RendererSwing`, JavaScript
`bitsquiggle32-renderer-canvas`, Python `bitsquiggle32_renderer_pillow`, and
MicroPython `bitsquiggle32_renderer_lvgl`. A renderer is optional and does not
change core identity or the [exact raster](04-exact-raster.md) contract.

### Renderer public surface

| Surface | Requirement | Input | Result or constraint |
| --- | --- | --- | --- |
| Complete declared core API | Required | The core operations, conformance helpers, and public constants | Expose them through the renderer entry point with unchanged behavior and values. |
| `renderRaster()` | Optional | Canonical pixel grid | Paint every grid element as an exact whole target pixel or integer-scaled square. It must not accept an identity input. |
| `renderSmooth()` | Optional | Canonical visual specification | Render the smooth presentation according to [smooth output](05-smooth-output.md); antialiasing must not close an unselected connection. |
| Target-specific helpers | Optional | Target-defined | May provide convenience operations without changing the required core or renderer operations. |

Every renderer **must** expose at least a smooth or a raster rendering operation.
Every renderer **must** expose the complete declared public core API through
its own public entry point, using the target language's conventional façade or
re-export mechanism, and add its renderer-specific operations. An integrator
can therefore use the selected renderer as the single application entry point
for `spec()`, `pixels()`, and rendering. Re-exported operations and constants
must retain the core's behavior and values. This does not change operation
ownership, source dependencies, or the requirement that `renderRaster()`
consumes a canonical pixel grid rather than an identity input.

Each port guide owns exact exported names, signatures, container types, input
validation, ownership, and target-specific helpers.

## Related

- Core transformation: [encoding](02-encoding.md)
- Exact output: [exact raster](04-exact-raster.md)
- Smooth output: [smooth output](05-smooth-output.md)
- Required validation: [conformance](07-conformance.md)
