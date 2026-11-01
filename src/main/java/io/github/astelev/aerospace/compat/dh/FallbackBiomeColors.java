package io.github.astelev.aerospace.compat.dh;

import java.util.concurrent.ConcurrentHashMap;

/** Bounded, disposable colors observed from DH's existing biome work. No queries or generation. */
public final class FallbackBiomeColors {
    public static final int CELL_SIZE = 256;
    public static final int HALF_SIZE = 17;
    public static final int WIDTH = HALF_SIZE * 2;
    private static final ConcurrentHashMap<Long, Integer> COLORS = new ConcurrentHashMap<>();
    private static volatile View view;
    private static volatile long generation;

    private FallbackBiomeColors() {}

    public static synchronized void setView(String dimension, int blockX, int blockZ) {
        var next = new View(dimension, Math.floorDiv(blockX, CELL_SIZE), Math.floorDiv(blockZ, CELL_SIZE));
        var previous = view;
        if (next.equals(previous)) return;
        if (previous == null || !previous.dimension.equals(dimension)) {
            COLORS.clear();
            generation++;
        }
        view = next;
        COLORS.keySet().removeIf(key -> !next.contains((int) (key >> 32), (int) (long) key));
    }

    public static boolean wantsSample(int blockX, int blockZ) {
        var snapshot = view;
        return snapshot != null && Math.floorMod(blockX, CELL_SIZE) == 0 && Math.floorMod(blockZ, CELL_SIZE) == 0
                && snapshot.contains(Math.floorDiv(blockX, CELL_SIZE), Math.floorDiv(blockZ, CELL_SIZE));
    }

    public static synchronized void observe(String dimension, int blockX, int blockZ, int rgb) {
        var snapshot = view;
        int x = Math.floorDiv(blockX, CELL_SIZE), z = Math.floorDiv(blockZ, CELL_SIZE);
        if (snapshot != null && snapshot.dimension.equals(dimension) && snapshot.contains(x, z)
                && COLORS.size() < WIDTH * WIDTH * 2) {
            COLORS.put(key(x, z), rgb & 0xffffff);
        }
    }

    public static int color(int cellX, int cellZ, int fallback) {
        return COLORS.getOrDefault(key(cellX, cellZ), fallback);
    }

    public static synchronized void clear() {
        if (view != null || !COLORS.isEmpty()) generation++;
        view = null;
        COLORS.clear();
    }

    public static long generation() { return generation; }

    private static long key(int x, int z) { return (long) x << 32 | z & 0xffffffffL; }
    private record View(String dimension, int x, int z) {
        private boolean contains(int cx, int cz) {
            return (long) cx >= (long) x - HALF_SIZE && (long) cx < (long) x + HALF_SIZE
                    && (long) cz >= (long) z - HALF_SIZE && (long) cz < (long) z + HALF_SIZE;
        }
    }
}
