# BitSquiggle32 for Python and MicroPython

← Back to the [BitSquiggles project overview](../README.md). Read the
[normative specification](../SPEC.md) for behavior shared by every port.

## 1. Status and scope

This guide explains how to integrate the dependency-free CPython and
MicroPython-compatible reference implementation. Project purpose, safety
boundaries, and release status live in the [project overview](../README.md);
the shared encoding contract lives in the [specification](../SPEC.md).

## 2. Include / install

For MicroPython, copy `bitsquiggle32.py` to the device. To render, also copy the
selected renderer: `bitsquiggle32_renderer_framebuffer.py` for `fill_rect`
framebuffers or `bitsquiggles_renderer_lvgl.py` for LVGL. Import the selected
renderer in the application. The LVGL renderer imports `lvgl` only when used.

For CPython, install the same module from a checkout:

```bash
python3 -m pip install .
```

No third-party runtime dependency is installed.

## 3. Create a BitSquiggle

```python
import bitsquiggle32

visual = bitsquiggle32.spec(0x12345678)
raster = bitsquiggle32.pixels(0x12345678)
print(visual["actual_mode"], raster["foreground"]["hex"])
```

Both `spec()` and `pixels()` accept the same unsigned input and optional style.
The dictionary fields, supported styles, constants, helpers, and input
validation rules are documented by this guide; shared semantics are in the
[core API contract](../SPEC.md#12-core-api-contract).

### 3.1 Core API

The module exports the constants `ROWS`, `COLUMNS`, `EDGE_COUNT`,
`PIXEL_WIDTH`, `PIXEL_HEIGHT`, `EDGES`, `STYLES`, and `MODES`; styles
`STANDARD`, `HIGH_CONTRAST`, `MONOCHROME`, `BLACK_AND_WHITE`; and mode labels `LEFT_RIGHT`,
`TOP_BOTTOM`, `HALF_TURN`, and `DIAGONAL_SLASH`.

| Function | Python / MicroPython result |
| --- | --- |
| `mix32(bits)` | Mixed unsigned 32-bit `int`. |
| `free_connection_count(mode)` | Independent class count for a mode label. |
| `matches_mode(connections, mode)` | Whether the 58-entry connection sequence belongs to the mode family. |
| `spec(bits[, style])` | Canonical visual `dict`. |
| `pixels(bits[, style])` | Exact raster `dict`. |
| `smooth_blobs(connections)` | Ordered tuple of `(top_row, left_column, bottom_row, right_column)` tuples. |

`bits` must be an `int` in $[0,2^{32}-1]$; unknown styles raise `ValueError`.
`smooth_blobs()` accepts a 58-entry sequence containing only `0` and `1`, and
raises `ValueError` otherwise. Its output contains at most 82 blobs and uses
inclusive cell coordinates. Pass `visual["connections"]` directly when
rendering `spec()` output.

## 4. Render the exact raster

The `pixels()` result is the normative $16\times22$ binary raster. Render its
row-major `pixels` values as whole target pixels or integer-scaled squares;
`0` is background and `1` is foreground. See the complete
[exact-raster rules](../SPEC.md#10-exact-binary-renderer).

### 4.1 Black-and-white display style

`BLACK_AND_WHITE` is the shared style for displays with only two physical
states. It uses fixed base colors: foreground lightness `L = 1`, background
lightness `L = 0`, and zero chroma for both. The parity-driven
foreground/background lightness swap is then applied, so its colors are always
`#ffffff` and `#000000`. The raster geometry is unchanged, so existing exact
and smooth renderers can consume it without modification.

Use the complete bordered tile: the background border makes the swapped
polarity visible. The pure black-and-white bitmap remains unique; color and
intermediate luminance only provide optional comparison cues. This style is
defined by the shared specification and included in the cross-port fixture.

### 4.2 LVGL renderer

The optional `bitsquiggles_renderer_lvgl.py` module renders the exact raster as
a cached RGB565 image with integer scaling:

```python
import bitsquiggles_renderer_lvgl as bitsquiggles

raster = bitsquiggles.pixels(0x12345678, bitsquiggles.HIGH_CONTRAST)
bitsquiggles.render_raster(parent, raster, scale=2)
```

Call `clear_cache()` after deleting renderer parents.

### 4.3 Fill-rectangle framebuffer renderer

The optional `bitsquiggle32_renderer_framebuffer.py` renderer provides the
Python-conventional `render_raster()` operation. It consumes a `pixels()` grid,
never a BitSquiggle input value, and writes each grid element as an exact whole
target pixel or integer-scaled square. Its target must provide
`fill_rect(x, y, width, height, color)`.

```python
import bitsquiggle32_renderer_framebuffer as bitsquiggles

def map_color(color):
    return 1 if color == "#ffffff" else 0

raster = bitsquiggles.pixels(0x12345678, bitsquiggles.BLACK_AND_WHITE)
bitsquiggles.render_raster(
    framebuffer, raster, x=48, y=19, scale=2, color_mapper=map_color)
```

`color_mapper` adapts the canonical `#rrggbb` colors to the target's native
color values. This permits the same renderer to serve one-bit, indexed, RGB565,
and other `fill_rect` framebuffer targets without a target-specific public API.

## 5. Optional smooth rendering

Smooth rendering is presentation only: it must preserve selected and
unselected connections without changing the exact-raster identity. Follow the
[smooth-rendering constraints](../SPEC.md#11-smooth-renderer) and the renderer
naming convention in the [core API contract](../SPEC.md#12-core-api-contract).

### 5.1 LVGL renderer

The optional `bitsquiggles_renderer_lvgl.py` module supports LVGL targets
without adding an LVGL dependency to the core. Its rounded canvas presentation
uses the core's canonical `smooth_blobs()` via `render_smooth()`:

```python
import bitsquiggles_renderer_lvgl as bitsquiggles

visual = bitsquiggles.spec(0x12345678, bitsquiggles.HIGH_CONTRAST)
bitsquiggles.render_smooth(parent, visual, scale=4)
```

Call `clear_cache()` after deleting renderer parents.

### 5.2 Other CPython targets

For CPython applications, suitable optional renderer targets include Pillow for
image output, Tkinter for a standard-library desktop canvas, and pygame for an
interactive display. None is required or bundled, so the core stays
MicroPython-compatible.

## 6. Test conformance

Run the dependency-free test harness under CPython or MicroPython:

```bash
cd micropython
python3 test_bitsquiggle32.py
```

Repository CI runs this harness alongside checks for the shared fixture. The
complete conformance requirements are in the
[specification](../SPEC.md#13-conformance-requirements).

## 7. Package / release notes

`pyproject.toml` packages `bitsquiggle32.py` for CPython while retaining the
same standalone source file for MicroPython vendoring. The distribution has no
runtime dependencies.

## 8. Limitations and compatibility

Native MicroPython targets vary in available RAM; reduce long sampled test
loops on constrained devices if needed. See the
[shared input contract](../SPEC.md#1-scope-and-contract) for value semantics.

| Surface | Verified target | Notes |
| --- | --- | --- |
| CPython package | Python 3.8+ | Declared in package metadata; CI uses Python 3.12. |
| CPython conformance | Python 3.12 in CI | Consumes every shared fixture vector. |
| MicroPython core | Targets with `math` support | Vendor the single core file; no device-specific CI target is claimed. |
| LVGL renderer | LVGL 9 MicroPython targets | Optional vendored module; validate exact and smooth output on the target display. |
| MicroPython testing | Target-dependent | Run the property harness on-device as resources permit; full fixture parity runs on the host. |

## 9. License

Grug 2-Clause License. See the repository-level [LICENSE](../LICENSE).
