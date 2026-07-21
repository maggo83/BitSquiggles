# BitSquiggle32 conformance

**Normative.** This chapter defines required behavior checks and the base
conformance vector. Read [the API contract](06-api.md) and its linked output
chapters for the public and rendering requirements.

## Conformance requirements

A conforming implementation must satisfy all of the following:

- produce exactly 58 canonical edges in the [specified order](02-encoding.md#fixed-dimensions-values-and-ordering);
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
- match the conformance vector below.

Every implementation must produce the ordered smooth-blob decomposition defined
in [canonical blob extraction](05-smooth-output.md#canonical-blob-extraction)
and preserve the exact-raster and connection-mask results.

Implementations should additionally test one-bit diffusion, observe every
preferred mode and fallback in representative samples, compare large sampled
sets for duplicate monochrome masks, and test invalid public inputs. Sampling
is implementation evidence, not the proof of complete-domain uniqueness.

## Conformance vector

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

The connection string uses the canonical 58-edge order from
[encoding](02-encoding.md#fixed-dimensions-values-and-ordering).

## Generated fixture ownership

[fixtures/v1.json](../fixtures/v1.json) and the example assets in
[docs/examples/](../docs/examples/) are Java-generated tracked outputs. After a
relevant change, validate them with the Java generators using `--check` as
described in the [Java guide](../java/README.md#6-test-conformance). Target
README files own their target-specific test commands.

## Related

- Public operations: [API contract](06-api.md)
- Canonical algorithm: [encoding](02-encoding.md)
- Exact format: [exact raster](04-exact-raster.md)
- Smooth output: [smooth output](05-smooth-output.md)
