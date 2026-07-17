# BitSquiggles

BitSquiggles is an experimental visual encoding for comparing two already-derived
32-bit fingerprints. It turns the same value into the same compact pattern on
different devices, including devices with very small or monochrome displays.

The intended interaction is a side-by-side check: show the pattern on both
devices and look for a difference. A typical example is comparing a BIP-32
master-key fingerprint shown by a hardware wallet with the fingerprint shown
by its companion application.

The project provides dependency-free reference implementations for Java 17
and MicroPython-compatible Python. The algorithm and conformance requirements
are defined in [SPEC.md](SPEC.md); this README deliberately stays at the
project and design-rationale level.

## Why this project exists

Eight hexadecimal digits are compact but tiring to compare, especially on a
small screen. BitSquiggles explores whether a structured visual can make accidental
mismatches easier to notice without requiring color, antialiasing, or a large
display.

It is useful when all of the following are true:

- a protocol or application already has a meaningful 32-bit fingerprint;
- the same value can be displayed independently in two places;
- a person can inspect both displays at roughly the same time;
- the goal is convenient detection of accidental mismatch.

BitSquiggles does not decide what should be fingerprinted. Deriving the correct
32-bit input remains the caller's responsibility.

## Intended audience and uses

BitSquiggles is primarily for:

- hardware-wallet and companion-application developers;
- embedded-device developers working with low-resolution displays;
- researchers and designers experimenting with human comparison of short
  fingerprints;
- applications that want an additional visual cue alongside the underlying
  fingerprint.

The patterns are designed for equality comparison, preferably side by side.
They are not designed for recognizing an identity from memory or finding one
identity in a large collection.

## Non-goals and unsafe uses

### Not protection against a deliberate attacker

The input contains only 32 bits. A targeted collision is computationally
feasible, regardless of how those bits are displayed. BitSquiggles is not a
cryptographic authentication mechanism and must not be treated as one.

A random value matches one fixed 32-bit value with probability $1/2^{32}$.
That may be useful for detecting accidents, but it is not an adequate security
boundary against an attacker who can search for inputs.

### Not a replacement for complete identifiers

Do not reduce a Bitcoin address, payment destination, public key, transaction,
or other long identifier to 32 bits and then use BitSquiggles as the authorization
decision. Different identifiers can have the same 32-bit fingerprint and will
then correctly produce the same pattern.

Payment destinations and other security-sensitive identifiers still require
an appropriate exact or authenticated comparison of the complete value. A
BitSquiggle32 can only be an additional cue.

### Not a hash, checksum, or fingerprint derivation function

BitSquiggle32 accepts an unsigned 32-bit value. It does not:

- accept arbitrary strings or byte arrays;
- derive BIP-32 or other protocol fingerprints;
- prove possession of a key;
- add information that was discarded before the value reached BitSquiggle32;
- provide cryptographic collision resistance.

## Design assumptions and choices

The design is guided by the following assumptions. These are rationale, not a
substitute for the normative rules in [SPEC.md](SPEC.md).

### Geometry carries identity

The pattern must remain unambiguous when rendered without color. Hue, chroma,
and lightness are therefore redundant comparison cues rather than part of the
uniqueness argument. This also limits the effect of display clipping,
desaturation, quantization, or inversion.

### Structure is easier to compare than visual noise

The visualization uses connections in a small cell grid and several structured
copy families. Connections provide enough capacity to retain the full input,
while reflection-like, rotation-like, and diagonal copy structures give the
eye larger features to compare. Independent ternary cell states were rejected
because their small subpatterns were difficult to distinguish on real
low-resolution displays.

### Diffusion must not discard information

Nearby numeric inputs should not lead to nearby-looking outputs. BitSquiggle32 uses
a reversible 32-bit mixer rather than a many-to-one hash: it improves avalanche
while preserving the size and uniqueness of the input domain.

### Invisible metadata cannot establish uniqueness

The internal copy-family choice is not printed into the pattern, and different
families can produce the same geometry. BitSquiggle32 resolves such overlaps by a
canonical priority rule and a full-capacity fallback. Uniqueness is claimed for
the visible connection geometry, not for a hidden mode label.

### A tiny exact rendering is the portability baseline

The conformance representation is a fixed binary raster with no antialiasing.
Each connection has dedicated pixels, allowing the abstract geometry to be
recovered from the raster. Larger smooth renderings are presentation options;
they do not redefine the encoded value.

### Reference implementations should be easy to audit and port

The Java and MicroPython implementations use no third-party runtime
dependencies. Both follow the same specification and share a conformance
vector. This favors transparent, portable code over framework integration.

## History

BitSquiggles was inspired by
[Hallmarks](https://github.com/GBKS/hallmarks). Early experiments sought more
visual diversity on small screens and clearer patterns in monochrome pixel
renderings.

The initial idea was broader address verification. Discussion and prototyping
made the underlying limitation clear: a 32-bit visual cannot securely stand in
for a full address, and secure address verification also needs a trustworthy
communication or authentication path. The project was consequently narrowed
to convenient comparison of an already-defined short fingerprint.

Further iterations moved information from independent cells to connections,
introduced reversible mixing, and added canonical handling of overlapping copy
families.

## Status

BitSquiggles is **experimental and unreleased**.

Current state:

- Java 17 and MicroPython-compatible implementations are present;
- both implementations include dependency-free test suites;
- the implementations share a documented conformance vector;
- uniqueness of the canonical connection mask is supported by a structural
  proof, while tests sample the implementation over large input sets;
- the Java implementation includes an interactive Swing demo;
- no independent security, cryptographic, accessibility, or usability review
  has been completed;
- no controlled user study has established how reliably people notice
  differences;
- smooth scaling, display defects, and human perception can still make two
  distinct patterns difficult to distinguish.

The mathematical uniqueness property should not be confused with a usability
or security guarantee.

## Rendered examples

Each sheet below is generated directly from the Java reference implementation.
The three columns are Standard, High Contrast, and Monochrome. Every column
contains an 80×110 smooth rendering above its native, unscaled 16×22 pixel
raster. The color changes between styles; the encoded geometry does not.

**Try any value in the [interactive playground](https://maggo83.github.io/BitSquiggles/).**
It runs entirely in the browser and creates a shareable link for each value.

| Input | Representative behavior | Rendered styles and native rasters |
| --- | --- | --- |
| `00000000` | Accepted half-turn (`A+`) | ![Input 00000000 in Standard, High Contrast, and Monochrome](docs/examples/00000000.svg) |
| `00000001` | Half-turn capacity fallback to `A\|` | ![Input 00000001 in Standard, High Contrast, and Monochrome](docs/examples/00000001.svg) |
| `00000003` | Accepted slash copy (`A/`) | ![Input 00000003 in Standard, High Contrast, and Monochrome](docs/examples/00000003.svg) |
| `00000004` | Accepted top/bottom copy (`A-`) | ![Input 00000004 in Standard, High Contrast, and Monochrome](docs/examples/00000004.svg) |
| `12345678` | Sparse left/right output (`A\|`) | ![Input 12345678 in Standard, High Contrast, and Monochrome](docs/examples/12345678.svg) |
| `ffffffff` | Denser left/right output (`A\|`) | ![Input ffffffff in Standard, High Contrast, and Monochrome](docs/examples/ffffffff.svg) |

### Keeping examples synchronized

The SVG sheets are generated files, not hand-maintained screenshots. After
cloning, enable the repository hook once:

```bash
git config core.hooksPath .githooks
```

The hook regenerates and stages the gallery during each commit. To regenerate
it manually after changing rendering behavior, run:

```bash
mkdir -p out
javac -d out java/module-info.java java/bitsquiggles/*.java
java -cp out bitsquiggles.GalleryGenerator
java -cp out bitsquiggles.ConformanceFixtureGenerator
```

GitHub Actions also verifies the gallery on every push and pull request. The
`Verify generated gallery` workflow should be configured as a required status
check for `main`, so stale documentation cannot be merged if a local hook was
not enabled.

## Include, use, render, and test

Every implementation exposes the same two layers:

- `spec` / `visualize` returns the connection graph, display colors, selected
  mode, and fallback metadata;
- `pixels` returns the exact $16\times22$ binary raster plus foreground and
  background colors.

Use the raster when equality comparison must remain exact. A renderer may scale
each raster pixel by an integer factor, but it must not antialias, interpolate,
or alter selected pixels. The smooth Swing and browser renderings are optional
presentation layers; they do not define the encoded value.

### Java 17+

#### Include and use

For source-level use, copy `java/module-info.java` and
`java/bitsquiggles/BitSquiggle32.java` into a Java 17+ project. The source-only
module is named `io.github.maggo83.bitsquiggles`; it has no third-party
dependencies. Java accepts every 32-bit bit pattern through `int`; parse and
format values as unsigned where needed.

```java
import bitsquiggles.BitSquiggle32;

int bits = (int) Long.parseLong("12345678", 16);
BitSquiggle32.VisSpec visual = BitSquiggle32.spec(bits);
BitSquiggle32.PixelGrid raster = BitSquiggle32.pixels(bits);

System.out.println(visual.actualMode().label());
System.out.println(raster.foreground().hex());
```

Arrays held by the Java records are mutable. Treat them as immutable outputs or
copy them before sharing them with untrusted code.

#### Render the exact raster

`raster.pixels()` is row-major, with `0` for background and `1` for foreground.
Render it directly with an integer `scale`:

```java
for (int y = 0; y < raster.height(); y++) {
    for (int x = 0; x < raster.width(); x++) {
        boolean on = raster.pixels()[y * raster.width() + x] != 0;
        // Fill one scale-by-scale rectangle using raster foreground/background.
    }
}
```

`BitSquigglesDemo` is a Swing presentation example for inspecting smooth and
exact raster output; it is not required by the core API.

#### Compile and test

```bash
mkdir -p out
javac -d out java/module-info.java java/bitsquiggles/*.java
java --module-path out --module io.github.maggo83.bitsquiggles/bitsquiggles.BitSquiggle32Test
java -cp out bitsquiggles.BitSquigglesDemo
```

The test harness exercises the core properties. To verify checked-in generated
files, also run `GalleryGenerator --check` and
`ConformanceFixtureGenerator --check` as shown in [Keeping examples
synchronized](#keeping-examples-synchronized).

### MicroPython-compatible Python

#### Include and use

For MicroPython, copy [micropython/bitsquiggle32.py](micropython/bitsquiggle32.py)
to the target and import it as `bitsquiggle32`. For CPython, install the same
dependency-free module from a checkout:

```bash
python3 -m pip install .
```

```python
import bitsquiggle32

visual = bitsquiggle32.spec(0x12345678)
raster = bitsquiggle32.pixels(0x12345678)
print(visual["actual_mode"], raster["foreground"]["hex"])
```

`raster["pixels"]` is a row-major `bytearray`; render each `0` or `1` as one
background or foreground cell. The `width` and `height` fields are always `16`
and `22` for BitSquiggle32.

#### Test

```bash
cd micropython
python3 test_bitsquiggle32.py
```

Native MicroPython targets vary in available RAM. The sampled uniqueness tests
may need reduced iteration counts on constrained devices; the runtime encoder
itself uses a small fixed working set.

### JavaScript / TypeScript

#### Include and use

The browser core in [web/bitsquiggle32.js](web/bitsquiggle32.js) is a plain ESM
package with no runtime dependencies or build step. After publication, install
`bitsquiggles` and import it as ESM. Until then, import the local module by its
relative path.

```js
import { parseHex, visualize } from "bitsquiggles";

const visual = visualize(parseHex("12345678"));
console.log(visual.actualMode, visual.foreground);
```

The accompanying `bitsquiggle32.d.ts` supplies optional TypeScript editor
support; JavaScript remains the executable source.

#### Render the exact raster

`visual.pixels` is a `Uint8Array` in row-major order. This canvas example keeps
the native raster exact:

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

Scale the canvas only with nearest-neighbor pixel rendering when a larger
display is needed.

#### Test and inspect the package

From the `web/` directory, run `npm pack --dry-run` to inspect the publishable
artifact and `npm test` to run all shared conformance vectors.

## Repository guide

```text
README.md                  project purpose, audience, rationale, and status
SPEC.md                    normative behavior and implementation details
java/bitsquiggles/
  BitSquiggle32.java       Java reference implementation
  BitSquiggle32Test.java   Java conformance and property tests
  BitSquigglesDemo.java    Java Swing demonstration
  GalleryGenerator.java    Deterministic README example-sheet generator
  ConformanceFixtureGenerator.java Java-generated cross-language test fixtures
java/module-info.java       Source-only Java module descriptor
micropython/
  bitsquiggle32.py         MicroPython-compatible implementation
  test_bitsquiggle32.py    Python conformance and property tests
docs/examples/             Generated README example sheets
fixtures/v1.json           Versioned cross-language conformance fixture
pyproject.toml              CPython package metadata for `bitsquiggle32`
web/                       Static GitHub Pages playground and parity tests
.githooks/pre-commit       Regenerates and stages example sheets locally
.github/workflows/         Verifies generated files, runs tests, and deploys Pages
```

When behavior, constants, or formats change, update [SPEC.md](SPEC.md) and the
conformance tests together. When rendering changes, regenerate
`docs/examples/` and `fixtures/v1.json` with their Java generators as
described above. Keep project motivation and trade-offs here so the same
technical rules do not have to be maintained in two documents.

## License

The files retain the Grug 2-Clause license:

1. do what want
2. not sue grug
