"""Dependency-free BitSquiggle32 tests, compatible with CPython and MicroPython.

Grug 2-Clause License: do what want; not sue grug.
"""

import bitsquiggle32
import bitsquiggle32_renderer_framebuffer

_checks = 0


def check(condition, message):
    global _checks
    _checks += 1
    if not condition:
        raise AssertionError(message)


def close(expected, actual, message):
    check(abs(expected - actual) < 1e-12,
          "%s: expected %r, got %r" % (message, expected, actual))


def expect_failure(function, message):
    global _checks
    _checks += 1
    try:
        function()
    except ValueError:
        return
    raise AssertionError(message)


def test_edges_and_mode_capacities():
    check(len(bitsquiggle32.EDGES) == 58, "58 canonical edges")
    previous = None
    for edge in bitsquiggle32.EDGES:
        start_row, start_column, end_row, end_column = edge
        check((end_row - start_row) + (end_column - start_column) == 1,
              "orthogonal unit edge")
        if previous is not None:
            check(previous < edge, "lexical edge order")
        previous = edge
    expected = (32, 31, 29, 33)
    for index, mode in enumerate(bitsquiggle32.MODES):
        check(bitsquiggle32.free_connection_count(mode) == expected[index],
              "free count %s" % mode)


def test_public_surface():
    expected = {
        "ROWS", "COLUMNS", "EDGE_COUNT", "PIXEL_WIDTH", "PIXEL_HEIGHT", "EDGES",
        "STANDARD", "HIGH_CONTRAST", "MONOCHROME", "BLACK_AND_WHITE", "STYLES",
        "LEFT_RIGHT", "TOP_BOTTOM", "HALF_TURN", "DIAGONAL_SLASH", "MODES",
        "mix32", "free_connection_count", "matches_mode", "spec", "pixels",
        "smooth_blobs",
    }
    check(set(bitsquiggle32.__all__) == expected, "declared public surface")


def test_modes_and_canonical_priority():
    seen = {mode: False for mode in bitsquiggle32.MODES}
    saw_fallback = False
    saw_half_turn_capacity_fallback = False
    saw_accepted_half_turn = False
    for value in range(50000):
        visual = bitsquiggle32.spec(value)
        seen[visual["preferred_mode"]] = True
        saw_fallback = saw_fallback or visual["fallback"]
        if visual["preferred_mode"] == bitsquiggle32.HALF_TURN:
            if visual["mixed"] & 1:
                check(visual["fallback"], "half-turn omitted bit fallback")
                saw_half_turn_capacity_fallback = True
            elif not visual["fallback"]:
                saw_accepted_half_turn = True
        check(bitsquiggle32.matches_mode(
            visual["connections"], visual["actual_mode"]), "actual mode match")
        if not visual["fallback"]:
            preferred = bitsquiggle32.MODES.index(visual["preferred_mode"])
            for earlier in bitsquiggle32.MODES[:preferred]:
                check(not bitsquiggle32.matches_mode(visual["connections"], earlier),
                      "canonical mode priority")
        else:
            check(visual["actual_mode"] == bitsquiggle32.LEFT_RIGHT,
                  "fallback mode")
    for mode in bitsquiggle32.MODES:
        check(seen[mode], "mode observed %s" % mode)
    check(saw_fallback, "fallback observed")
    check(saw_half_turn_capacity_fallback, "half-turn capacity fallback observed")
    check(saw_accepted_half_turn, "injective half-turn candidate observed")


def test_input_bit_avalanche():
    base = bitsquiggle32.spec(0)["connections"]
    total = 0
    for bit in range(32):
        changed = bitsquiggle32.spec(1 << bit)["connections"]
        distance = sum(first != second for first, second in zip(base, changed))
        check(distance >= 20, "edge avalanche bit %d" % bit)
        total += distance
    check(total / 32.0 >= 26.0, "average edge avalanche")


def test_sampled_monochrome_injectivity():
    seen = set()
    for value in range(100000):
        visual = bitsquiggle32.spec(value, bitsquiggle32.MONOCHROME)
        mask = bytes(visual["connections"])
        check(mask not in seen, "unique monochrome visual %d" % value)
        seen.add(mask)


def _black_and_white_bitmap(value):
    grid = bitsquiggle32.pixels(value, bitsquiggle32.BLACK_AND_WHITE)
    foreground = grid["foreground"]["hex"] == "#ffffff"
    background = grid["background"]["hex"] == "#ffffff"
    return bytes(foreground if pixel else background for pixel in grid["pixels"])


def test_black_and_white_colors_and_polarity():
    for value in range(100000):
        visual = bitsquiggle32.spec(value, bitsquiggle32.BLACK_AND_WHITE)
        grid = bitsquiggle32.pixels(value, bitsquiggle32.BLACK_AND_WHITE)
        check(visual["foreground"]["hex"] in ("#000000", "#ffffff"),
              "black-and-white foreground")
        check(visual["background"]["hex"] in ("#000000", "#ffffff"),
              "black-and-white background")
        check(visual["foreground"]["hex"] != visual["background"]["hex"],
              "black-and-white colors differ")
        check((visual["foreground"]["hex"] == "#000000") == visual["swapped"],
              "swap controls black-and-white foreground polarity")
        check(grid["pixels"][0] == 0, "black-and-white border is background")

    seen = set()
    for value in range(100000):
        bitmap = _black_and_white_bitmap(value)
        check(bitmap not in seen, "unique black-and-white bitmap %d" % value)
        seen.add(bitmap)


def test_pixel_renderer_is_lossless():
    for value in range(1000):
        visual = bitsquiggle32.spec(value)
        grid = bitsquiggle32.pixels(value)
        check(grid["width"] == 16 and grid["height"] == 22, "pixel size")
        check(len(grid["pixels"]) == 352, "pixel storage")
        for x in range(16):
            check(grid["pixels"][x] == 0, "top border")
            check(grid["pixels"][21 * 16 + x] == 0, "bottom border")
        for y in range(22):
            check(grid["pixels"][y * 16] == 0, "left border")
            check(grid["pixels"][y * 16 + 15] == 0, "right border")
        for edge_index, edge in enumerate(bitsquiggle32.EDGES):
            start_row, start_column, end_row, _ = edge
            x = 1 + start_column * 3
            y = 1 + start_row * 3
            if start_row == end_row:
                bridge = grid["pixels"][y * 16 + x + 2]
            else:
                bridge = grid["pixels"][(y + 2) * 16 + x]
            check(bridge == visual["connections"][edge_index],
                  "recoverable edge")


class _RecordingFramebuffer:
    def __init__(self):
        self.calls = []

    def fill_rect(self, x, y, width, height, color):
        self.calls.append((x, y, width, height, color))


def _one_bit_color(color):
    if color == "#000000":
        return 0
    if color == "#ffffff":
        return 1
    raise ValueError("expected black-and-white color")


def test_framebuffer_renderer_is_exact():
    for value in (0, 1, 0x89ABCDEF, 0xFFFFFFFF):
        grid = bitsquiggle32.pixels(value, bitsquiggle32.BLACK_AND_WHITE)
        target = _RecordingFramebuffer()
        bitsquiggle32_renderer_framebuffer.render_raster(
            target, grid, x=5, y=7, scale=2, color_mapper=_one_bit_color)

        background = _one_bit_color(grid["background"]["hex"])
        foreground = _one_bit_color(grid["foreground"]["hex"])
        check(target.calls[0] == (5, 7, 32, 44, background),
              "framebuffer renderer paints complete scaled background first")
        active = sum(grid["pixels"])
        check(len(target.calls) == active + 1,
              "framebuffer renderer paints each foreground pixel once")
        for row in range(grid["height"]):
            for column in range(grid["width"]):
                if not grid["pixels"][row * grid["width"] + column]:
                    continue
                check((5 + column * 2, 7 + row * 2, 2, 2, foreground)
                      in target.calls[1:], "framebuffer foreground pixel")

    grid = bitsquiggle32.pixels(0, bitsquiggle32.BLACK_AND_WHITE)
    expect_failure(lambda: bitsquiggle32_renderer_framebuffer.render_raster(
        object(), grid), "reject framebuffer without fill_rect")
    expect_failure(lambda: bitsquiggle32_renderer_framebuffer.render_raster(
        _RecordingFramebuffer(), grid, scale=0), "reject non-positive scale")
    bad_grid = dict(grid)
    bad_grid["width"] = 15
    expect_failure(lambda: bitsquiggle32_renderer_framebuffer.render_raster(
        _RecordingFramebuffer(), bad_grid), "reject non-canonical dimensions")


def test_colors_parity_and_golden_vector():
    value = 0x89ABCDEF
    standard = bitsquiggle32.spec(value, bitsquiggle32.STANDARD)
    contrast = bitsquiggle32.spec(value, bitsquiggle32.HIGH_CONTRAST)
    mono = bitsquiggle32.spec(value, bitsquiggle32.MONOCHROME)
    check(standard["connections"] == contrast["connections"], "style edges")
    check(standard["connections"] == mono["connections"], "monochrome edges")
    check(contrast["foreground"]["C"] > standard["foreground"]["C"], "HC chroma")
    close(0.0, mono["foreground"]["C"], "mono foreground chroma")
    close(0.0, mono["background"]["C"], "mono background chroma")
    check(not bitsquiggle32.spec(0)["swapped"], "even parity")
    check(bitsquiggle32.spec(1)["swapped"], "odd parity")

    mask = "".join(str(value) for value in standard["connections"])
    check(standard["mixed"] == 0x47AC5876, "golden mixed")
    check(standard["preferred_mode"] == bitsquiggle32.TOP_BOTTOM, "golden mode")
    check(not standard["fallback"], "golden fallback")
    check(mask == "0001111010110001011000011101010010101100001010011011010011",
          "golden connections")
    check(standard["background"]["hex"] == "#140040", "golden background")
    check(standard["foreground"]["hex"] == "#8d9200", "golden foreground")


def test_slash_wrap_regression():
    visual = bitsquiggle32.spec(0xD9ABCDEF)
    check(visual["actual_mode"] == bitsquiggle32.DIAGONAL_SLASH,
          "slash wrap regression mode")
    check(bitsquiggle32._connection(visual["connections"], 1, 1, 2, 1)
          == bitsquiggle32._connection(visual["connections"], 4, 3, 4, 4),
          "slash copy maps upper vertical to lower horizontal edge")
    check(bitsquiggle32._connection(visual["connections"], 1, 2, 2, 2)
          == bitsquiggle32._connection(visual["connections"], 3, 3, 3, 4),
          "slash copy preserves adjacent diagonal edge relation")


def _connections(*endpoints):
    result = bytearray(bitsquiggle32.EDGE_COUNT)
    for offset in range(0, len(endpoints), 4):
        target = tuple(endpoints[offset:offset + 4])
        for index, edge in enumerate(bitsquiggle32.EDGES):
            if edge == target:
                result[index] = 1
                break
    return result


def _assert_blob_coverage(connections, blobs):
    active_cells = bitsquiggle32._active_cells(connections)
    required_junctions = bytearray((bitsquiggle32.ROWS - 1)
                                   * (bitsquiggle32.COLUMNS - 1))
    covered_edges = bytearray(bitsquiggle32.EDGE_COUNT)
    covered_junctions = bytearray(len(required_junctions))

    for row in range(bitsquiggle32.ROWS - 1):
        for column in range(bitsquiggle32.COLUMNS - 1):
            if (bitsquiggle32._connection(connections, row, column, row, column + 1)
                    and bitsquiggle32._connection(
                        connections, row + 1, column, row + 1, column + 1)
                    and bitsquiggle32._connection(connections, row, column, row + 1, column)
                    and bitsquiggle32._connection(
                        connections, row, column + 1, row + 1, column + 1)):
                required_junctions[row * (bitsquiggle32.COLUMNS - 1) + column] = 1

    for top, left, bottom, right in blobs:
        check(top >= 0 and left >= 0 and bottom < bitsquiggle32.ROWS
              and right < bitsquiggle32.COLUMNS, "blob coordinates are in range")
        for row in range(top, bottom + 1):
            for column in range(left, right + 1):
                check(active_cells[row][column] == 1, "blob contains active cells only")
        for index, edge in enumerate(bitsquiggle32.EDGES):
            start_row, start_column, end_row, end_column = edge
            if (top <= start_row <= bottom and left <= start_column <= right
                    and top <= end_row <= bottom and left <= end_column <= right):
                check(connections[index] == 1, "blob internal edge is selected")
                covered_edges[index] = 1
        for row in range(top, bottom):
            for column in range(left, right):
                covered_junctions[row * (bitsquiggle32.COLUMNS - 1) + column] = 1

    check(covered_edges == connections, "blobs cover every selected edge")
    for index in range(len(required_junctions)):
        check(not required_junctions[index] or covered_junctions[index],
              "blobs cover every required junction")


def test_smooth_blobs():
    empty = bytearray(bitsquiggle32.EDGE_COUNT)
    check(bitsquiggle32.smooth_blobs(empty) == (), "empty mask has no smooth blobs")

    single_edge = _connections(0, 0, 0, 1)
    check(bitsquiggle32.smooth_blobs(single_edge) == ((0, 0, 0, 1),),
          "one edge has one 1x2 blob")
    _assert_blob_coverage(single_edge, bitsquiggle32.smooth_blobs(single_edge))

    row = _connections(0, 0, 0, 1, 0, 1, 0, 2)
    check(bitsquiggle32.smooth_blobs(row) == ((0, 0, 0, 2),),
          "connected row merges into one blob")
    _assert_blob_coverage(row, bitsquiggle32.smooth_blobs(row))

    square = _connections(
        0, 0, 0, 1,
        1, 0, 1, 1,
        0, 0, 1, 0,
        0, 1, 1, 1)
    check(bitsquiggle32.smooth_blobs(square) == ((0, 0, 1, 1),),
          "four-edge junction merges into one 2x2 blob")
    _assert_blob_coverage(square, bitsquiggle32.smooth_blobs(square))

    complete = bytearray([1] * bitsquiggle32.EDGE_COUNT)
    check(bitsquiggle32.smooth_blobs(complete) == ((0, 0, 6, 4),),
          "complete grid merges into one blob")
    _assert_blob_coverage(complete, bitsquiggle32.smooth_blobs(complete))

    for value in range(2000):
        connections = bitsquiggle32.spec(value)["connections"]
        _assert_blob_coverage(connections, bitsquiggle32.smooth_blobs(connections))

    expect_failure(lambda: bitsquiggle32.smooth_blobs(bytearray(57)),
                   "reject short smooth edge mask")
    invalid = bytearray(bitsquiggle32.EDGE_COUNT)
    invalid[0] = 2
    expect_failure(lambda: bitsquiggle32.smooth_blobs(invalid),
                   "reject non-binary smooth edge mask")


def test_input_validation():
    expect_failure(lambda: bitsquiggle32.spec(-1), "reject negative")
    expect_failure(lambda: bitsquiggle32.spec(0x100000000), "reject >32 bit")
    expect_failure(lambda: bitsquiggle32.spec(0, "unknown"), "reject style")


def test_shared_fixture_under_cpython():
    try:
        import json
        import os
        import sys
    except ImportError:
        return
    implementation = getattr(getattr(sys, "implementation", None), "name", "")
    if implementation != "cpython":
        return

    fixture_path = os.path.join(
        os.path.dirname(__file__), "..", "fixtures", "v1.json")
    with open(fixture_path, "r", encoding="utf-8") as fixture_file:
        fixture = json.load(fixture_file)
    check(fixture["schema"] == "bitsquiggles-conformance", "known fixture schema")
    check(fixture["version"] == 1, "known fixture version")
    check(fixture["dimensions"] == {
        "rows": 7, "columns": 5, "edges": 58, "pixelWidth": 16, "pixelHeight": 22,
    }, "known fixture dimensions")
    for vector in fixture["vectors"]:
        bits = int(vector["input"], 16)
        visual = bitsquiggle32.spec(bits)
        grid = bitsquiggle32.pixels(bits)
        check("%08x" % visual["mixed"] == vector["mixed"], "fixture mixed")
        check("".join(str(value) for value in visual["connections"]) == vector["connections"],
              "fixture connections")
        check(visual["preferred_mode"] == vector["preferredMode"], "fixture preferred mode")
        check(visual["actual_mode"] == vector["actualMode"], "fixture actual mode")
        check(visual["fallback"] == vector["fallback"], "fixture fallback")
        check("".join(str(value) for value in grid["pixels"]) == vector["pixels"],
              "fixture pixels")
        for style, colors in vector["styles"].items():
            styled = bitsquiggle32.spec(bits, style)
            check(styled["background"]["hex"] == colors["background"], "fixture background")
            check(styled["foreground"]["hex"] == colors["foreground"], "fixture foreground")
            check(styled["connections"] == visual["connections"], "fixture style geometry")


def main():
    test_edges_and_mode_capacities()
    test_public_surface()
    test_modes_and_canonical_priority()
    test_input_bit_avalanche()
    test_sampled_monochrome_injectivity()
    test_black_and_white_colors_and_polarity()
    test_pixel_renderer_is_lossless()
    test_framebuffer_renderer_is_exact()
    test_colors_parity_and_golden_vector()
    test_slash_wrap_regression()
    test_smooth_blobs()
    test_input_validation()
    test_shared_fixture_under_cpython()
    print("BitSquiggles MicroPython tests passed (%d checks)" % _checks)


if __name__ == "__main__":
    main()
