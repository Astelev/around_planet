package io.github.astelev.around_planet.seam;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID)
public final class SeamTicketBounds {
    private static final Map<DistanceManager,ServerLevel> BORDERS=Collections.synchronizedMap(new WeakHashMap<>());
    @SubscribeEvent public static void load(LevelEvent.Load event) {
        if(event.getLevel() instanceof ServerLevel level) BORDERS.put(level.getChunkSource().chunkMap.getDistanceManager(),level);
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if(event.getLevel() instanceof ServerLevel level) BORDERS.remove(level.getChunkSource().chunkMap.getDistanceManager());
    }
    public static boolean allow(DistanceManager manager,int x,int z) {
        var level=BORDERS.get(manager);
        return level==null || !SeamGeometry.clipped(level,x,z);
    }
    private SeamTicketBounds() {}
}
