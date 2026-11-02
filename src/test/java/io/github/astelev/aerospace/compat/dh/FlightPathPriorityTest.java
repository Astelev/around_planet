package io.github.astelev.aerospace.compat.dh;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FlightPathPriorityTest {
    @Test void travelAddsWeightWithoutReplacingDistance() {
        var east = FlightPathPriority.velocity(100, 0);
        int ahead = FlightPathPriority.distance(1000, 1000, 0, east);
        int behind = FlightPathPriority.distance(1000, -1000, 0, east);
        assertTrue(ahead < 1000 && behind > 1000);
        assertTrue(FlightPathPriority.distance(513, 513, 0, east) > 512);
        assertEquals(200, FlightPathPriority.distance(200, -200, 0, east));
        assertTrue(FlightPathPriority.distance(400, -400, 0, east) < ahead);
        assertTrue(ahead < FlightPathPriority.distance(5000, 5000, 0, east));
    }
    @Test void turnsStopsAndTeleportsDoNotLeaveOldHeading() {
        var west = FlightPathPriority.velocity(-100, 0);
        assertTrue(FlightPathPriority.distance(1000, -1000, 0, west) < 1000);
        assertEquals(1000, FlightPathPriority.distance(1000, 1000, 0, FlightPathPriority.velocity(0,0)));
        assertFalse(FlightPathPriority.velocity(100000, 0).moving());
        assertFalse(FlightPathPriority.velocity(Double.NaN, 0).moving());
        DhFlightState.publish(true, 4);
        DhFlightState.travelVelocity(100, 0);
        DhFlightState.publish(false, 0);
        assertFalse(DhFlightState.travelMotion().moving());
    }
}
