package io.github.astelev.aerospace.compat.dh;

import java.util.concurrent.ConcurrentHashMap;

/** Bounded, disposable surface palette shared by biome sampling and DH observation. */
public final class FallbackBiomeColors {
    public static final int CELL_SIZE = 64;
    public static final int HALF_SIZE = 66;
    public static final int WIDTH = HALF_SIZE * 2;
    private static final ConcurrentHashMap<Long, Sample> COLORS = new ConcurrentHashMap<>();
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
        observe(dimension, blockX, blockZ, rgb, 0);
    }

    public static synchronized void observe(String dimension, int blockX, int blockZ, int rgb, int heightOffset) {
        var snapshot = view;
        int x = Math.floorDiv(blockX, CELL_SIZE), z = Math.floorDiv(blockZ, CELL_SIZE);
        if (snapshot != null && snapshot.dimension.equals(dimension) && snapshot.contains(x, z)
                && COLORS.size() < WIDTH * WIDTH * 2) {
            COLORS.put(key(x, z), new Sample(rgb & 0xffffff, Math.max(0, Math.min(192, heightOffset))));
        }
    }

    public static int color(int cellX, int cellZ, int fallback) {
        var sample = COLORS.get(key(cellX, cellZ));
        return sample == null ? fallback : sample.rgb;
    }

    public static int heightOffset(int cellX, int cellZ) {
        var sample = COLORS.get(key(cellX, cellZ));
        return sample == null ? 0 : sample.heightOffset;
    }
    private record Sample(int rgb, int heightOffset) {}

    public static synchronized void clear() {
        if (view != null || !COLORS.isEmpty()) generation++;
        view = null;
        COLORS.clear();
    }

    /** Read a bounded nearest-first batch; running work carries this world's epoch. */
    public static synchronized Batch missingNearest(int limit) {
        var snapshot = view;
        var cells = new java.util.ArrayList<Cell>();
        if (snapshot != null && COLORS.size() < WIDTH * WIDTH) {
            addMissing(cells, snapshot, snapshot.x, snapshot.z, limit);
            for (int radius = 1; radius <= HALF_SIZE && cells.size() < limit; radius++) {
                for (int dx = -radius; dx <= radius && cells.size() < limit; dx++) {
                    addMissing(cells, snapshot, snapshot.x + dx, snapshot.z - radius, limit);
                    addMissing(cells, snapshot, snapshot.x + dx, snapshot.z + radius, limit);
                }
                for (int dz = -radius + 1; dz < radius && cells.size() < limit; dz++) {
                    addMissing(cells, snapshot, snapshot.x - radius, snapshot.z + dz, limit);
                    addMissing(cells, snapshot, snapshot.x + radius, snapshot.z + dz, limit);
                }
            }
        }
        return new Batch(generation, snapshot == null ? "" : snapshot.dimension, java.util.List.copyOf(cells));
    }

    private static void addMissing(java.util.List<Cell> cells, View view, int x, int z, int limit) {
        if (cells.size() < limit && view.contains(x, z) && !COLORS.containsKey(key(x, z))) cells.add(new Cell(x, z));
    }

    public static synchronized void observe(long epoch, String dimension, int x, int z, int color) {
        if (epoch == generation) observe(dimension, x, z, color);
    }

    public static synchronized void observe(long epoch, String dimension, int x, int z, int color, int height) {
        if (epoch == generation) observe(dimension, x, z, color, height);
    }

    public record Cell(int x, int z) {}
    public record Batch(long generation, String dimension, java.util.List<Cell> cells) {}

    public static int knownCount() { return COLORS.size(); }

    public static long generation() { return generation; }

    private static long key(int x, int z) { return (long) x << 32 | z & 0xffffffffL; }
    private record View(String dimension, int x, int z) {
        private boolean contains(int cx, int cz) {
            return (long) cx >= (long) x - HALF_SIZE && (long) cx < (long) x + HALF_SIZE
                    && (long) cz >= (long) z - HALF_SIZE && (long) cz < (long) z + HALF_SIZE;
        }
    }
}
