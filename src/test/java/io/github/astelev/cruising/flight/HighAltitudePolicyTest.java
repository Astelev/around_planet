package io.github.astelev.cruising.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighAltitudePolicyTest {
    private boolean eligible(double lowestPoint) {
        return HighAltitudePolicy.maySkipTerrainTicket(true, true, false, lowestPoint, 320, 356);
    }

    @Test
    void requiresEntireHullStrictlyAboveThreshold() {
        assertFalse(eligible(355.9));
        assertFalse(eligible(356));
        assertTrue(eligible(356.01));
    }

    @Test
    void descentRestoresNormalRequestsWithoutPersistentState() {
        assertTrue(eligible(500));
        assertFalse(eligible(350));
        assertTrue(eligible(500));
    }

    @Test
    void protectsTallerWorldsEvenIfConfiguredThresholdIsTooLow() {
        assertFalse(HighAltitudePolicy.maySkipTerrainTicket(true, true, false, 400, 512, 356));
        assertFalse(HighAltitudePolicy.maySkipTerrainTicket(true, true, false, 533, 512, 356));
        assertTrue(HighAltitudePolicy.maySkipTerrainTicket(true, true, false, 534, 512, 356));
    }

    @Test
    void neverInterceptsDisabledOtherDimensionOrPlotRequests() {
        assertFalse(HighAltitudePolicy.maySkipTerrainTicket(false, true, false, 500, 320, 356));
        assertFalse(HighAltitudePolicy.maySkipTerrainTicket(true, false, false, 500, 320, 356));
        assertFalse(HighAltitudePolicy.maySkipTerrainTicket(true, true, true, 500, 320, 356));
    }

    @Test
    void rejectsInvalidBounds() {
        assertFalse(eligible(Double.NaN));
        assertFalse(eligible(Double.POSITIVE_INFINITY));
        assertFalse(eligible(Double.NEGATIVE_INFINITY));
    }
}
