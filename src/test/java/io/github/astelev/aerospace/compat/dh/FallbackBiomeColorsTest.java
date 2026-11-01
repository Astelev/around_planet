package io.github.astelev.aerospace.compat.dh;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FallbackBiomeColorsTest {
    @AfterEach void clear() { FallbackBiomeColors.clear(); }

    @Test void noColorIsInventedBeforeAnObservation() {
        FallbackBiomeColors.setView("world", 0, 0);
        assertEquals(123, FallbackBiomeColors.color(0, 0, 123));
        FallbackBiomeColors.observe("world", 0, 0, 0x34ab67);
        assertEquals(0x34ab67, FallbackBiomeColors.color(0, 0, 123));
    }

    @Test void onlySparseLocalExistingSamplesAreWanted() {
        FallbackBiomeColors.setView("world", -1, -1);
        assertTrue(FallbackBiomeColors.wantsSample(-256, -256));
        assertFalse(FallbackBiomeColors.wantsSample(-255, -256));
        assertFalse(FallbackBiomeColors.wantsSample(1000000, 1000000));
        FallbackBiomeColors.observe("world", 1000000, 1000000, 123);
        assertEquals(456, FallbackBiomeColors.color(3906, 3906, 456));
    }

    @Test void oldAndOtherDimensionsCannotProvideColors() {
        FallbackBiomeColors.setView("one", 0, 0);
        FallbackBiomeColors.observe("one", 0, 0, 123);
        FallbackBiomeColors.setView("two", 0, 0);
        FallbackBiomeColors.observe("one", 0, 0, 789);
        assertEquals(456, FallbackBiomeColors.color(0, 0, 456));
        FallbackBiomeColors.clear();
        assertFalse(FallbackBiomeColors.wantsSample(0, 0));
    }

    @Test void movementPrunesDistantCells() {
        FallbackBiomeColors.setView("world", 0, 0);
        FallbackBiomeColors.observe("world", 0, 0, 123);
        FallbackBiomeColors.setView("world", 1000000, 1000000);
        assertEquals(456, FallbackBiomeColors.color(0, 0, 456));
    }
}
