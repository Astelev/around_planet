package io.github.astelev.around_planet.topology;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EastWestTopologyTest {
    private final EastWestTopology world = new EastWestTopology(1024);
    @Test void exactEastBoundaryMapsToWestButWestRemainsInside() {
        assertEquals(-512,world.canonicalX(512));
        assertFalse(world.outside(-512));
        assertTrue(world.outside(512));
    }
    @Test void fractionalOvershootIsPreservedInBothDirections() {
        assertEquals(-511.875,world.canonicalX(512.125));
        assertEquals(511.875,world.canonicalX(-512.125));
    }
    @Test void multipleCircumferencesMapInOneOperation() {
        assertEquals(-500.25,world.canonicalX(512+1024*100+11.75));
        assertEquals(500.25,world.canonicalX(-512-1024*100-11.75));
    }
    @Test void interiorCoordinatesDoNotMove() {
        for(double x:new double[]{-512,-511.5,-0.25,0,511.999}) assertEquals(x,world.canonicalX(x));
    }
    @Test void periodicImagesShareTheSameCoordinate() {
        for(int turns=-20;turns<=20;turns++) for(double x:new double[]{-511.75,-3.5,0,123.25,511.75})
            assertEquals(x,world.canonicalX(x+turns*1024));
    }
    @Test void widthMustBeChunkAlignedAndBounded() {
        assertThrows(IllegalArgumentException.class,()->new EastWestTopology(255));
        assertThrows(IllegalArgumentException.class,()->new EastWestTopology(1000));
        assertThrows(IllegalArgumentException.class,()->new EastWestTopology(60_000_000));
        assertDoesNotThrow(()->new EastWestTopology(256));
    }
    @Test void rejectsNonFiniteCoordinates() {
        for(double x:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->world.canonicalX(x));
    }
}
