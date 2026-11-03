package io.github.astelev.cruising.compat.dh;

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

    @Test void backgroundSamplesStartUnderCameraAndCannotLeakAcrossWorldReload() {
        FallbackBiomeColors.setView("world", -1, -1);
        var batch = FallbackBiomeColors.missingNearest(10);
        assertEquals(new FallbackBiomeColors.Cell(-1, -1), batch.cells().getFirst());
        assertEquals(10, batch.cells().size());
        FallbackBiomeColors.observe(batch.generation(), "world", -64, -64, 123);
        assertFalse(FallbackBiomeColors.missingNearest(10).cells().contains(new FallbackBiomeColors.Cell(-1, -1)));
        FallbackBiomeColors.clear();
        FallbackBiomeColors.setView("world", -1, -1);
        FallbackBiomeColors.observe(batch.generation(), "world", -64, -64, 789);
        assertEquals(456, FallbackBiomeColors.color(-1, -1, 456));
    }

    @Test void finerGridKeepsCoverageAndDistinguishesNeighboring64BlockTiles() {
        assertEquals(64, FallbackBiomeColors.CELL_SIZE);
        assertEquals(8448, FallbackBiomeColors.WIDTH * FallbackBiomeColors.CELL_SIZE);
        FallbackBiomeColors.setView("world",0,0);
        FallbackBiomeColors.observe("world",0,0,123,8);
        FallbackBiomeColors.observe("world",64,0,456,40);
        assertEquals(123,FallbackBiomeColors.color(0,0,789));
        assertEquals(456,FallbackBiomeColors.color(1,0,789));
        assertEquals(40,FallbackBiomeColors.heightOffset(1,0));
        assertEquals(128,FallbackBiomeColors.missingNearest(128).cells().stream().distinct().count());
    }

    @Test void movementPrunesDistantCells() {
        FallbackBiomeColors.setView("world", 0, 0);
        FallbackBiomeColors.observe("world", 0, 0, 123);
        FallbackBiomeColors.setView("world", 1000000, 1000000);
        assertEquals(456, FallbackBiomeColors.color(0, 0, 456));
    }
}
