/** Optional JavaFX renderer for the BitSquiggle32 core API. */
module io.github.maggo83.bitsquiggles.renderer.javafx {
    requires io.github.maggo83.bitsquiggles;
    requires transitive javafx.graphics;

    exports bitsquiggles.renderer.javafx;
}
