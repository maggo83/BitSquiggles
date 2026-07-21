// BitSquiggles — optional Canvas 2D renderer.
// Grug 2-Clause License: do what want; not sue grug.

import { PIXEL_HEIGHT, PIXEL_WIDTH, smoothBlobs } from "./bitsquiggle32.js";

export * from "./bitsquiggle32.js";

function roundedRect(context, x, y, width, height, radius) {
  context.beginPath();
  context.roundRect(x, y, width, height, radius);
  context.fill();
}

/** Paint a smooth Canvas 2D presentation from a canonical visual specification. */
export function renderSmooth(canvas, visual) {
  const context = canvas.getContext("2d");
  context.clearRect(0, 0, canvas.width, canvas.height);
  context.fillStyle = visual.background;
  roundedRect(context, 0, 0, canvas.width, canvas.height, 20);
  context.fillStyle = visual.foreground;
  for (const blob of smoothBlobs(visual.connections)) {
    roundedRect(context,
      10 + blob.leftColumn * 30,
      10 + blob.topRow * 30,
      20 + (blob.rightColumn - blob.leftColumn) * 30,
      20 + (blob.bottomRow - blob.topRow) * 30,
      10);
  }
}

/** Paint an exact native raster from a canonical pixel grid. */
export function renderRaster(canvas, grid) {
  const context = canvas.getContext("2d");
  context.fillStyle = grid.background;
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.fillStyle = grid.foreground;
  grid.pixels.forEach((pixel, index) => {
    if (pixel) context.fillRect(index % PIXEL_WIDTH, Math.floor(index / PIXEL_WIDTH), 1, 1);
  });
}