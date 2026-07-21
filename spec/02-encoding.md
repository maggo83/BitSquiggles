# BitSquiggle32 encoding

**Normative.** This chapter defines the complete unsigned uint32-to-connection-
mask transformation, including the uniqueness proof. Read [the overview](01-overview.md)
first. Presentation rules begin in [presentation](03-presentation.md).

## Fixed dimensions, values, and ordering

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

## Bijective mixer

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
invertible. The complete mixer is therefore a permutation of the 32-bit domain.

## Connection classes and copy templates

Each template labels the 7×5 physical cells. An unprimed label identifies a
source cell; a primed label is a copy of that source.

For every physical edge:

1. remove primes from both endpoint labels;
2. locate the unprimed source coordinate for each endpoint;
3. sort the two source coordinates lexicographically;
4. use the coordinate pair as the connection-class key.

Sort connection classes by that key. All physical edges with one key receive
the same free connection bit.

### Left/right (`A|`)

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

### Top/bottom (`A-`)

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

### Half-turn (`A+`)

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

### Slash copy (`A/`)

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

## Preferred modes and class-bit assignment

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

## Family membership, canonical priority, and fallback

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

Capacity fallback from the preceding section is applied in addition to this
overlap rule.

### Exact membership test

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

### Uniqueness proof

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

The exact raster is also injective because [the exact raster](04-exact-raster.md)
assigns dedicated bridge pixels from which all 58 edge bits can be recovered.
Color and metadata are not used in either argument.

For symbolic analysis, each complete family may equivalently be represented as
the affine binary map:

```text
edgeMask = Ai × classBits XOR bi
```

Intersections can then be counted by solving
`Ai × x XOR Aj × y = bi XOR bj` over `GF(2)`. Runtime encoding does not need
those counts; concrete membership tests are sufficient.

## Related

- Prerequisite: [overview](01-overview.md)
- Next: [presentation](03-presentation.md)
- Exact rendering: [exact raster](04-exact-raster.md)
- Public operations and helpers: [API contract](06-api.md)
- Required checks: [conformance](07-conformance.md)
