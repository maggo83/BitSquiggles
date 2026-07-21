# BitSquiggle32 for Python and MicroPython

← Back to the [BitSquiggles project overview](../README.md). Read the
[normative specification](../SPEC.md) for behavior shared by every port.

## 1. Status and scope

This guide covers the dependency-free CPython and MicroPython-compatible core,
the PyQt6 and LVGL renderers, and the generic fill-rectangle framebuffer
renderer. **Use a selected renderer module as the normal application import**:
it re-exports the complete core API and adds its rendering operations. Import
the core alone only for a non-rendering or separately rendered integration.

Project purpose, safety boundaries, and release status live in the
[project overview](../README.md); shared behavior is in [SPEC.md](../SPEC.md).

## 2. Include / install

### 2.1 Renderer-first integration (primary)

For MicroPython, copy `bitsquiggle32.py` and one selected renderer to the
device:

- `bitsquiggles_renderer_lvgl.py` for LVGL;
- `bitsquiggle32_renderer_framebuffer.py` for any target exposing
  `fill_rect(x, y, width, height, color)`.

Import the selected renderer in application code. It provides the core façade
and its render operation. The LVGL module imports `lvgl` only when that renderer
is selected.

For CPython, install the core from a checkout:

```bash
python3 -m pip install .
```

No third-party runtime dependency is installed by the core. The generic
framebuffer and LVGL renderers can be vendored from this directory when needed.

Install the optional PyQt6 renderer and its framework dependency with:

```bash
python3 -m pip install '.[pyqt6]'
```

### 2.2 Core-only option

Copy or install only `bitsquiggle32.py` when the application does not draw or
owns a different renderer:

```python
import bitsquiggle32

raster = bitsquiggle32.pixels(0x12345678)
```

## 3. Render a BitSquiggle

Use the renderer entry point to derive canonical output and render it. Call
`pixels()` before `render_raster()` and `spec()` before `render_smooth()`;
render operations consume those canonical values, never a BitSquiggle input.

### 3.1 LVGL renderer

The LVGL renderer supports exact raster and smooth output:

```python
import bitsquiggles_renderer_lvgl as bitsquiggles

input_value = 0x12345678
raster = bitsquiggles.pixels(input_value, bitsquiggles.BLACK_AND_WHITE)
image = bitsquiggles.render_raster(parent, raster, scale=2)

visual = bitsquiggles.spec(input_value, bitsquiggles.HIGH_CONTRAST)
smooth = bitsquiggles.render_smooth(parent, visual, scale=4)
```

`render_raster()` returns an integer-scaled RGB565 LVGL image. `render_smooth()`
returns a rounded LVGL canvas using canonical smooth blobs. Call `clear_cache()`
after deleting parents that contain renderer output.

### 3.2 Fill-rectangle framebuffer renderer

The framebuffer renderer supports exact raster output:

```python
import bitsquiggle32_renderer_framebuffer as bitsquiggles

def map_color(color):
    return 1 if color == "#ffffff" else 0

raster = bitsquiggles.pixels(0x12345678, bitsquiggles.BLACK_AND_WHITE)
bitsquiggles.render_raster(
    framebuffer, raster, x=48, y=19, scale=2, color_mapper=map_color)
```

It paints each source pixel as a whole target pixel or integer-scaled square.
`color_mapper` adapts canonical `#rrggbb` colors to one-bit, indexed, RGB565,
or other native values. Smooth output is not provided by this renderer.

### 3.3 PyQt6 renderer

The PyQt6 renderer supports exact raster and smooth output as `QPixmap` values:

```python
import bitsquiggles_renderer_pyqt6 as bitsquiggles

visual = bitsquiggles.spec(0x12345678, bitsquiggles.HIGH_CONTRAST)
smooth_pixmap = bitsquiggles.render_smooth(visual, scale=4)

grid = bitsquiggles.pixels(0x12345678, bitsquiggles.BLACK_AND_WHITE)
raster_pixmap = bitsquiggles.render_raster(grid, scale=2)
```

`render_smooth()` draws the canonical blob union with antialiasing.
`render_raster()` preserves every exact source pixel as an integer-scaled
rectangle without antialiasing.

## 4. Exposed API

Shared API semantics are defined in the [API contract](../spec/06-api.md). The
[presentation chapter](../spec/03-presentation.md) owns shared style, color, and
polarity rules.

### 4.1 Renderer entry points

| Renderer import | Core façade | Rendering operations |
| --- | --- | --- |
| `bitsquiggles_renderer_lvgl` | Complete core API | `render_raster(parent, grid[, scale])`, `render_smooth(parent, visual[, scale, bordered])`, `clear_cache()` |
| `bitsquiggle32_renderer_framebuffer` | Complete core API | `render_raster(target, grid[, x, y, scale, color_mapper])` |
| `bitsquiggles_renderer_pyqt6` | Complete core API | `render_raster(grid[, scale])`, `render_smooth(visual[, scale])` |

### 4.2 Core API

| Function | Python / MicroPython result |
| --- | --- |
| `mix32(bits)` | Mixed unsigned 32-bit `int`. |
| `free_connection_count(mode)` | Independent class count for a mode label. |
| `matches_mode(connections, mode)` | Whether the 58-entry connection sequence belongs to the mode family. |
| `spec(bits[, style])` | Canonical visual `dict`. |
| `pixels(bits[, style])` | Exact raster `dict`. |
| `smooth_blobs(connections)` | Ordered tuple of inclusive cell-coordinate tuples. |

The core exports `ROWS`, `COLUMNS`, `EDGE_COUNT`, `PIXEL_WIDTH`,
`PIXEL_HEIGHT`, `EDGES`, `STYLES`, and `MODES`; styles `STANDARD`,
`HIGH_CONTRAST`, `MONOCHROME`, `BLACK_AND_WHITE`; and mode labels
`LEFT_RIGHT`, `TOP_BOTTOM`, `HALF_TURN`, and `DIAGONAL_SLASH`. `bits` must be
an `int` in $[0,2^{32}-1]$; invalid styles and masks raise `ValueError`.

## 5. Test conformance

Run the dependency-free harness under CPython or MicroPython:

```bash
cd micropython
python3 test_bitsquiggle32.py
```

Repository CI runs this harness alongside shared-fixture checks. See
[conformance](../spec/07-conformance.md) for the common requirements.

## 6. Package / release notes

`pyproject.toml` packages `bitsquiggle32.py` for CPython while retaining the
same standalone source for MicroPython vendoring. Renderer modules are optional
vendored source so the core remains dependency-free and MicroPython-compatible.

## 7. Limitations and compatibility

Native MicroPython targets vary in available RAM; reduce long sampled test loops
on constrained devices if needed. See the [shared input contract](../spec/01-overview.md)
for value semantics.

| Surface | Verified target | Notes |
| --- | --- | --- |
| CPython package | Python 3.8+ | Declared in package metadata; CI uses Python 3.12. |
| CPython conformance | Python 3.12 in CI | Consumes every shared fixture vector. |
| PyQt6 renderer | PyQt6 6.5+ | Optional CPython dependency; exact and smooth `QPixmap` output. |
| MicroPython core | Targets with `math` support | Vendor the single core file. |
| LVGL renderer | LVGL 9 MicroPython targets | Validate exact and smooth output on the target display. |
| Framebuffer renderer | `fill_rect` targets | Validate target color mapping and exact output. |

## 8. License

Grug 2-Clause License. See the repository-level [LICENSE](../LICENSE).
