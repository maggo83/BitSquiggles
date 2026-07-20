/* C99 BitSquiggle32 conformance and regression tests.
 *
 * Run from the repository root: c/test_bitsquiggle32 fixtures/v1.json
 * Grug 2-Clause License: do what want; not sue grug.
 */
#include "bitsquiggle32.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static unsigned long checks = 0;

static void check(bool condition, const char *message) {
    ++checks;
    if (!condition) {
        fprintf(stderr, "C99 test failed: %s\n", message);
        exit(1);
    }
}

static void connections_string(const uint8_t connections[BITSQUIGGLE32_EDGE_COUNT],
                               char output[BITSQUIGGLE32_EDGE_COUNT + 1]) {
    unsigned int index;
    for (index = 0; index < BITSQUIGGLE32_EDGE_COUNT; ++index) output[index] = (char)('0' + connections[index]);
    output[BITSQUIGGLE32_EDGE_COUNT] = '\0';
}

static void pixels_string(const Bitsquiggle32PixelGrid *grid,
                          char output[BITSQUIGGLE32_PIXEL_WIDTH * BITSQUIGGLE32_PIXEL_HEIGHT + 1]) {
    unsigned int index;
    for (index = 0; index < BITSQUIGGLE32_PIXEL_WIDTH * BITSQUIGGLE32_PIXEL_HEIGHT; ++index) {
        output[index] = (char)('0' + grid->pixels[index]);
    }
    output[BITSQUIGGLE32_PIXEL_WIDTH * BITSQUIGGLE32_PIXEL_HEIGHT] = '\0';
}

static void test_edges_and_capacities(void) {
    Bitsquiggle32Edge edges[BITSQUIGGLE32_EDGE_COUNT];
    const unsigned int expected[] = {32, 31, 29, 33};
    unsigned int index;
    bitsquiggle32_edges(edges);
    for (index = 0; index < BITSQUIGGLE32_EDGE_COUNT; ++index) {
        check((unsigned int)(edges[index].end_row - edges[index].start_row)
                  + (unsigned int)(edges[index].end_column - edges[index].start_column) == 1,
              "orthogonal unit edge");
        if (index > 0) {
            check(memcmp(&edges[index - 1], &edges[index], sizeof(edges[index])) < 0,
                  "lexical edge order");
        }
    }
    for (index = 0; index < 4; ++index) {
        check(bitsquiggle32_free_connection_count((Bitsquiggle32Mode)index) == expected[index],
              "free connection count");
    }
}

static void test_golden_vector(void) {
    Bitsquiggle32Spec visual;
    Bitsquiggle32PixelGrid grid;
    char mask[BITSQUIGGLE32_EDGE_COUNT + 1];
    check(bitsquiggle32_spec(UINT32_C(0x89abcdef), BITSQUIGGLE32_STANDARD, &visual) == 0,
          "golden spec success");
    check(visual.mixed == UINT32_C(0x47ac5876), "golden mixed value");
    check(visual.preferred_mode == BITSQUIGGLE32_TOP_BOTTOM, "golden preferred mode");
    check(visual.actual_mode == BITSQUIGGLE32_TOP_BOTTOM && !visual.fallback, "golden actual mode");
    connections_string(visual.connections, mask);
    check(strcmp(mask, "0001111010110001011000011101010010101100001010011011010011") == 0,
          "golden connections");
    check(strcmp(visual.background.hex, "#140040") == 0, "golden background");
    check(strcmp(visual.foreground.hex, "#8d9200") == 0, "golden foreground");
    check(bitsquiggle32_pixels(UINT32_C(0x89abcdef), BITSQUIGGLE32_STANDARD, &grid) == 0,
          "golden pixels success");
    check(grid.width == 16 && grid.height == 22, "golden grid dimensions");
}

static void test_style_and_raster_properties(void) {
    unsigned int value;
    for (value = 0; value < 1000; ++value) {
        Bitsquiggle32Spec standard;
        Bitsquiggle32Spec monochrome;
        Bitsquiggle32PixelGrid grid;
        Bitsquiggle32Edge edges[BITSQUIGGLE32_EDGE_COUNT];
        unsigned int edge;
        unsigned int x;
        unsigned int y;
        check(bitsquiggle32_spec(value, BITSQUIGGLE32_STANDARD, &standard) == 0, "standard spec");
        check(bitsquiggle32_spec(value, BITSQUIGGLE32_MONOCHROME, &monochrome) == 0, "monochrome spec");
        check(memcmp(standard.connections, monochrome.connections, BITSQUIGGLE32_EDGE_COUNT) == 0,
              "style-independent geometry");
        check(bitsquiggle32_matches_mode(standard.connections, standard.actual_mode), "actual mode match");
        check(bitsquiggle32_pixels(value, BITSQUIGGLE32_STANDARD, &grid) == 0, "pixels success");
        for (x = 0; x < BITSQUIGGLE32_PIXEL_WIDTH; ++x) {
            check(grid.pixels[x] == 0 && grid.pixels[(BITSQUIGGLE32_PIXEL_HEIGHT - 1) * BITSQUIGGLE32_PIXEL_WIDTH + x] == 0,
                  "horizontal border");
        }
        bitsquiggle32_edges(edges);
        for (edge = 0; edge < BITSQUIGGLE32_EDGE_COUNT; ++edge) {
            x = 1 + 3 * edges[edge].start_column;
            y = 1 + 3 * edges[edge].start_row;
            check((edges[edge].start_row == edges[edge].end_row
                   ? grid.pixels[y * BITSQUIGGLE32_PIXEL_WIDTH + x + 2]
                   : grid.pixels[(y + 2) * BITSQUIGGLE32_PIXEL_WIDTH + x]) == standard.connections[edge],
                  "bridge recovers edge");
        }
    }
}

static void set_connection(uint8_t connections[BITSQUIGGLE32_EDGE_COUNT],
                           uint8_t start_row, uint8_t start_column,
                           uint8_t end_row, uint8_t end_column) {
    Bitsquiggle32Edge edges[BITSQUIGGLE32_EDGE_COUNT];
    unsigned int index;
    bitsquiggle32_edges(edges);
    for (index = 0; index < BITSQUIGGLE32_EDGE_COUNT; ++index) {
        if (edges[index].start_row == start_row && edges[index].start_column == start_column
            && edges[index].end_row == end_row && edges[index].end_column == end_column) {
            connections[index] = 1;
            return;
        }
    }
    check(false, "known canonical edge");
}

static void check_blob(const Bitsquiggle32SmoothBlob *blob,
                       uint8_t top, uint8_t left, uint8_t bottom, uint8_t right,
                       const char *message) {
    check(blob->top_row == top && blob->left_column == left
          && blob->bottom_row == bottom && blob->right_column == right, message);
}

static void test_smooth_blobs(void) {
    uint8_t connections[BITSQUIGGLE32_EDGE_COUNT];
    Bitsquiggle32SmoothBlob blobs[BITSQUIGGLE32_MAX_SMOOTH_BLOBS];
    unsigned int value;
    int count;
    memset(connections, 0, sizeof(connections));
    check(bitsquiggle32_smooth_blobs(connections, blobs) == 0, "empty mask has no blobs");

    set_connection(connections, 0, 0, 0, 1);
    count = bitsquiggle32_smooth_blobs(connections, blobs);
    check(count == 1, "one edge has one blob");
    check_blob(&blobs[0], 0, 0, 0, 1, "one edge blob geometry");

    memset(connections, 0, sizeof(connections));
    set_connection(connections, 0, 0, 0, 1);
    set_connection(connections, 0, 1, 0, 2);
    count = bitsquiggle32_smooth_blobs(connections, blobs);
    check(count == 1, "connected row has one blob");
    check_blob(&blobs[0], 0, 0, 0, 2, "connected row blob geometry");

    memset(connections, 0, sizeof(connections));
    set_connection(connections, 0, 0, 0, 1);
    set_connection(connections, 1, 0, 1, 1);
    set_connection(connections, 0, 0, 1, 0);
    set_connection(connections, 0, 1, 1, 1);
    count = bitsquiggle32_smooth_blobs(connections, blobs);
    check(count == 1, "junction has one blob");
    check_blob(&blobs[0], 0, 0, 1, 1, "junction blob geometry");

    memset(connections, 1, sizeof(connections));
    count = bitsquiggle32_smooth_blobs(connections, blobs);
    check(count == 1, "complete grid has one blob");
    check_blob(&blobs[0], 0, 0, 6, 4, "complete grid blob geometry");

    for (value = 0; value < 2000; ++value) {
        Bitsquiggle32Spec visual;
        check(bitsquiggle32_spec(value, BITSQUIGGLE32_STANDARD, &visual) == 0,
              "generated blob spec");
        count = bitsquiggle32_smooth_blobs(visual.connections, blobs);
        check(count >= 0 && count <= (int)BITSQUIGGLE32_MAX_SMOOTH_BLOBS,
              "generated blob count is bounded");
    }

    connections[0] = 2;
    check(bitsquiggle32_smooth_blobs(connections, blobs) == -1,
          "reject non-binary blob mask");
    check(bitsquiggle32_smooth_blobs(NULL, blobs) == -1,
          "reject null blob mask");
    check(bitsquiggle32_smooth_blobs(connections, NULL) == -1,
          "reject null blob output");
}

static char *read_file(const char *path, size_t *length) {
    FILE *file = fopen(path, "rb");
    char *content;
    long file_length;
    if (file == NULL) return NULL;
    if (fseek(file, 0, SEEK_END) != 0 || (file_length = ftell(file)) < 0 || fseek(file, 0, SEEK_SET) != 0) {
        fclose(file);
        return NULL;
    }
    content = (char *)malloc((size_t)file_length + 1);
    if (content == NULL || fread(content, 1, (size_t)file_length, file) != (size_t)file_length) {
        free(content);
        fclose(file);
        return NULL;
    }
    fclose(file);
    content[file_length] = '\0';
    *length = (size_t)file_length;
    return content;
}

static bool quoted_field(const char *begin, const char *end, const char *field,
                         char *output, size_t output_size) {
    char needle[48];
    const char *value;
    const char *finish;
    (void)snprintf(needle, sizeof(needle), "\"%s\":\"", field);
    value = strstr(begin, needle);
    if (value == NULL || value >= end) return false;
    value += strlen(needle);
    finish = strchr(value, '\"');
    if (finish == NULL || finish >= end || (size_t)(finish - value) >= output_size) return false;
    memcpy(output, value, (size_t)(finish - value));
    output[finish - value] = '\0';
    return true;
}

static bool bool_field(const char *begin, const char *end, const char *field, bool *output) {
    char needle[48];
    const char *value;
    (void)snprintf(needle, sizeof(needle), "\"%s\":", field);
    value = strstr(begin, needle);
    if (value == NULL || value >= end) return false;
    value += strlen(needle);
    if (strncmp(value, "true", 4) == 0) {
        *output = true;
        return true;
    }
    if (strncmp(value, "false", 5) == 0) {
        *output = false;
        return true;
    }
    return false;
}

    static void check_fixture_style(const char *begin, const char *end, uint32_t input,
                          const char *name, Bitsquiggle32Style style) {
        char needle[64];
        char expected[16];
        const char *style_begin;
        Bitsquiggle32Spec visual;
        (void)snprintf(needle, sizeof(needle), "\"%s\":{", name);
        style_begin = strstr(begin, needle);
        check(style_begin != NULL && style_begin < end, "fixture style field");
        check(bitsquiggle32_spec(input, style, &visual) == 0, "fixture styled spec");
        check(quoted_field(style_begin, end, "background", expected, sizeof(expected)),
            "fixture background field");
        check(strcmp(visual.background.hex, expected) == 0, "fixture background");
        check(quoted_field(style_begin, end, "foreground", expected, sizeof(expected)),
            "fixture foreground field");
        check(strcmp(visual.foreground.hex, expected) == 0, "fixture foreground");
    }

static void test_shared_fixture(const char *path) {
    size_t length;
    char *fixture = read_file(path, &length);
    const char *vector = fixture;
    unsigned int count = 0;
    (void)length;
    check(fixture != NULL, "open shared fixture");
    while ((vector = strstr(vector, "\"input\":\"")) != NULL) {
        const char *next = strstr(vector + 1, "\"input\":\"");
        const char *end = next == NULL ? fixture + strlen(fixture) : next;
        char input[9];
        char expected[353];
        char actual[353];
        bool fallback;
        unsigned long value;
        Bitsquiggle32Spec visual;
        Bitsquiggle32PixelGrid grid;
        check(quoted_field(vector, end, "input", input, sizeof(input)), "fixture input");
        value = strtoul(input, NULL, 16);
        check(bitsquiggle32_spec((uint32_t)value, BITSQUIGGLE32_STANDARD, &visual) == 0, "fixture spec");
        check(quoted_field(vector, end, "mixed", expected, sizeof(expected)), "fixture mixed field");
        (void)snprintf(actual, sizeof(actual), "%08x", visual.mixed);
        check(strcmp(actual, expected) == 0, "fixture mixed");
        check(quoted_field(vector, end, "connections", expected, sizeof(expected)), "fixture connections field");
        connections_string(visual.connections, actual);
        check(strcmp(actual, expected) == 0, "fixture connections");
        check(quoted_field(vector, end, "preferredMode", expected, sizeof(expected)), "fixture preferred field");
        check(strcmp(bitsquiggle32_mode_label(visual.preferred_mode), expected) == 0, "fixture preferred mode");
        check(quoted_field(vector, end, "actualMode", expected, sizeof(expected)), "fixture actual field");
        check(strcmp(bitsquiggle32_mode_label(visual.actual_mode), expected) == 0, "fixture actual mode");
        check(bool_field(vector, end, "fallback", &fallback) && fallback == visual.fallback, "fixture fallback");
        check(bitsquiggle32_pixels((uint32_t)value, BITSQUIGGLE32_STANDARD, &grid) == 0, "fixture pixels");
        check(quoted_field(vector, end, "pixels", expected, sizeof(expected)), "fixture pixels field");
        pixels_string(&grid, actual);
        check(strcmp(actual, expected) == 0, "fixture pixels");
        check_fixture_style(vector, end, (uint32_t)value, "standard", BITSQUIGGLE32_STANDARD);
        check_fixture_style(vector, end, (uint32_t)value, "high-contrast", BITSQUIGGLE32_HIGH_CONTRAST);
        check_fixture_style(vector, end, (uint32_t)value, "monochrome", BITSQUIGGLE32_MONOCHROME);
        ++count;
        vector = end;
    }
    check(count > 0, "fixture vector count");
    free(fixture);
}

int main(int argc, char **argv) {
    const char *fixture = argc > 1 ? argv[1] : "fixtures/v1.json";
    test_edges_and_capacities();
    test_golden_vector();
    test_style_and_raster_properties();
    test_smooth_blobs();
    test_shared_fixture(fixture);
    printf("BitSquiggles C99 tests passed (%lu checks)\n", checks);
    return 0;
}
