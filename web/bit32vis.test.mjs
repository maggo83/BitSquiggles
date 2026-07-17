import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { EDGES, formatHex, parseHex, pixels, visualize } from "./bit32vis.js";

const fixtures = JSON.parse(await readFile(new URL("./fixtures.json", import.meta.url), "utf8"));
assert.equal(fixtures.format, 1, "known fixture format");
assert.equal(EDGES.length, 58, "canonical edge count");

for (const vector of fixtures.vectors) {
  const input = Number.parseInt(vector.input, 16) | 0;
  const visual = visualize(input);
  assert.equal(formatHex(visual.mixed).toLowerCase(), vector.mixed, `mixed value for ${vector.input}`);
  assert.equal([...visual.connections].join(""), vector.connections, `connections for ${vector.input}`);
  assert.equal(visual.preferredMode, vector.preferredMode, `preferred mode for ${vector.input}`);
  assert.equal(visual.actualMode, vector.actualMode, `actual mode for ${vector.input}`);
  assert.equal(visual.fallback, vector.fallback, `fallback state for ${vector.input}`);
  assert.equal([...visual.pixels].join(""), [...pixels(visual.connections)].join(""), `raster for ${vector.input}`);
  for (const [style, [background, foreground]] of Object.entries(vector.styles)) {
    const styled = visualize(input, style);
    assert.equal(styled.background, background, `${style} background for ${vector.input}`);
    assert.equal(styled.foreground, foreground, `${style} foreground for ${vector.input}`);
    assert.deepEqual([...styled.connections], [...visual.connections], `${style} preserves geometry for ${vector.input}`);
  }
}

assert.equal(parseHex("0xFFFFFFFF"), -1, "parse unsigned maximum");
assert.equal(formatHex(-1), "FFFFFFFF", "format unsigned maximum");
assert.throws(() => parseHex("not hex"), /hexadecimal/, "reject invalid hexadecimal");
console.log(`Bit32Vis browser tests passed (${fixtures.vectors.length} Java parity vectors)`);