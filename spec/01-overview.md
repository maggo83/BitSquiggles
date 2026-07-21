# BitSquiggle32 overview

**Normative.** This chapter defines the externally observable contract and the
coarse processing model. Read [the specification index](../SPEC.md) first;
read [encoding](02-encoding.md) for the exact transformation.

## Scope and contract

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

The caller supplies the 32-bit integer. BitSquiggle32 does not define how a
Bitcoin fingerprint or any other protocol value is derived. Hexadecimal
notation is most-significant-digit first; `89abcdef` denotes the integer
`0x89abcdef`. The encoder consumes an integer and otherwise defines no byte
order.

## Observable outputs

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

## Coarse processing model

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

## Related

- Next: [encoding](02-encoding.md)
- Output data and styles: [presentation](03-presentation.md)
- Exact display format: [exact raster](04-exact-raster.md)
