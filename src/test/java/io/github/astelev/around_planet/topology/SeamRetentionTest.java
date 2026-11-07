package io.github.astelev.around_planet.topology;
import io.github.astelev.around_planet.seam.SeamRetention;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SeamRetentionTest {
    @Test void oldShoreRemainsInViewAfterBothCrossingDirections() {
        assertTrue(SeamRetention.inView(4095,40,-4096,40,8192,12));
        assertTrue(SeamRetention.inView(-4096,40,4095,40,8192,12));
        assertTrue(SeamRetention.inView(4085,48,-4096,40,8192,12));
        assertFalse(SeamRetention.inView(4000,40,-4096,40,8192,12));
        assertFalse(SeamRetention.inView(4095,80,-4096,40,8192,12));
    }
    @Test void ordinaryInteriorDistanceAndSmallWorldWrappingAreBounded() {
        assertTrue(SeamRetention.inView(15,0,-16,0,32,4));
        assertFalse(SeamRetention.inView(6,0,-16,0,32,4));
        assertTrue(SeamRetention.inView(102,101,100,100,8192,4));
        assertFalse(SeamRetention.inView(120,101,100,100,8192,4));
    }
}
