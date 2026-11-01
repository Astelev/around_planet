package io.github.astelev.aerospace.flight;

/** A budget shared by all physics substeps in the same server tick. */
public final class TickWorkBudget {
    private final int limit;
    private long tick = Long.MIN_VALUE;
    private int used;

    public TickWorkBudget(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Work limit must be positive");
        }
        this.limit = limit;
    }

    public boolean acquire(long now) {
        if (tick != now) {
            tick = now;
            used = 0;
        }
        if (used == limit) {
            return false;
        }
        used++;
        return true;
    }
}
