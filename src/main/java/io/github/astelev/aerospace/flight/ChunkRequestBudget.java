package io.github.astelev.aerospace.flight;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongConsumer;

/** Main-thread, deduplicated requests with bounded admission and idle expiry. */
public final class ChunkRequestBudget {
    private final int perTick;
    private final int capacity;
    private final Map<Long, Long> active = new LinkedHashMap<>();
    private long budgetTick = Long.MIN_VALUE;
    private int admitted;

    public ChunkRequestBudget(int perTick, int capacity) {
        if (perTick < 1 || capacity < perTick) {
            throw new IllegalArgumentException("Invalid chunk request limits");
        }
        this.perTick = perTick;
        this.capacity = capacity;
    }

    public boolean request(long key, long tick, LongConsumer add) {
        if (budgetTick != tick) {
            budgetTick = tick;
            admitted = 0;
        }
        if (active.containsKey(key)) {
            long previousTick = active.get(key);
            active.put(key, tick);
            // Refresh vanilla's expiring ticket while it is still needed.
            if (previousTick != tick) {
                add.accept(key);
            }
            return true;
        }
        if (admitted >= perTick || active.size() >= capacity) {
            return false;
        }
        add.accept(key);
        active.put(key, tick);
        admitted++;
        return true;
    }

    public void expire(long tick, long idleTicks, LongConsumer remove) {
        active.entrySet().removeIf(entry -> {
            if (tick - entry.getValue() > idleTicks) {
                remove.accept(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
