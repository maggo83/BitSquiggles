export type Style = "standard" | "high-contrast" | "monochrome";
export type Mode = "A|" | "A-" | "A+" | "A/";

export interface Edge {
  startRow: number;
  startColumn: number;
  endRow: number;
  endColumn: number;
}

export interface Visualization {
  input: number;
  mixed: number;
  connections: Uint8Array;
  cells: number[][];
  pixels: Uint8Array;
  preferredMode: Mode;
  actualMode: Mode;
  fallback: boolean;
  background: string;
  foreground: string;
}

export const ROWS: number;
export const COLUMNS: number;
export const PIXEL_WIDTH: number;
export const PIXEL_HEIGHT: number;
export const STYLES: readonly Style[];
export const MODES: readonly Mode[];
export const EDGES: readonly Edge[];

/** Return the exact 16×22 binary raster for a canonical connection mask. */
export function pixels(connections: ArrayLike<number>): Uint8Array;

/** Return the complete BitSquiggle32 visualization for a signed 32-bit bit pattern. */
export function visualize(input: number, style?: Style): Visualization;

/** Parse one to eight hexadecimal digits as a signed 32-bit bit pattern. */
export function parseHex(value: string): number;

/** Format a signed 32-bit bit pattern as eight uppercase hexadecimal digits. */
export function formatHex(value: number): string;
