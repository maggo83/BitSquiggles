// BitSquiggles — optional Canvas 2D renderer.
// Grug 2-Clause License: do what want; not sue grug.

import { EDGES, PIXEL_HEIGHT, PIXEL_WIDTH } from "./bitsquiggle32.js";

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
  visual.cells.forEach((row, rowIndex) => row.forEach((active, columnIndex) => {
    if (active) roundedRect(context, 10 + columnIndex * 30, 10 + rowIndex * 30, 20, 20, 10);
  }));
  visual.connections.forEach((connected, index) => {
    if (!connected) return;
    const edge = EDGES[index];
    const x = 10 + edge.startColumn * 30;
    const y = 10 + edge.startRow * 30;
    const horizontal = edge.startRow === edge.endRow;
    context.fillRect(x + (horizontal ? 10 : 0), y + (horizontal ? 0 : 10), horizontal ? 30 : 20, horizontal ? 20 : 30);
  });
  for (let row = 0; row < 6; row += 1) {
    for (let column = 0; column < 4; column += 1) {
      if (edgeValue(visual, row, column, row, column + 1)
          && edgeValue(visual, row + 1, column, row + 1, column + 1)
          && edgeValue(visual, row, column, row + 1, column)
          && edgeValue(visual, row, column + 1, row + 1, column + 1)) {
        context.fillRect(30 + column * 30, 30 + row * 30, 10, 10);
      }
    }
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

function edgeValue(visual, startRow, startColumn, endRow, endColumn) {
  const index = EDGES.findIndex((edge) => edge.startRow === startRow
    && edge.startColumn === startColumn
    && edge.endRow === endRow
    && edge.endColumn === endColumn);
  return visual.connections[index];
}

export { PIXEL_WIDTH, PIXEL_HEIGHT };
