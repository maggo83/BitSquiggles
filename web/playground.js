import { STYLES, formatHex, parseHex, pixels, spec } from "./bitsquiggle32.js";
import { renderRaster, renderSmooth } from "./bitsquiggle32-renderer-canvas.js";

const form = document.querySelector("#value-form");
const input = document.querySelector("#value");
const message = document.querySelector("#message");
const random = document.querySelector("#random");
const copyLink = document.querySelector("#copy-link");
const cards = new Map(STYLES.map((style) => [style, document.querySelector(`#${style}-card`)]));

function update(value) {
  const hex = formatHex(value);
  input.value = hex;
  const standard = spec(value);
  document.querySelector("#mixed-value").textContent = `0x${formatHex(standard.mixed)}`;
  document.querySelector("#preferred-mode").textContent = standard.preferredMode;
  document.querySelector("#rendered-mode").textContent = standard.actualMode;
  const fallback = document.querySelector("#fallback");
  fallback.hidden = !standard.fallback;
  fallback.textContent = "The preferred symmetry could not encode this value uniquely, so BitSquiggle32 used A|.";
  STYLES.forEach((style) => {
    const visual = spec(value, style);
    const raster = pixels(value, style);
    const card = cards.get(style);
    card.style.setProperty("--background", visual.background);
    card.style.setProperty("--foreground", visual.foreground);
    renderSmooth(card.querySelector(".smooth"), visual);
    renderRaster(card.querySelector(".raster"), raster);
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