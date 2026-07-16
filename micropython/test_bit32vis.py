"""Dependency-free Bit32Vis tests, compatible with CPython and MicroPython.

Grug 2-Clause License: do what want; not sue grug.
"""

import bit32vis

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
    check(len(bit32vis.EDGES) == 58, "58 canonical edges")
    previous = None
    for edge in bit32vis.EDGES:
        start_row, start_column, end_row, end_column = edge
        check((end_row - start_row) + (end_column - start_column) == 1,
              "orthogonal unit edge")
        if previous is not None:
            check(previous < edge, "lexical edge order")
        previous = edge
    expected = (32, 31, 29, 33)
    for index, mode in enumerate(bit32vis.MODES):
        check(bit32vis.free_connection_count(mode) == expected[index],
              "free count %s" % mode)


def test_modes_and_canonical_priority():
    seen = {mode: False for mode in bit32vis.MODES}
    saw_fallback = False
    saw_half_turn_capacity_fallback = False
    saw_accepted_half_turn = False
    for value in range(50000):
        visual = bit32vis.spec(value)
        seen[visual["preferred_mode"]] = True
        saw_fallback = saw_fallback or visual["fallback"]
        if visual["preferred_mode"] == bit32vis.HALF_TURN:
            if visual["mixed"] & 1:
                check(visual["fallback"], "half-turn omitted bit fallback")
                saw_half_turn_capacity_fallback = True
            elif not visual["fallback"]:
                saw_accepted_half_turn = True
        check(bit32vis.matches_mode(
            visual["connections"], visual["actual_mode"]), "actual mode match")
        if not visual["fallback"]:
            preferred = bit32vis.MODES.index(visual["preferred_mode"])
            for earlier in bit32vis.MODES[:preferred]:
                check(not bit32vis.matches_mode(visual["connections"], earlier),
                      "canonical mode priority")
        else:
            check(visual["actual_mode"] == bit32vis.LEFT_RIGHT,
                  "fallback mode")
    for mode in bit32vis.MODES:
        check(seen[mode], "mode observed %s" % mode)
    check(saw_fallback, "fallback observed")
    check(saw_half_turn_capacity_fallback, "half-turn capacity fallback observed")
    check(saw_accepted_half_turn, "injective half-turn candidate observed")


def test_input_bit_avalanche():
    base = bit32vis.spec(0)["connections"]
    total = 0
    for bit in range(32):
        changed = bit32vis.spec(1 << bit)["connections"]
        distance = sum(first != second for first, second in zip(base, changed))
        check(distance >= 20, "edge avalanche bit %d" % bit)
        total += distance
    check(total / 32.0 >= 26.0, "average edge avalanche")


def test_sampled_monochrome_injectivity():
    seen = set()
    for value in range(100000):
        visual = bit32vis.spec(value, bit32vis.MONOCHROME)
        mask = bytes(visual["connections"])
        check(mask not in seen, "unique monochrome visual %d" % value)
        seen.add(mask)


def test_pixel_renderer_is_lossless():
    for value in range(1000):
        visual = bit32vis.spec(value)
        grid = bit32vis.pixels(value)
        check(grid["width"] == 16 and grid["height"] == 22, "pixel size")
        check(len(grid["pixels"]) == 352, "pixel storage")
        for x in range(16):
            check(grid["pixels"][x] == 0, "top border")
            check(grid["pixels"][21 * 16 + x] == 0, "bottom border")
        for y in range(22):
            check(grid["pixels"][y * 16] == 0, "left border")
            check(grid["pixels"][y * 16 + 15] == 0, "right border")
        for edge_index, edge in enumerate(bit32vis.EDGES):
            start_row, start_column, end_row, _ = edge
            x = 1 + start_column * 3
            y = 1 + start_row * 3
            if start_row == end_row:
                bridge = grid["pixels"][y * 16 + x + 2]
            else:
                bridge = grid["pixels"][(y + 2) * 16 + x]
            check(bridge == visual["connections"][edge_index],
                  "recoverable edge")


def test_colors_parity_and_golden_vector():
    value = 0x89ABCDEF
    standard = bit32vis.spec(value, bit32vis.STANDARD)
    contrast = bit32vis.spec(value, bit32vis.HIGH_CONTRAST)
    mono = bit32vis.spec(value, bit32vis.MONOCHROME)
    check(standard["connections"] == contrast["connections"], "style edges")
    check(standard["connections"] == mono["connections"], "monochrome edges")
    check(contrast["foreground"]["C"] > standard["foreground"]["C"], "HC chroma")
    close(0.0, mono["foreground"]["C"], "mono foreground chroma")
    close(0.0, mono["background"]["C"], "mono background chroma")
    check(not bit32vis.spec(0)["swapped"], "even parity")
    check(bit32vis.spec(1)["swapped"], "odd parity")

    mask = "".join(str(value) for value in standard["connections"])
    check(standard["mixed"] == 0x47AC5876, "golden mixed")
    check(standard["preferred_mode"] == bit32vis.TOP_BOTTOM, "golden mode")
    check(not standard["fallback"], "golden fallback")
    check(mask == "0001111010110001011000011101010010101100001010011011010011",
          "golden connections")
    check(standard["background"]["hex"] == "#140040", "golden background")
    check(standard["foreground"]["hex"] == "#8d9200", "golden foreground")


def test_slash_wrap_regression():
    visual = bit32vis.spec(0xD9ABCDEF)
    check(visual["actual_mode"] == bit32vis.DIAGONAL_SLASH,
          "slash wrap regression mode")
    check(bit32vis._connection(visual["connections"], 1, 1, 2, 1)
          == bit32vis._connection(visual["connections"], 4, 3, 4, 4),
          "slash copy maps upper vertical to lower horizontal edge")
    check(bit32vis._connection(visual["connections"], 1, 2, 2, 2)
          == bit32vis._connection(visual["connections"], 3, 3, 3, 4),
          "slash copy preserves adjacent diagonal edge relation")


def test_input_validation():
    expect_failure(lambda: bit32vis.spec(-1), "reject negative")
    expect_failure(lambda: bit32vis.spec(0x100000000), "reject >32 bit")
    expect_failure(lambda: bit32vis.spec(0, "unknown"), "reject style")


def main():
    test_edges_and_mode_capacities()
    test_modes_and_canonical_priority()
    test_input_bit_avalanche()
    test_sampled_monochrome_injectivity()
    test_pixel_renderer_is_lossless()
    test_colors_parity_and_golden_vector()
    test_slash_wrap_regression()
    test_input_validation()
    print("Bit32Vis MicroPython tests passed (%d checks)" % _checks)


if __name__ == "__main__":
    main()
