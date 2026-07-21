/** Optional Swing/Java2D renderer for the BitSquiggle32 core API. */
module io.github.maggo83.bitsquiggles.renderer.swing {
    requires transitive io.github.maggo83.bitsquiggles;
    requires transitive java.desktop;

    exports bitsquiggles.renderer.swing;
}
