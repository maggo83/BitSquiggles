// BitSquiggle32 browser renderer — a tested port of the Java reference algorithm.
// Grug 2-Clause License: do what want; not sue grug.

export const ROWS = 7;
export const COLUMNS = 5;
export const EDGE_COUNT = 58;
export const PIXEL_WIDTH = 16;
export const PIXEL_HEIGHT = 22;

export const STYLES = ["standard", "high-contrast", "monochrome"];
export const MODES = ["A|", "A-", "A+", "A/"];

const TEMPLATES = [
  ["A B C B' A'", "D E F E' D'", "G H I H' G'", "J K L K' J'", "M N O N' M'", "P Q R Q' P'", "S T U T' S'"],
  ["A B C D E", "F G H I J", "K L M N O", "P Q R S T", "K' L' M' N' O'", "F' G' H' I' J'", "A' B' C' D' E'"],
  ["A B C D E", "F G H I J", "K L M N O", "P Q R Q' P'", "O' N' M' L' K'", "J' I' H' G' F'", "E' D' C' B' A'"],
  ["A B C D E", "F G H I J", "K L M N I'", "O P Q M' H'", "R S P' L' G'", "T R' O' K' F'", "E' D' C' B' A'"]
].map((rows) => rows.map((row) => row.split(" ")));

export const EDGES = createEdges();
const MODE_DEFINITIONS = TEMPLATES.map(createModeDefinition);

function createEdges() {
  const edges = [];
  for (let row = 0; row < ROWS; row += 1) {
    for (let column = 0; column < COLUMNS; column += 1) {
      if (column + 1 < COLUMNS) edges.push({ startRow: row, startColumn: column, endRow: row, endColumn: column + 1 });
      if (row + 1 < ROWS) edges.push({ startRow: row, startColumn: column, endRow: row + 1, endColumn: column });
    }
  }
  return edges;
}

function createModeDefinition(template) {
  const references = [];
  const sources = new Map();
  template.forEach((row, rowIndex) => row.forEach((token, columnIndex) => {
    const copied = token.endsWith("'");
    const name = copied ? token.slice(0, -1) : token;
    references[rowIndex * COLUMNS + columnIndex] = name;
    if (!copied) sources.set(name, rowIndex * COLUMNS + columnIndex);
  }));

  const classes = new Map();
  EDGES.forEach((edge, edgeIndex) => {
    const firstSource = sources.get(references[edge.startRow * COLUMNS + edge.startColumn]);
    const secondSource = sources.get(references[edge.endRow * COLUMNS + edge.endColumn]);
    const key = Math.min(firstSource, secondSource) * 64 + Math.max(firstSource, secondSource);
    if (!classes.has(key)) classes.set(key, []);
    classes.get(key).push(edgeIndex);
  });
  return [...classes.entries()].sort(([first], [second]) => first - second).map(([, occurrences]) => occurrences);
}

export function mix32(value) {
  let mixed = (value + 0x9e3779b9) | 0;
  mixed ^= mixed >>> 16;
  mixed = Math.imul(mixed, 0x85ebca6b);
  mixed ^= mixed >>> 13;
  mixed = Math.imul(mixed, 0xc2b2ae35);
  mixed ^= mixed >>> 16;
  return mixed | 0;
}

function expand(definition, values) {
  const connections = new Uint8Array(EDGES.length);
  definition.forEach((occurrences, classIndex) => occurrences.forEach((edgeIndex) => {
    connections[edgeIndex] = values[classIndex];
  }));
  return connections;
}

function encodeDefault(mixed) {
  const values = new Uint8Array(32);
  for (let index = 0; index < values.length; index += 1) values[index] = (mixed >>> (31 - index)) & 1;
  return expand(MODE_DEFINITIONS[0], values);
}

function encodeConnections(mixed, modeIndex) {
  if (modeIndex === 0) return encodeDefault(mixed);
  const definition = MODE_DEFINITIONS[modeIndex];
  const payload = mixed & 0x3fffffff;
  const values = new Uint8Array(definition.length);
  for (let index = 0; index < values.length; index += 1) values[index] = (payload >>> (29 - (index % 30))) & 1;
  return expand(definition, values);
}

function hasCapacity(mixed, modeIndex) {
  const missingBits = 30 - MODE_DEFINITIONS[modeIndex].length;
  return missingBits <= 0 || (mixed & ((1 << missingBits) - 1)) === 0;
}

function matchesModeIndex(connections, modeIndex) {
  return MODE_DEFINITIONS[modeIndex].every((occurrences) => occurrences.every(
    (edgeIndex) => connections[edgeIndex] === connections[occurrences[0]]
  ));
}

export function freeConnectionCount(mode) {
  const modeIndex = MODES.indexOf(mode);
  if (modeIndex < 0) throw new RangeError(`unknown mode: ${mode}`);
  return MODE_DEFINITIONS[modeIndex].length;
}

export function matchesMode(connections, mode) {
  const modeIndex = MODES.indexOf(mode);
  if (modeIndex < 0) throw new RangeError(`unknown mode: ${mode}`);
  return matchesModeIndex(connections, modeIndex);
}

function conflictsWithEarlierMode(connections, modeIndex) {
  for (let index = 0; index < modeIndex; index += 1) if (matchesModeIndex(connections, index)) return true;
  return false;
}

function activeCells(connections) {
  const cells = Array.from({ length: ROWS }, () => Array(COLUMNS).fill(0));
  connections.forEach((connected, index) => {
    if (!connected) return;
    const edge = EDGES[index];
    cells[edge.startRow][edge.startColumn] = 1;
    cells[edge.endRow][edge.endColumn] = 1;
  });
  return cells;
}

function clamp01(value) {
  return Math.max(0, Math.min(1, value));
}

function srgbEncode(value) {
  value = clamp01(value);
  return value <= 0.0031308 ? 12.92 * value : 1.055 * (value ** (1 / 2.4)) - 0.055;
}

function makeColor(lightness, chroma, hue) {
  const radians = hue * Math.PI / 180;
  const a = chroma * Math.cos(radians);
  const b = chroma * Math.sin(radians);
  const l = lightness + 0.3963377774 * a + 0.2158037573 * b;
  const m = lightness - 0.1055613458 * a - 0.0638541728 * b;
  const s = lightness - 0.0894841775 * a - 1.2914855480 * b;
  const l3 = l ** 3;
  const m3 = m ** 3;
  const s3 = s ** 3;
  const red = srgbEncode(4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3);
  const green = srgbEncode(-1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3);
  const blue = srgbEncode(-0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3);
  const hex = (channel) => Math.round(channel * 255).toString(16).padStart(2, "0");
  return `#${hex(red)}${hex(green)}${hex(blue)}`;
}

function colors(mixed, input, style) {
  const hue = ((mixed >>> 12) & 0xf) * 22.5;
  const chroma = 0.05 + ((mixed >>> 8) & 0xf) * (0.2 / 15);
  const baseL = 0.5 + (mixed & 0x3) * (0.2 / 3);
  let foregroundL = baseL;
  let backgroundL = foregroundL - 0.5;
  let foregroundC = chroma;
  let backgroundC = chroma;
  if (style === "high-contrast" || style === "monochrome") {
    foregroundL = baseL + 0.3;
    backgroundL = foregroundL - 0.8;
    foregroundC = style === "monochrome" ? 0 : chroma + 0.1;
    backgroundC = foregroundC;
  }
  if (popcount(input) % 2 === 1) [foregroundL, backgroundL] = [backgroundL, foregroundL];
  return {
    background: makeColor(backgroundL, backgroundC, (hue + 180) % 360),
    foreground: makeColor(foregroundL, foregroundC, hue)
  };
}

function popcount(value) {
  value >>>= 0;
  let count = 0;
  while (value) {
    count += value & 1;
    value >>>= 1;
  }
  return count;
}

function generatePixels(connections) {
  const raster = new Uint8Array(PIXEL_WIDTH * PIXEL_HEIGHT);
  const cells = activeCells(connections);
  const setPixel = (x, y) => { raster[y * PIXEL_WIDTH + x] = 1; };
  cells.forEach((row, rowIndex) => row.forEach((active, columnIndex) => {
    if (!active) return;
    const x = 1 + columnIndex * 3;
    const y = 1 + rowIndex * 3;
    setPixel(x, y); setPixel(x + 1, y); setPixel(x, y + 1); setPixel(x + 1, y + 1);
  }));
  connections.forEach((connected, index) => {
    if (!connected) return;
    const edge = EDGES[index];
    const x = 1 + edge.startColumn * 3;
    const y = 1 + edge.startRow * 3;
    if (edge.startRow === edge.endRow) {
      setPixel(x + 2, y); setPixel(x + 2, y + 1);
    } else {
      setPixel(x, y + 2); setPixel(x + 1, y + 2);
    }
  });
  for (let row = 0; row < ROWS - 1; row += 1) for (let column = 0; column < COLUMNS - 1; column += 1) {
    if (edgeValue(connections, row, column, row, column + 1)
        && edgeValue(connections, row + 1, column, row + 1, column + 1)
        && edgeValue(connections, row, column, row + 1, column)
        && edgeValue(connections, row, column + 1, row + 1, column + 1)) setPixel(3 + column * 3, 3 + row * 3);
  }
  return raster;
}

function edgeValue(connections, startRow, startColumn, endRow, endColumn) {
  const index = EDGES.findIndex((edge) => edge.startRow === startRow && edge.startColumn === startColumn
    && edge.endRow === endRow && edge.endColumn === endColumn);
  return connections[index];
}

export function spec(input, style = "standard") {
  if (!STYLES.includes(style)) throw new RangeError(`unknown style: ${style}`);
  input |= 0;
  const mixed = mix32(input);
  const preferredModeIndex = mixed >>> 30;
  const candidate = encodeConnections(mixed, preferredModeIndex);
  const fallback = preferredModeIndex !== 0 && (!hasCapacity(mixed, preferredModeIndex)
    || conflictsWithEarlierMode(candidate, preferredModeIndex));
  const actualModeIndex = fallback ? 0 : preferredModeIndex;
  const connections = fallback ? encodeDefault(mixed) : candidate;
  return {
    input,
    mixed,
    connections,
    cells: activeCells(connections),
    style,
    preferredMode: MODES[preferredModeIndex],
    actualMode: MODES[actualModeIndex],
    fallback,
    luminanceIndex: mixed & 0x3,
    swapped: popcount(input) % 2 === 1,
    ...colors(mixed, input, style)
  };
}

export function pixels(input, style = "standard") {
  const visual = spec(input, style);
  return {
    width: PIXEL_WIDTH,
    height: PIXEL_HEIGHT,
    pixels: generatePixels(visual.connections),
    background: visual.background,
    foreground: visual.foreground,
    style
  };
}

function horizontalEdgeBit(row, column) {
  const offset = row === ROWS - 1 ? row * (2 * COLUMNS - 1) + column : row * (2 * COLUMNS - 1) + 2 * column;
  return 1n << BigInt(offset);
}

function verticalEdgeBit(row, column) {
  const offset = column === COLUMNS - 1 ? 2 * COLUMNS - 2 : 2 * column + 1;
  return 1n << BigInt(row * (2 * COLUMNS - 1) + offset);
}

function junctionBit(row, column) {
  return 1n << BigInt(row * (COLUMNS - 1) + column);
}

function requiredEdgeMask(connections) {
  if (connections == null || connections.length !== EDGE_COUNT) throw new RangeError("connections must contain 58 entries");
  let result = 0n;
  for (let index = 0; index < EDGE_COUNT; index += 1) {
    if (connections[index] !== 0 && connections[index] !== 1) throw new RangeError("connections must contain only zeroes and ones");
    if (connections[index]) result |= 1n << BigInt(index);
  }
  return result;
}

function connectedRectangleEdgeMask(top, left, bottom, right, requiredEdges) {
  let result = 0n;
  for (let row = top; row <= bottom; row += 1) {
    for (let column = left; column <= right; column += 1) {
      if (column < right) {
        const edge = horizontalEdgeBit(row, column);
        if ((requiredEdges & edge) === 0n) return 0n;
        result |= edge;
      }
      if (row < bottom) {
        const edge = verticalEdgeBit(row, column);
        if ((requiredEdges & edge) === 0n) return 0n;
        result |= edge;
      }
    }
  }
  return result;
}

function requiredJunctionMask(requiredEdges) {
  let result = 0n;
  for (let row = 0; row < ROWS - 1; row += 1) {
    for (let column = 0; column < COLUMNS - 1; column += 1) {
      if (connectedRectangleEdgeMask(row, column, row + 1, column + 1, requiredEdges) !== 0n) {
        result |= junctionBit(row, column);
      }
    }
  }
  return result;
}

function rectangleJunctionMask(top, left, bottom, right, requiredJunctions) {
  let result = 0n;
  for (let row = top; row < bottom; row += 1) {
    for (let column = left; column < right; column += 1) {
      const junction = junctionBit(row, column);
      if ((requiredJunctions & junction) !== 0n) result |= junction;
    }
  }
  return result;
}

function popcountBigInt(value) {
  let count = 0;
  while (value) {
    value &= value - 1n;
    count += 1;
  }
  return count;
}

function isBetterBlob(candidate, best, newEdges, newJunctions, bestNewEdges, bestNewJunctions) {
  const junctionCount = popcountBigInt(newJunctions);
  const bestJunctionCount = popcountBigInt(bestNewJunctions);
  if (junctionCount !== bestJunctionCount) return junctionCount > bestJunctionCount;
  const edgeCount = popcountBigInt(newEdges);
  const bestEdgeCount = popcountBigInt(bestNewEdges);
  if (edgeCount !== bestEdgeCount) return edgeCount > bestEdgeCount;
  const area = (candidate.bottomRow - candidate.topRow + 1) * (candidate.rightColumn - candidate.leftColumn + 1);
  const bestArea = (best.bottomRow - best.topRow + 1) * (best.rightColumn - best.leftColumn + 1);
  if (area !== bestArea) return area < bestArea;
  if (candidate.topRow !== best.topRow) return candidate.topRow < best.topRow;
  if (candidate.leftColumn !== best.leftColumn) return candidate.leftColumn < best.leftColumn;
  if (candidate.bottomRow !== best.bottomRow) return candidate.bottomRow < best.bottomRow;
  return candidate.rightColumn < best.rightColumn;
}

function firstUncoveredEdge(requiredEdges, coveredEdges) {
  const remaining = requiredEdges & ~coveredEdges;
  for (let index = 0; index < EDGE_COUNT; index += 1) if ((remaining & (1n << BigInt(index))) !== 0n) return index;
  return -1;
}

function firstUncoveredJunction(requiredJunctions, coveredJunctions) {
  const remaining = requiredJunctions & ~coveredJunctions;
  for (let index = 0; index < (ROWS - 1) * (COLUMNS - 1); index += 1) if ((remaining & (1n << BigInt(index))) !== 0n) return index;
  return -1;
}

/** Return ordered canonical rounded-rectangle blobs for a connection mask. */
export function smoothBlobs(connections) {
  const requiredEdges = requiredEdgeMask(connections);
  const requiredJunctions = requiredJunctionMask(requiredEdges);
  let coveredEdges = 0n;
  let coveredJunctions = 0n;
  const result = [];

  while (coveredEdges !== requiredEdges || coveredJunctions !== requiredJunctions) {
    const anchorEdge = firstUncoveredEdge(requiredEdges, coveredEdges);
    const anchorJunction = anchorEdge < 0 ? firstUncoveredJunction(requiredJunctions, coveredJunctions) : -1;
    let anchorTop;
    let anchorLeft;
    let anchorBottom;
    let anchorRight;
    if (anchorEdge >= 0) {
      const edge = EDGES[anchorEdge];
      ({ startRow: anchorTop, startColumn: anchorLeft, endRow: anchorBottom, endColumn: anchorRight } = edge);
    } else {
      anchorTop = Math.floor(anchorJunction / (COLUMNS - 1));
      anchorLeft = anchorJunction % (COLUMNS - 1);
      anchorBottom = anchorTop + 1;
      anchorRight = anchorLeft + 1;
    }

    let best = null;
    let bestEdges = 0n;
    let bestJunctions = 0n;
    let leftInclusive = 0;
    for (let top = anchorTop; top >= 0; top -= 1) {
      for (let left = anchorLeft; left >= leftInclusive; left -= 1) {
        let rightExclusive = COLUMNS;
        let stopLeftExpansion = false;
        for (let bottom = anchorBottom; bottom < ROWS; bottom += 1) {
          for (let right = anchorRight; right < rightExclusive; right += 1) {
            const edges = connectedRectangleEdgeMask(top, left, bottom, right, requiredEdges);
            if (edges === 0n) {
              if (bottom === anchorBottom && right === anchorRight) {
                leftInclusive = left + 1;
                stopLeftExpansion = true;
              } else {
                rightExclusive = right;
              }
              break;
            }
            const junctions = rectangleJunctionMask(top, left, bottom, right, requiredJunctions);
            const newEdges = edges & ~coveredEdges;
            const newJunctions = junctions & ~coveredJunctions;
            if (newEdges === 0n && newJunctions === 0n) continue;
            const candidate = { topRow: top, leftColumn: left, bottomRow: bottom, rightColumn: right };
            if (best === null || isBetterBlob(candidate, best, newEdges, newJunctions,
              bestEdges & ~coveredEdges, bestJunctions & ~coveredJunctions)) {
              best = candidate;
              bestEdges = edges;
              bestJunctions = junctions;
            }
          }
          if (stopLeftExpansion || rightExclusive === anchorRight) break;
        }
        if (stopLeftExpansion) break;
      }
    }
    if (best === null) throw new Error("uncoverable smooth feature");
    result.push(best);
    coveredEdges |= bestEdges;
    coveredJunctions |= bestJunctions;
  }
  return result;
}

export function parseHex(value) {
  const normalized = value.trim().replace(/^0x/i, "");
  if (!/^[0-9a-f]{1,8}$/i.test(normalized)) throw new RangeError("Enter 1–8 hexadecimal digits.");
  return Number.parseInt(normalized, 16) | 0;
}

export function formatHex(value) {
  return (value >>> 0).toString(16).padStart(8, "0").toUpperCase();
}