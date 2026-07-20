import assert from "node:assert/strict";

class FakeContext {
  constructor() {
    this.fillCount = 0;
  }

  beginPath() {}
  clearRect() {}
  roundRect() {}
  fill() { this.fillCount += 1; }
  fillRect() { this.fillCount += 1; }
}

class FakeElement {
  constructor() {
    this.listeners = new Map();
    this.style = { setProperty() {} };
    this.textContent = "";
    this.hidden = false;
    this.value = "";
    this.focused = false;
  }

  addEventListener(type, listener) {
    this.listeners.set(type, listener);
  }

  focus() {
    this.focused = true;
  }
}

class FakeCanvas extends FakeElement {
  constructor(width, height) {
    super();
    this.width = width;
    this.height = height;
    this.context = new FakeContext();
  }

  getContext(kind) {
    assert.equal(kind, "2d");
    return this.context;
  }
}

class FakeCard extends FakeElement {
  constructor() {
    super();
    this.smooth = new FakeCanvas(160, 220);
    this.raster = new FakeCanvas(16, 22);
  }

  querySelector(selector) {
    return selector === ".smooth" ? this.smooth : this.raster;
  }
}

const form = new FakeElement();
const input = new FakeElement();
input.value = "12345678";
const message = new FakeElement();
const random = new FakeElement();
const copyLink = new FakeElement();
const fallback = new FakeElement();
const mixedValue = new FakeElement();
const preferredMode = new FakeElement();
const renderedMode = new FakeElement();
const cards = new Map([
  ["#standard-card", new FakeCard()],
  ["#high-contrast-card", new FakeCard()],
  ["#monochrome-card", new FakeCard()],
  ["#black-and-white-card", new FakeCard()],
]);
const elements = new Map([
  ["#value-form", form],
  ["#value", input],
  ["#message", message],
  ["#random", random],
  ["#copy-link", copyLink],
  ["#fallback", fallback],
  ["#mixed-value", mixedValue],
  ["#preferred-mode", preferredMode],
  ["#rendered-mode", renderedMode],
  ...cards,
]);

globalThis.document = {
  querySelector(selector) {
    return elements.get(selector) ?? null;
  },
};
globalThis.window = {
  location: {
    href: "https://example.test/?value=89abcdef",
    search: "?value=89abcdef",
  },
};
globalThis.history = {
  replaceState(_state, _title, url) {
    window.location.href = url.toString();
    window.location.search = new URL(url).search;
  },
};
Object.defineProperty(globalThis, "navigator", {
  configurable: true,
  value: { clipboard: { writeText: async () => {} } },
});

await import("./playground.js");

assert.equal(input.value, "89ABCDEF", "URL value populates input");
assert.equal(mixedValue.textContent, "0x47AC5876", "shows mixed value");
assert.equal(preferredMode.textContent, "A-", "shows preferred mode");
assert.equal(renderedMode.textContent, "A-", "shows actual mode");
for (const card of cards.values()) {
  assert.ok(card.smooth.context.fillCount > 0, "smooth renderer paints a card");
  assert.ok(card.raster.context.fillCount > 0, "raster renderer paints a card");
}

input.value = "not hex";
form.listeners.get("submit")({ preventDefault() {} });
assert.match(message.textContent, /hexadecimal/, "invalid input shows an error");
assert.equal(input.focused, true, "invalid input receives focus");

random.listeners.get("click")();
assert.match(input.value, /^[0-9A-F]{8}$/, "random action updates the value");
console.log("BitSquiggles playground smoke tests passed");
