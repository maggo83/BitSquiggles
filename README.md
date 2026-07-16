# Bit32Vis

Bit32Vis is an experimental visual encoding for comparing two already-derived
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
small screen. Bit32Vis explores whether a structured visual can make accidental
mismatches easier to notice without requiring color, antialiasing, or a large
display.

It is useful when all of the following are true:

- a protocol or application already has a meaningful 32-bit fingerprint;
- the same value can be displayed independently in two places;
- a person can inspect both displays at roughly the same time;
- the goal is convenient detection of accidental mismatch.

Bit32Vis does not decide what should be fingerprinted. Deriving the correct
32-bit input remains the caller's responsibility.

## Intended audience and uses

Bit32Vis is primarily for:

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
feasible, regardless of how those bits are displayed. Bit32Vis is not a
cryptographic authentication mechanism and must not be treated as one.

A random value matches one fixed 32-bit value with probability $1/2^{32}$.
That may be useful for detecting accidents, but it is not an adequate security
boundary against an attacker who can search for inputs.

### Not a replacement for complete identifiers

Do not reduce a Bitcoin address, payment destination, public key, transaction,
or other long identifier to 32 bits and then use Bit32Vis as the authorization
decision. Different identifiers can have the same 32-bit fingerprint and will
then correctly produce the same pattern.

Payment destinations and other security-sensitive identifiers still require
an appropriate exact or authenticated comparison of the complete value. A
Bit32Vis pattern can only be an additional cue.

### Not a hash, checksum, or fingerprint derivation function

Bit32Vis accepts an unsigned 32-bit value. It does not:

- accept arbitrary strings or byte arrays;
- derive BIP-32 or other protocol fingerprints;
- prove possession of a key;
- add information that was discarded before the value reached Bit32Vis;
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
while symmetry, rotation-like structure, and anti-copy structure give the eye
larger features to compare. Independent ternary cell states were rejected
because their small subpatterns were difficult to distinguish on real
low-resolution displays.

### Diffusion must not discard information

Nearby numeric inputs should not lead to nearby-looking outputs. Bit32Vis uses
a reversible 32-bit mixer rather than a many-to-one hash: it improves avalanche
while preserving the size and uniqueness of the input domain.

### Invisible metadata cannot establish uniqueness

The internal copy-family choice is not printed into the pattern, and different
families can produce the same geometry. Bit32Vis resolves such overlaps by a
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

Bit32Vis was inspired by
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

Bit32Vis is **experimental and unreleased**.

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

## Trying the reference implementations

### Java 17+

From the repository root:

```bash
mkdir -p out
javac -d out java/bit32vis/*.java
java -Xmx512m -cp out bit32vis.Bit32VisTest
java -cp out bit32vis.DemoApp
```

The API entry points are `Bit32Vis.spec(...)` for the abstract visual and
`Bit32Vis.pixels(...)` for the exact binary raster. Java accepts every 32-bit
pattern through `int`; format it as unsigned when displaying it. Arrays inside
the returned records are mutable and should be treated as immutable outputs or
copied before being shared with untrusted code.

### MicroPython-compatible Python

Copy [micropython/bit32vis.py](micropython/bit32vis.py) to the target and import
it as `bit32vis`. The corresponding entry points are `spec(...)` and
`pixels(...)`.

Run the tests under CPython or MicroPython:

```bash
cd micropython
python3 test_bit32vis.py
```

Native MicroPython targets vary in available RAM. The sampled uniqueness tests
may need reduced iteration counts on constrained devices; the runtime encoder
itself uses a small fixed working set.

## Repository guide

```text
README.md                  project purpose, audience, rationale, and status
SPEC.md                    normative behavior and implementation details
java/bit32vis/
  Bit32Vis.java            Java reference implementation
  Bit32VisTest.java        Java conformance and property tests
  DemoApp.java             Java Swing demonstration
micropython/
  bit32vis.py              MicroPython-compatible implementation
  test_bit32vis.py         Python conformance and property tests
```

When behavior, constants, or formats change, update [SPEC.md](SPEC.md) and the
conformance tests together. Keep project motivation and trade-offs here so the
same technical rules do not have to be maintained in two documents.

## License

The files retain the Grug 2-Clause license used during the experiment:

1. do what want
2. not sue grug
