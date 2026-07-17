import type { Visualization } from "./bitsquiggle32.js";

/** Paint a smooth Canvas 2D presentation without changing the canonical raster. */
export function drawSmooth(canvas: HTMLCanvasElement, visual: Visualization): void;

/** Paint the exact native raster with one Canvas pixel per canonical raster pixel. */
export function drawRaster(canvas: HTMLCanvasElement, visual: Visualization): void;

export const PIXEL_WIDTH: number;
export const PIXEL_HEIGHT: number;
