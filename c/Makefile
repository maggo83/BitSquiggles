CC ?= cc
CFLAGS ?= -std=c99 -Wall -Wextra -Werror -pedantic
LDLIBS ?= -lm
BUILD_DIR ?= build
TEST_BIN := $(BUILD_DIR)/test_bitsquiggle32

.PHONY: all test clean

all: $(TEST_BIN)

$(TEST_BIN): bitsquiggle32.c bitsquiggle32.h test_bitsquiggle32.c | $(BUILD_DIR)
	$(CC) $(CFLAGS) bitsquiggle32.c test_bitsquiggle32.c $(LDLIBS) -o $@

$(BUILD_DIR):
	mkdir -p $@

test: $(TEST_BIN)
	$(TEST_BIN) ../fixtures/v1.json

clean:
	rm -rf $(BUILD_DIR)
