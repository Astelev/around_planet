package io.github.astelev.cruising.compat.dh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DhGenerationPriorityTest {
    @Test void containingSectionWinsEvenWithOffCenterPlayer() {
        assertTrue(DhGenerationPriority.score(-1, 10) < DhGenerationPriority.score(1, 6));
        assertTrue(DhGenerationPriority.score(100, 8) < DhGenerationPriority.score(200, 12));
    }
    @Test void tiesPreferLocalDetailAndLargeDistancesDoNotOverflow() {
        assertTrue(DhGenerationPriority.score(0, 6) < DhGenerationPriority.score(0, 10));
        assertTrue(DhGenerationPriority.score(Integer.MAX_VALUE, 31) > DhGenerationPriority.score(10000, 6));
    }
}
