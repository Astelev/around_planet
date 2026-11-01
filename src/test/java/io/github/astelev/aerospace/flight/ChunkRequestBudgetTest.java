package io.github.astelev.aerospace.flight;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ChunkRequestBudgetTest {
    @Test
    void deduplicatesAndLimitsNewRequestsPerGameTick() {
        var queue = new ChunkRequestBudget(2, 4);
        var added = new ArrayList<Long>();
        assertTrue(queue.request(1, 100, added::add));
        assertTrue(queue.request(1, 100, added::add));
        assertTrue(queue.request(2, 100, added::add));
        assertFalse(queue.request(3, 100, added::add));
        assertTrue(queue.request(3, 101, added::add));
        assertTrue(queue.request(4, 101, added::add));
        assertFalse(queue.request(5, 102, added::add));
    }

    @Test
    void expiresUnneededTicketsButRetainsRefreshedRequests() {
        var queue = new ChunkRequestBudget(2, 2);
        queue.request(1, 0, key -> {});
        queue.request(2, 0, key -> {});
        queue.request(2, 15, key -> {});
        var removed = new ArrayList<Long>();
        queue.expire(21, 20, removed::add);
        assertEquals(java.util.List.of(1L), removed);
        assertTrue(queue.request(3, 21, key -> {}));
        assertFalse(queue.request(4, 21, key -> {}));
        queue.expire(100, 20, removed::add);
        assertTrue(queue.request(4, 100, key -> {}));
    }
}
