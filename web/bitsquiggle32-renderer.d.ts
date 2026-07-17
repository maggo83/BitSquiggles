import type { PixelGrid, VisSpec } from "./bitsquiggle32.js";

/** Paint a smooth Canvas 2D presentation from a canonical visual specification. */
export function renderSmooth(canvas: HTMLCanvasElement, visual: VisSpec): void;

/** Paint an exact native raster from a canonical pixel grid. */
export function renderRaster(canvas: HTMLCanvasElement, grid: PixelGrid): void;

export const PIXEL_WIDTH: number;
export const PIXEL_HEIGHT: number;
