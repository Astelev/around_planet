package io.github.astelev.around_planet.border;

import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.world.level.border.WorldBorder;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Only explicitly bound Overworld borders are changed. Saved vanilla border settings remain intact. */
public final class PlanetBorders {
    public static final double GUARD_MARGIN = 1024;
    public static double guardMargin() {
        return io.github.astelev.around_planet.PlanetConfig.SPEC.isLoaded()
                ?io.github.astelev.around_planet.PlanetConfig.GUARD_MARGIN.getAsInt():GUARD_MARGIN;
    }
    private record Binding(EastWestTopology topology, double boundaryZ) {}
    private static final Map<WorldBorder, Binding> BORDERS = Collections.synchronizedMap(new WeakHashMap<>());
    public static void bind(WorldBorder border, EastWestTopology topology) { bind(border,topology,Double.POSITIVE_INFINITY); }
    public static void bind(WorldBorder border, EastWestTopology topology,double boundaryZ) {
        if(!(boundaryZ>0) || Double.isNaN(boundaryZ))throw new IllegalArgumentException("Invalid north/south boundary");
        BORDERS.put(border,new Binding(topology,boundaryZ));
    }
    public static double boundaryZ(WorldBorder border) {
        var binding=BORDERS.get(border);
        return binding==null || get(border)==null?Double.POSITIVE_INFINITY:binding.boundaryZ();
    }
    public static void unbind(WorldBorder border) { BORDERS.remove(border); }
    public static EastWestTopology get(WorldBorder border) {
        var binding=BORDERS.get(border);
        var topology=binding==null?null:binding.topology();
        // Respect an existing vanilla border whose entire area lies outside the configured planet.
        if(topology!=null && (border.getCenterX()-border.getSize()/2 >= topology.east()+guardMargin()
                || border.getCenterX()+border.getSize()/2 <= topology.west()-guardMargin()
                || border.getCenterZ()-border.getSize()/2 >= binding.boundaryZ()
                || border.getCenterZ()+border.getSize()/2 <= -binding.boundaryZ())) return null;
        return topology;
    }
    /** No danger vignette for a traversable seam. Keep warnings for a genuinely tighter vanilla border. */
    public static double warningDistance(WorldBorder border, net.minecraft.world.entity.Entity entity) {
        var topology=get(border);
        if(topology==null) return border.getDistanceToBorder(entity);
        double distance=Math.min(entity.getZ()-border.getMinZ(),border.getMaxZ()-entity.getZ());
        double minX=Math.max(-border.getAbsoluteMaxSize(),border.getCenterX()-border.getSize()/2);
        double maxX=Math.min(border.getAbsoluteMaxSize(),border.getCenterX()+border.getSize()/2);
        if(minX>topology.west()) distance=Math.min(distance,entity.getX()-minX);
        if(maxX<topology.east()) distance=Math.min(distance,maxX-entity.getX());
        return distance;
    }
    private PlanetBorders() {}
}
