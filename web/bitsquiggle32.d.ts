export type Style = "standard" | "high-contrast" | "monochrome";
export type Mode = "A|" | "A-" | "A+" | "A/";

export interface Edge {
  startRow: number;
  startColumn: number;
  endRow: number;
  endColumn: number;
}

export interface VisSpec {
  input: number;
  mixed: number;
  connections: Uint8Array;
  cells: number[][];
  style: Style;
  preferredMode: Mode;
  actualMode: Mode;
  fallback: boolean;
  luminanceIndex: number;
  swapped: boolean;
  background: string;
  foreground: string;
}

export interface PixelGrid {
  width: number;
  height: number;
  pixels: Uint8Array;
  background: string;
  foreground: string;
  style: Style;
}

export const ROWS: number;
export const COLUMNS: number;
export const EDGE_COUNT: number;
export const PIXEL_WIDTH: number;
export const PIXEL_HEIGHT: number;
export const STYLES: readonly Style[];
export const MODES: readonly Mode[];
export const EDGES: readonly Edge[];

/** Return the canonical visual specification for a signed 32-bit bit pattern. */
export function spec(input: number, style?: Style): VisSpec;

/** Return the exact 16×22 binary raster and colors for a signed 32-bit bit pattern. */
export function pixels(input: number, style?: Style): PixelGrid;

/** Parse one to eight hexadecimal digits as a signed 32-bit bit pattern. */
export function parseHex(value: string): number;

/** Format a signed 32-bit bit pattern as eight uppercase hexadecimal digits. */
export function formatHex(value: number): string;
