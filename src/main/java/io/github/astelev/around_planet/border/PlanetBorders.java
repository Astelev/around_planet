package io.github.astelev.around_planet.border;

import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.world.level.border.WorldBorder;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Only explicitly bound Overworld borders are changed. Saved vanilla border settings remain intact. */
public final class PlanetBorders {
    public static final double GUARD_MARGIN = 8;
    private static final Map<WorldBorder, EastWestTopology> BORDERS = Collections.synchronizedMap(new WeakHashMap<>());
    public static void bind(WorldBorder border, EastWestTopology topology) { BORDERS.put(border, topology); }
    public static void unbind(WorldBorder border) { BORDERS.remove(border); }
    public static EastWestTopology get(WorldBorder border) {
        var topology=BORDERS.get(border);
        // Respect an existing vanilla border whose entire area lies outside the configured planet.
        if(topology!=null && (border.getCenterX()-border.getSize()/2 >= topology.east()+GUARD_MARGIN
                || border.getCenterX()+border.getSize()/2 <= topology.west()-GUARD_MARGIN)) return null;
        return topology;
    }
    private PlanetBorders() {}
}
