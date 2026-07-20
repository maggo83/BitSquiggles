# BitSquiggle32 for Python and MicroPython

← Back to the [BitSquiggles project overview](../README.md). Read the
[normative specification](../SPEC.md) for behavior shared by every port.

## 1. Status and scope

This guide explains how to integrate the dependency-free CPython and
MicroPython-compatible reference implementation. Project purpose, safety
boundaries, and release status live in the [project overview](../README.md);
the shared encoding contract lives in the [specification](../SPEC.md).

## 2. Include / install

For MicroPython, copy `bitsquiggle32.py` to the device and import it directly.
It uses only core language features plus `math`. For LVGL targets, also vendor
`bitsquiggles_renderer_lvgl.py`; it is optional and imports `lvgl` only when
the renderer is used.

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
validation rules are defined in the
[Python API mapping](../SPEC.md#122-micropython-compatible-python).

## 4. Render the exact raster

The `pixels()` result is the normative $16\times22$ binary raster. Render its
row-major `pixels` values as whole target pixels or integer-scaled squares;
`0` is background and `1` is foreground. See the complete
[exact-raster rules](../SPEC.md#10-exact-binary-renderer).

### 4.1 LVGL renderer

The optional `bitsquiggles_renderer_lvgl.py` module renders the exact raster as
a cached RGB565 image with integer scaling:

```python
import bitsquiggle32
import bitsquiggles_renderer_lvgl as lvgl_renderer

raster = bitsquiggle32.pixels(0x12345678, bitsquiggle32.HIGH_CONTRAST)
lvgl_renderer.render_raster(parent, raster, scale=2)
```

Call `clear_cache()` after deleting renderer parents.

## 5. Optional smooth rendering

Smooth rendering is presentation only: it must preserve selected and
unselected connections without changing the exact-raster identity. Follow the
[smooth-rendering constraints](../SPEC.md#11-smooth-renderer) and the renderer
naming convention in the [API mapping](../SPEC.md#12-reference-api-mapping).

### 5.1 LVGL renderer

The optional `bitsquiggles_renderer_lvgl.py` module supports LVGL targets
without adding an LVGL dependency to the core. Its rounded canvas presentation
uses `render_smooth()`:

```python
import bitsquiggle32
import bitsquiggles_renderer_lvgl as lvgl_renderer

visual = bitsquiggle32.spec(0x12345678, bitsquiggle32.HIGH_CONTRAST)
lvgl_renderer.render_smooth(parent, visual, scale=4)
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
