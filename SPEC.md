# BitSquiggles / BitSquiggle32 Specification

**Status:** Experimental, unreleased  
**Implementations:** Java 17 and MicroPython-compatible Python  
**License:** Grug 2-Clause License

This document is the technical source of truth for BitSquiggle32. It is organized
in two levels:

- **Part I** defines the externally observable contract and the coarse
  processing model.
- **Part II** defines the exact data layout, algorithms, rendering, APIs, and
  conformance requirements needed for an interoperable implementation.

Project motivation, audience, history, and design trade-offs are intentionally
kept in [README.md](README.md) rather than repeated here.

## Part I — System-level specification

### 1. Scope and contract

BitSquiggle32 maps one unsigned 32-bit integer to a deterministic visual
specification. The identity-bearing output is a binary mask over the canonical
connections of a fixed cell grid. An exact binary pixel rendering preserves
that mask without loss.

For conforming implementations:

```text
equal inputs   => equal connection masks and exact rasters
unequal inputs => unequal connection masks and exact rasters
```

The guarantee applies to the abstract connection mask and the exact binary
raster. It does not assert that every pair will be easy for a person to
distinguish after arbitrary scaling, smoothing, display degradation, or brief
observation.

The caller supplies the 32-bit integer. BitSquiggle32 does not define how a Bitcoin
fingerprint or any other protocol value is derived. Hexadecimal notation is
most-significant-digit first; `89abcdef` denotes the integer `0x89abcdef`.
The encoder consumes an integer and otherwise defines no byte order.

### 2. Observable outputs

An abstract visual specification contains:

- the original and mixed 32-bit values;
- a 58-bit canonical connection mask;
- the active state of each cell, derived from the mask;
- foreground and background colors;
- the requested rendering style;
- preferred-mode, actual-mode, fallback, luminance, and polarity metadata.

The exact renderer contains:

- a 16×22 binary foreground/background raster;
- the same foreground and background colors;
- the requested style.

The mode metadata is diagnostic. It is not rendered into the geometry and is
not required to decode or compare the identity-bearing mask.

### 3. Coarse processing model

A conforming encoder performs these conceptual stages:

1. **Normalize the input.** Interpret it as an unsigned 32-bit integer.
2. **Diffuse it.** Apply a bijective 32-bit mixer so nearby inputs normally
   affect many output features without creating collisions.
3. **Construct a candidate.** Use mixed bits to choose a copy family and fill
   its independent connection classes.
4. **Canonicalize overlaps.** Reject a non-default candidate that also belongs
   to an earlier family and encode the complete mixed value in the default
   family instead.
5. **Derive presentation.** Mark cells incident to selected connections and
   derive optional color cues. Geometry is identical in every style.
6. **Render if requested.** Place cells, bridges, and closed junctions in the
   exact binary raster.

The default family has capacity for all 32 mixed bits. The two-bit selector
leaves a 30-bit payload. Non-default families preserve that payload directly
when they have sufficient capacity. The 29-class half-turn family accepts only
the half of its payloads that fit and sends the others to the full-capacity
fallback. Canonical priority makes accepted family ranges disjoint.

## Part II — Detailed implementation specification

### 4. Fixed dimensions, values, and ordering

All input and mixer arithmetic is modulo $2^{32}$.

| Item | Required value |
| --- | ---: |
| Grid rows | 7 |
| Grid columns | 5 |
| Horizontal edges | 28 |
| Vertical edges | 30 |
| Total edges | 58 |
| Exact raster width | 16 |
| Exact raster height | 22 |

Rows are numbered `0…6` and columns `0…4`. A canonical edge is the tuple:

```text
(startRow, startColumn, endRow, endColumn)
```

Only orthogonal nearest neighbours are valid:

```text
(row, column, row,     column + 1)  horizontal
(row, column, row + 1, column    )  vertical
```

Edges are ordered lexicographically by the tuple. Equivalently, iterate cells
in row-major order and emit the right edge before the down edge whenever that
edge exists. Connection arrays and bit strings use this order.

A cell is active if and only if at least one selected edge is incident to it.
Cells do not carry independent data bits.

### 5. Bijective mixer

Given `input`, calculate:

```text
mixed = input + 0x9e3779b9
mixed = (mixed XOR (mixed >>> 16)) × 0x85ebca6b
mixed = (mixed XOR (mixed >>> 13)) × 0xc2b2ae35
mixed =  mixed XOR (mixed >>> 16)
```

`>>>` is a logical right shift. Mask intermediate results to 32 bits where the
implementation language does not overflow naturally.

Both multipliers are odd, addition is reversible, and each XOR-shift is
invertible. The complete mixer is therefore a permutation of the 32-bit
domain.

### 6. Connection classes and copy templates

Each template labels the 7×5 physical cells. An unprimed label identifies a
source cell; a primed label is a copy of that source.

For every physical edge:

1. remove primes from both endpoint labels;
2. locate the unprimed source coordinate for each endpoint;
3. sort the two source coordinates lexicographically;
4. use the coordinate pair as the connection-class key.

Sort connection classes by that key. All physical edges with one key receive
the same free connection bit.

#### 6.1 Left/right (`A|`)

```text
A B C B' A'
D E F E' D'
G H I H' G'
J K L K' J'
M N O N' M'
P Q R Q' P'
S T U T' S'
```

Free connection classes: **32**.

#### 6.2 Top/bottom (`A-`)

```text
A  B  C  D  E
F  G  H  I  J
K  L  M  N  O
P  Q  R  S  T
K' L' M' N' O'
F' G' H' I' J'
A' B' C' D' E'
```

Free connection classes: **31**.

#### 6.3 Half-turn (`A+`)

```text
A  B  C  D  E
F  G  H  I  J
K  L  M  N  O
P  Q  R  Q' P'
O' N' M' L' K'
J' I' H' G' F'
E' D' C' B' A'
```

Free connection classes: **29**.

#### 6.4 Slash copy (`A/`)

```text
A  B  C  D  E
F  G  H  I  J
K  L  M  N  I'
O  P  Q  M' H'
R  S  P' L' G'
T  R' O' K' F'
E' D' C' B' A'
```

Free connection classes: **33**. The final row extends the copied diagonal
sequence and mirrors the top row in reverse. This remains a copy template, not
a geometric reflection of the non-square rectangle.

### 7. Preferred modes and class-bit assignment

Bits `31…30` of `mixed` select one of four preferred modes:

| Index | Mode | Free classes |
| ---: | --- | ---: |
| 0 | Left/right (`A\|`) | 32 |
| 1 | Top/bottom (`A-`) | 31 |
| 2 | Half-turn (`A+`) | 29 |
| 3 | Slash copy (`A/`) | 33 |

Bits `29…0` are the 30-bit payload. Assign them most significant first to the
first 30 sorted connection classes. If a template has further classes, wrap
around and reuse payload bits from the beginning:

```text
classBit[i] = payloadBit[29 - (i modulo 30)]
```

Thus top/bottom class 30 repeats class 0. Slash-copy classes 30…32 repeat
classes 0…2. This extends visible relationships between the template's upper
and lower regions without deriving any class value from a secondary
pseudo-random source.

The half-turn family has only 29 classes and cannot injectively represent all
$2^{30}$ payloads. If payload bit 0 is zero, assign payload bits `29…1` most
significant first to its 29 classes. If payload bit 0 is one, do not accept the
candidate: encode the complete mixed value in the default family and set
`fallback` to true. The omitted bit is therefore represented by the choice
between an eligible half-turn candidate and fallback rather than being lost.

Mode 0 is different: assign all 32 bits of `mixed`, most significant first,
directly to the 32 left/right classes. This is a bijection onto the complete
default family and is also the fallback encoding.

### 8. Family membership, canonical priority, and fallback

Mode families overlap. A family label cannot disambiguate them because it is
not part of the visible geometry. Apply this priority rule to every candidate
with preferred mode `i > 0`:

1. expand its classes into the canonical 58-edge mask;
2. test that mask against every complete family with index less than `i`;
3. accept it only when no earlier family matches;
4. otherwise encode the complete `mixed` value using mode 0 and set `fallback`
   to true.

An accepted candidate has `actualMode = preferredMode` and `fallback = false`.
A rejected candidate has `actualMode = A|` and `fallback = true`. Preferred
mode 0 directly uses `A|` and is not considered fallback.

Capacity fallback from section 7 is applied in addition to this overlap rule.

#### 8.1 Exact membership test

For a mode, let:

- `class(e)` identify the connection class of physical edge `e`;

Expansion is:

```text
edge[e] = classBit[class(e)]
```

The mask belongs to that complete family if and only if all occurrences in
each connection class are equal. One representative occurrence can be compared
with all remaining occurrences. Singleton classes impose no constraint. The
test is exact and linear in the 58 physical edges.

It is necessary to test all earlier families, not only an adjacent or
higher-index family: the earliest family owns every overlap in which it
participates.

#### 8.2 Uniqueness proof

Let `M` be the bijective mixer and `Fi` the complete mask family for mode `i`.

1. Mode-0 and fallback outputs encode all 32 mixed bits injectively in `F0`.
2. Every accepted non-default output is explicitly outside `F0`, so it cannot
   equal a mode-0 or fallback output.
3. Within top/bottom or slash copy, different inputs selecting that mode have
   different 30-bit payloads, all of which are present in the first 30 classes.
4. An accepted half-turn output has payload bit 0 equal to zero. Its remaining
   29 payload bits are present in its 29 classes; different accepted half-turn
   inputs therefore produce different masks. Half-turn inputs with payload bit
   0 equal to one use the injective default fallback.
5. For accepted modes `i < j`, every mode-`i` output belongs to `Fi`, while
   mode `j` rejects every candidate belonging to `Fi`. Their accepted outputs
   cannot coincide.

Because `M` is bijective, these cases establish:

```text
input1 != input2  =>  edgeMask1 != edgeMask2
```

The exact raster is also injective because section 10 assigns dedicated bridge
pixels from which all 58 edge bits can be recovered. Color and metadata are not
used in either argument.

For symbolic analysis, each complete family may equivalently be represented
as the affine binary map:

```text
edgeMask = Ai × classBits XOR bi
```

Intersections can then be counted by solving
`Ai × x XOR Aj × y = bi XOR bj` over `GF(2)`. Runtime encoding does not need
those counts; concrete membership tests are sufficient.

### 9. Cell and color derivation

After the final connection mask is known, mark both endpoints of every selected
edge as active. All other cells are inactive.

Extract color indices from `mixed`:

```text
hueIndex       = bits 15…12
chromaIndex    = bits 11…8
luminanceIndex = bits 1…0
```

Calculate:

```text
hue    = hueIndex × (360 / 16)
chroma = 0.05 + chromaIndex × ((0.25 - 0.05) / 15)
baseL  = 0.50 + luminanceIndex × ((0.70 - 0.50) / 3)
```

Derive ordered OKLCH components by style:

| Style | Foreground L | Background L | Foreground C | Background C |
| --- | --- | --- | --- | --- |
| Standard | `baseL` | `foregroundL - 0.50` | `chroma` | `chroma` |
| High contrast | `baseL + 0.30` | `foregroundL - 0.80` | `chroma + 0.10` | `chroma + 0.10` |
| Monochrome | `baseL + 0.30` | `foregroundL - 0.80` | `0` | `0` |

Clamp both lightness values to `[0,1]`. Foreground hue is `hue`; background hue
is `(hue + 180) modulo 360`.

Calculate the XOR parity of all bits in the original, unmixed input. If the
parity is odd, swap the foreground and background lightness values. Do not swap
hue or chroma. The returned `swapped` field reports this operation.

#### 9.1 OKLCH to sRGB

For `(L,C,h)`, with `h` in degrees:

```text
a  = C × cos(h × pi / 180)
b  = C × sin(h × pi / 180)
l_ = L + 0.3963377774 × a + 0.2158037573 × b
m_ = L - 0.1055613458 × a - 0.0638541728 × b
s_ = L - 0.0894841775 × a - 1.2914855480 × b

rLinear =  4.0767416621 × l_^3 - 3.3077115913 × m_^3 + 0.2309699292 × s_^3
gLinear = -1.2684380046 × l_^3 + 2.6097574011 × m_^3 - 0.3413193965 × s_^3
bLinear = -0.0041960863 × l_^3 - 0.7034186147 × m_^3 + 1.7076147010 × s_^3
```

Clamp each linear component to `[0,1]`, then encode it with:

```text
srgb(v) = 12.92 × v                            when v <= 0.0031308
          1.055 × v^(1 / 2.4) - 0.055         otherwise
```

Multiply by 255 and round to the nearest integer, rounding exact half values
upward. Text colors use lowercase `#rrggbb`.

### 10. Exact binary renderer

The normative raster is 16×22:

```text
width  = 1 border + 5 × 2 cell pixels + 4 × 1 gaps + 1 border = 16
height = 1 border + 7 × 2 cell pixels + 6 × 1 gaps + 1 border = 22
```

Cell `(row,column)` begins at:

```text
x = 1 + 3 × column
y = 1 + 3 × row
```

Rendering rules:

1. initialize every pixel as background;
2. fill each active cell's 2×2 block with foreground;
3. for a selected horizontal edge, fill the 1×2 gap between its cells;
4. for a selected vertical edge, fill the 2×1 gap between its cells;
5. set the one-pixel junction between four cells only when all four perimeter
   edges of that cell square are selected;
6. leave every border pixel as background.

Each physical edge owns bridge pixels not owned by any other edge. Reading one
of those bridge pixels recovers that edge bit even when connected-component
membership alone would not reveal it.

### 11. Smooth renderer

Smooth rendering is presentation rather than the binary conformance format,
but it must preserve selected and unselected connections. Use the same 16×22
coordinate system at any scale:

1. draw the complete background tile with rounded outer corners;
2. represent every active 2×2 cell with corner radii equal to half the cell
   width, giving terminal cells semicircular caps;
3. overlay rectangular bridges for selected horizontal and vertical edges;
4. fill a four-cell center junction only when all four perimeter edges are
   selected;
5. combine all foreground cells, bridges, and junctions into one geometric
   union before antialiased rasterization.

The union avoids hairline background seams between overlapping components.
Smoothing must not close an unselected connection.

### 12. Reference API mapping

The language APIs expose the same conceptual operations and values. Their
container types and naming conventions differ.

#### 12.1 Java 17

Public static operations on `BitSquiggle32`:

| Operation | Result |
| --- | --- |
| `edges()` | canonical `Edge[]` |
| `freeConnectionCount(Mode)` | free-class count |
| `spec(int[, Style])` | `VisSpec` |
| `pixels(int[, Style])` | `PixelGrid` |

Java represents the unsigned input domain with all `int` bit patterns. Public
records are `Edge`, `OklchColor`, `VisSpec`, and `PixelGrid`; styles and modes
are enums. Arrays held by records remain mutable Java arrays. The reference
implementation also keeps `mix32(int)` and `matchesMode(byte[], Mode)` visible
to its package for conformance tests; they are not part of the public Java API.

#### 12.2 MicroPython-compatible Python

Public module operations:

| Operation | Result |
| --- | --- |
| `mix32(value)` | mixed integer |
| `free_connection_count(mode)` | free-class count |
| `matches_mode(connections, mode)` | complete-family membership |
| `spec(bits, style=STANDARD)` | visual dictionary |
| `pixels(bits, style=STANDARD)` | raster dictionary |

`bits` must be an `int` in the inclusive range `0…0xffffffff`; otherwise
`spec()` and `pixels()` raise `ValueError`. The style must be `standard`,
`high_contrast`, or `monochrome`; unknown styles raise `ValueError`.

The visual dictionary exposes `input`, `mixed`, `connections`, `cells`,
`background`, `foreground`, `style`, `preferred_mode`, `actual_mode`,
`fallback`, `luminance_index`, and `swapped`. The raster dictionary exposes
`width`, `height`, `pixels`, `background`, `foreground`, and `style`.

### 13. Conformance requirements

A conforming implementation must satisfy all of the following:

- produce exactly 58 canonical edges in the order from section 4;
- produce free-class counts `32, 31, 29, 33` in mode order;
- use mixed bits `31…30` as the selector and bits `29…0` as payload;
- implement cyclic payload reuse and half-turn capacity fallback exactly;
- derive no connection-class values from a secondary pseudo-random source;
- accept a non-default mask only when it belongs to no earlier family;
- encode all fallback masks in the complete default family;
- preserve one connection mask across all rendering styles;
- derive cells only from incident selected edges;
- produce the exact 16×22 raster with a background border;
- permit recovery of every connection from its bridge pixels;
- match the conformance vector in section 13.1.

Implementations should additionally test one-bit diffusion, observe every
preferred mode and fallback in representative samples, compare large sampled
sets for duplicate monochrome masks, and test invalid public inputs. Sampling
is implementation evidence, not the proof of complete-domain uniqueness.

#### 13.1 Conformance vector

For input `0x89abcdef` in Standard style:

```text
mixed         = 0x47ac5876
preferredMode = A-
actualMode    = A-
fallback      = false
luminance     = 2
background    = #140040
foreground    = #8d9200
connections   = 0001111010110001011000011101010010101100001010011011010011
```

The connection string uses the canonical 58-edge order from section 4.
