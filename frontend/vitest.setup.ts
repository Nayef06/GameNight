// Radix measures its radio indicators in browsers. JSDOM does not provide ResizeObserver.
class TestResizeObserver implements ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

globalThis.ResizeObserver = TestResizeObserver;
