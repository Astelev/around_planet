package io.github.astelev.aerospace.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TickWorkBudgetTest {
    @Test
    void physicsSubstepsCannotMultiplyPerTickWork() {
        var budget = new TickWorkBudget(2);
        assertTrue(budget.acquire(10));
        assertTrue(budget.acquire(10));
        assertFalse(budget.acquire(10));
        assertFalse(budget.acquire(10));
        assertTrue(budget.acquire(11));
    }
}
