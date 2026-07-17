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
It uses only core language features plus `math`.

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

`raster["pixels"]` is a row-major `bytearray`. `0` means background and `1`
means foreground. Render every element as one whole pixel or an integer-scaled
square:

```python
for y in range(raster["height"]):
    for x in range(raster["width"]):
        on = raster["pixels"][y * raster["width"] + x] != 0
        # Draw one foreground or background cell at x, y.
```

The exact dimensions and rendering rules are defined in the
[normative raster specification](../SPEC.md#10-exact-binary-renderer).

## 5. Optional smooth rendering

This port intentionally contains no graphics-framework renderer. Use the exact
raster on constrained displays, or build a target-specific smooth renderer from
the `spec()` connection/cell data.

For CPython applications, suitable optional renderer targets include Pillow for
image output, Tkinter for a standard-library desktop canvas, and pygame for an
interactive display. None is required or bundled, so the core stays
MicroPython-compatible. Follow the shared
[smooth-rendering constraints](../SPEC.md#11-smooth-renderer) and renderer
naming convention in the [API mapping](../SPEC.md#12-reference-api-mapping).

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
| MicroPython testing | Target-dependent | Run the property harness on-device as resources permit; full fixture parity runs on the host. |

## 9. License

Grug 2-Clause License. See the repository-level [LICENSE](../LICENSE).
