# BitSquiggle32 for Python and MicroPython

## 1. Status and scope

This dependency-free module is the BitSquiggle32 reference implementation for
CPython and MicroPython-compatible environments. It produces the canonical
visual specification and exact $16\times22$ binary raster. It does not derive
fingerprints or make security decisions from them.

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

`spec()` returns a dictionary containing the input, mixed value, connections,
active cells, colors, style, mode/fallback metadata, and polarity metadata.
`pixels()` returns the exact raster and its colors. Inputs must be `int` values
from `0` through `0xffffffff`; invalid inputs and unknown styles raise
`ValueError`.

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

Do not antialias, interpolate, or alter raster pixels when exact comparison is
required.

## 5. Optional smooth rendering

This port intentionally contains no graphics-framework renderer. Use the exact
raster on constrained displays, or build a target-specific smooth renderer from
the `spec()` connection/cell data. A smooth renderer is a presentation layer and
must not redefine the canonical connection mask or raster.

## 6. Test conformance

Run the dependency-free test harness under CPython or MicroPython:

```bash
cd micropython
python3 test_bitsquiggle32.py
```

Repository CI runs this harness alongside checks for the Java-generated
versioned conformance fixture. Direct fixture consumption by this port is a
future hardening step.

## 7. Package / release notes

`pyproject.toml` packages `bitsquiggle32.py` for CPython while retaining the
same standalone source file for MicroPython vendoring. The distribution has no
runtime dependencies.

## 8. Limitations and compatibility

The module accepts only unsigned 32-bit integer inputs. Native MicroPython
targets vary in available RAM; reduce long sampled test loops on constrained
devices if needed. Callers remain responsible for deriving the fingerprint and
for displaying any numeric value as unsigned where appropriate.

## 9. License

Grug 2-Clause License. See the repository-level [LICENSE](../LICENSE).
