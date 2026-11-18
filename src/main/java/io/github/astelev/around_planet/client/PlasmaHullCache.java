package io.github.astelev.around_planet.client;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import io.github.astelev.around_planet.plasma.ProjectedHull;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Event-invalidated local geometry. No polling, pose comparisons, server scans or chunk requests. */
public final class PlasmaHullCache {
    public record Mesh(BlockPos origin, ProjectedHull hull) {}
    private static final Map<ClientSubLevel, Entry> CACHE = new WeakHashMap<>();
    private static final class Entry { boolean dirty = true; Mesh mesh; }
    public static long builds;
    public static void invalidate(ClientSubLevel ship) { var entry = CACHE.get(ship); if (entry != null) entry.dirty = true; }
    public static Mesh get(ClientSubLevel ship) {
        var entry = CACHE.computeIfAbsent(ship, k -> new Entry());
        if (!entry.dirty) return entry.mesh;
        var origin = ship.getPlot().getCenterBlock(); var builder = new ProjectedHull.Builder();
        for (var holder : ship.getPlot().getLoadedChunks()) {
            var chunk = holder.getChunk(); var sections = chunk.getSections();
            int dx = chunk.getPos().getMinBlockX() - origin.getX(), dz = chunk.getPos().getMinBlockZ() - origin.getZ();
            for (int i = 0; i < sections.length; i++) {
                var section = sections[i]; if (section.hasOnlyAir()) continue;
                int dy = chunk.getSectionYFromSectionIndex(i) * 16 - origin.getY();
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++)
                    if (!section.getBlockState(x, y, z).isAir()) builder.block(dx + x, dy + y, dz + z);
            }
        }
        entry.mesh = new Mesh(origin.immutable(), builder.build(.22)); entry.dirty = false; builds++;
        return entry.mesh;
    }
    private PlasmaHullCache() {}
}
