package io.github.astelev.around_planet.border;

import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.world.level.border.WorldBorder;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Topology lookup for integrations; never changes vanilla border bounds or collision. */
public final class PlanetBorders {
    private static final Map<WorldBorder, EastWestTopology> BORDERS = Collections.synchronizedMap(new WeakHashMap<>());
    public static void bind(WorldBorder border, EastWestTopology topology) { BORDERS.put(border, topology); }
    public static void unbind(WorldBorder border) { BORDERS.remove(border); }
    public static EastWestTopology get(WorldBorder border) { return BORDERS.get(border); }
    private PlanetBorders() {}
}
