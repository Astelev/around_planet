package io.github.astelev.around_planet.topology;

import io.github.astelev.around_planet.portal.PortalLoadBudget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalLoadBudgetTest {
    @Test void centersThePreloadInsideTheOppositeCanonicalEdge() {
        assertEquals(-4096,PortalLoadBudget.destinationChunkX(-65536,true));
        assertEquals(4095,PortalLoadBudget.destinationChunkX(65536,false));
        assertEquals(-9,PortalLoadBudget.destinationChunkX(-144,true));
        assertEquals(8,PortalLoadBudget.destinationChunkX(144,false));
    }
    @Test void preparesEarlierAndNeverExceedsEitherLimit() {
        assertEquals(2,PortalLoadBudget.radius(16,16,512,512));
        assertEquals(9,PortalLoadBudget.radius(16,16,512,256));
        assertEquals(15,PortalLoadBudget.radius(16,16,512,64));
        assertEquals(16,PortalLoadBudget.radius(16,16,512,0));
        assertEquals(8,PortalLoadBudget.radius(8,16,512,0));
        assertEquals(12,PortalLoadBudget.radius(32,12,512,0));
    }
    @Test void radiusGrowsMonotonicallyAcrossTheApproachBand() {
        int previous=2;
        for(int distance=512;distance>=0;distance--) {
            int radius=PortalLoadBudget.radius(24,16,512,distance);
            assertTrue(radius>=previous && radius<=16);
            assertTrue(radius-previous<=1);
            previous=radius;
        }
    }
}
