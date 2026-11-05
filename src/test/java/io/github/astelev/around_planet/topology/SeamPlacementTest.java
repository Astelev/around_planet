package io.github.astelev.around_planet.topology;
import io.github.astelev.around_planet.seam.SeamPlacement;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SeamPlacementTest {
    @Test void departureShoreRemainsAdjacentAfterBothCrossings() {
        assertEquals(4095,SeamPlacement.nearest(4095,4095,8192));
        assertEquals(-4097,SeamPlacement.nearest(4095,-4096,8192));
        assertEquals(4096,SeamPlacement.nearest(-4096,4095,8192));
        assertEquals(-4096,SeamPlacement.nearest(-4096,-4096,8192));
    }
    @Test void repeatsRemainPeriodicAndRejectInvalidWidth() {
        for(int camera=-100;camera<100;camera++) {
            int image=SeamPlacement.nearest(-15,camera,32);
            assertEquals(0,Math.floorMod(image+15,32));
            assertTrue(Math.abs(image-camera)<=16);
        }
        assertThrows(IllegalArgumentException.class,()->SeamPlacement.nearest(0,0,0));
    }
}
