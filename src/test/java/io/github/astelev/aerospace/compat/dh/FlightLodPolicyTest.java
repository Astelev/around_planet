package io.github.astelev.aerospace.compat.dh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightLodPolicyTest {
    private FlightLodPolicy policy() {
        return new FlightLodPolicy(4, 20, 5000, 10000);
    }

    @Test
    void startsCoarseEvenWhenEnteringSkyModeStationary() {
        assertEquals(4, policy().sample(true, 0, 1000));
    }

    @Test
    void continuousFastFlightNeverStartsRefining() {
        var policy = policy();
        assertEquals(4, policy.sample(true, 1000, 0));
        assertEquals(4, policy.sample(true, 1000, 100000));
        assertEquals(4, policy.sample(true, 0, 104999));
    }

    @Test
    void refinesInTimeStepsAfterSlowingDown() {
        var policy = policy();
        assertEquals(4, policy.sample(true, 100, 0));
        assertEquals(4, policy.sample(true, 0, 4999));
        assertEquals(3, policy.sample(true, 0, 5000));
        assertEquals(2, policy.sample(true, 0, 15000));
        assertEquals(1, policy.sample(true, 0, 25000));
        assertEquals(0, policy.sample(true, 0, 35000));
        assertEquals(0, policy.sample(true, 0, 100000));
        assertEquals(4, policy.sample(true, 20, 100001));
    }

    @Test
    void exitRestoresNormalDetailImmediatelyAndReentryStartsFresh() {
        var policy = policy();
        policy.sample(true, 100, 0);
        assertEquals(0, policy.sample(false, 100, 1));
        assertEquals(4, policy.sample(true, 0, 100000));
    }

    @Test
    void unknownSpeedDoesNotTriggerPrematureRefinement() {
        var policy = policy();
        policy.sample(true, 0, 0);
        assertEquals(4, policy.sample(true, Double.NaN, 100000));
    }
}
