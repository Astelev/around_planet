package io.github.astelev.cruising.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemporaryViewTest {
    @Test
    void boundsFollowMovementAndIncludeNegativeCoordinates() {
        var oldView = new TemporaryView(-20, 30, 2);
        var newView = new TemporaryView(100, 200, 2);
        assertTrue(oldView.contains(-22, 32));
        assertFalse(oldView.contains(-23, 30));
        assertFalse(newView.contains(-20, 30));
        assertTrue(newView.contains(100, 200));
    }

    @Test
    void rejectsUnboundedViewsAndAvoidsCoordinateOverflow() {
        assertThrows(IllegalArgumentException.class, () -> new TemporaryView(0, 0, 33));
        assertThrows(IllegalArgumentException.class, () -> new TemporaryView(0, 0, -1));
        assertFalse(new TemporaryView(Integer.MIN_VALUE, 0, 32).contains(Integer.MAX_VALUE, 0));
    }
}
