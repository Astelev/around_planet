package io.github.astelev.aerospace.compat.dh;

import java.util.HashMap;
import java.util.HashSet;

/** Actual client columns only; temporary air never emits chunk-load events. */
public final class LoadedSurfaceTiles {
    private final HashSet<Long> columns = new HashSet<>();
    private final HashMap<Long,Integer> counts = new HashMap<>();
    private static final int CHUNKS_PER_TILE = FallbackBiomeColors.CELL_SIZE / 16;
    public void load(int x, int z) {
        if (columns.add(key(x,z))) counts.merge(tile(x,z),1,Integer::sum);
    }
    public void unload(int x, int z) {
        if (columns.remove(key(x,z))) counts.computeIfPresent(tile(x,z),(key,value) -> value == 1 ? null : value - 1);
    }
    public boolean hasColumn(int x, int z) { return columns.contains(key(x,z)); }
    public boolean covered(int cellX, int cellZ) {
        return counts.getOrDefault(key(cellX,cellZ),0) == CHUNKS_PER_TILE * CHUNKS_PER_TILE;
    }
    public void clear() { columns.clear(); counts.clear(); }
    private static long tile(int x, int z) { return key(Math.floorDiv(x,CHUNKS_PER_TILE),Math.floorDiv(z,CHUNKS_PER_TILE)); }
    private static long key(int x, int z) { return (long) x << 32 | z & 0xffffffffL; }
}
