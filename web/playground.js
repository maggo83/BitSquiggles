import { EDGES, STYLES, formatHex, parseHex, visualize } from "./bitsquiggle32.js";

const form = document.querySelector("#value-form");
const input = document.querySelector("#value");
const message = document.querySelector("#message");
const random = document.querySelector("#random");
const copyLink = document.querySelector("#copy-link");
const cards = new Map(STYLES.map((style) => [style, document.querySelector(`#${style}-card`)]));

function roundedRect(context, x, y, width, height, radius) {
  context.beginPath();
  context.roundRect(x, y, width, height, radius);
  context.fill();
}

function drawSmooth(canvas, visual) {
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
    context.fillRect(x + (edge.startRow === edge.endRow ? 10 : 0), y + (edge.startRow === edge.endRow ? 0 : 10), edge.startRow === edge.endRow ? 30 : 20, edge.startRow === edge.endRow ? 20 : 30);
  });
  for (let row = 0; row < 6; row += 1) for (let column = 0; column < 4; column += 1) {
    const at = (startRow, startColumn, endRow, endColumn) => visual.connections[EDGES.findIndex((edge) => edge.startRow === startRow && edge.startColumn === startColumn && edge.endRow === endRow && edge.endColumn === endColumn)];
    if (at(row, column, row, column + 1) && at(row + 1, column, row + 1, column + 1)
        && at(row, column, row + 1, column) && at(row, column + 1, row + 1, column + 1)) context.fillRect(30 + column * 30, 30 + row * 30, 10, 10);
  }
}

function drawRaster(canvas, visual) {
  const context = canvas.getContext("2d");
  context.fillStyle = visual.background;
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.fillStyle = visual.foreground;
  visual.pixels.forEach((pixel, index) => {
    if (pixel) context.fillRect(index % 16, Math.floor(index / 16), 1, 1);
  });
}

function update(value) {
  const hex = formatHex(value);
  input.value = hex;
  const standard = visualize(value);
  document.querySelector("#mixed-value").textContent = `0x${formatHex(standard.mixed)}`;
  document.querySelector("#preferred-mode").textContent = standard.preferredMode;
  document.querySelector("#rendered-mode").textContent = standard.actualMode;
  const fallback = document.querySelector("#fallback");
  fallback.hidden = !standard.fallback;
  fallback.textContent = "The preferred symmetry could not encode this value uniquely, so BitSquiggle32 used A|.";
  STYLES.forEach((style) => {
    const visual = visualize(value, style);
    const card = cards.get(style);
    card.style.setProperty("--background", visual.background);
    card.style.setProperty("--foreground", visual.foreground);
    drawSmooth(card.querySelector(".smooth"), visual);
    drawRaster(card.querySelector(".raster"), visual);
  });
  const url = new URL(window.location.href);
  url.search = `value=${hex}`;
  history.replaceState(null, "", url);
  message.textContent = "";
}

form.addEventListener("submit", (event) => {
  event.preventDefault();
  try {
    update(parseHex(input.value));
  } catch (error) {
    message.textContent = error.message;
    input.focus();
  }
});

random.addEventListener("click", () => update(crypto.getRandomValues(new Uint32Array(1))[0]));
copyLink.addEventListener("click", async () => {
  try {
    await navigator.clipboard.writeText(window.location.href);
    message.textContent = "Share link copied.";
  } catch {
    message.textContent = "Copy the address bar to share this visualization.";
  }
});

try {
  update(parseHex(new URLSearchParams(window.location.search).get("value") || input.value));
} catch {
  update(0x12345678);
  message.textContent = "The link contained an invalid value; showing the default instead.";
}