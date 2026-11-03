package io.github.astelev.cruising.client;

import io.github.astelev.cruising.CruisingSpeed;
import io.github.astelev.cruising.compat.dh.LoadedSurfaceTiles;
import io.github.astelev.cruising.compat.dh.FallbackBiomeColors;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Hide approximate tiles once their entire footprint is available as real chunks. */
@EventBusSubscriber(modid = CruisingSpeed.MODID, value = Dist.CLIENT)
public final class SurfaceTerrainMask {
    private static final LoadedSurfaceTiles loaded = new LoadedSurfaceTiles();
    private static ClientLevel world;
    private SurfaceTerrainMask() {}
    public static boolean covered(ClientLevel level, int cellX, int cellZ) {
        if (world != level || !loaded.covered(cellX,cellZ)) return false;
        // Radius changes can discard storage without a ChunkEvent.Unload.
        // Validate only already-complete tiles, with non-generating cache reads.
        int width = FallbackBiomeColors.CELL_SIZE / 16;
        for (int dx=0; dx<width; dx++) for (int dz=0; dz<width; dz++) {
            int x=cellX*width+dx, z=cellZ*width+dz;
            var chunk = level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);
            if (chunk == null || chunk instanceof EmptyLevelChunk) {
                loaded.unload(x,z);
                return false;
            }
        }
        return true;
    }
    public static boolean coveredColumn(ClientLevel level, int x, int z) {
        if(world!=level || !loaded.hasColumn(x,z)) return false;
        var chunk=level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);
        if(chunk==null || chunk instanceof EmptyLevelChunk) { loaded.unload(x,z); return false; }
        return true;
    }
    @SubscribeEvent
    public static void load(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel level) || event.getChunk() instanceof EmptyLevelChunk) return;
        if (world != level) { loaded.clear(); world = level; }
        var pos = event.getChunk().getPos();
        loaded.load(pos.x,pos.z);
    }
    public static void unloadColumn(ClientLevel level, ChunkPos pos) {
        if (level == world) loaded.unload(pos.x,pos.z);
    }
    @SubscribeEvent
    public static void unloadWorld(LevelEvent.Unload event) {
        if (event.getLevel() == world) reset();
    }
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); }
    private static void reset() { world = null; loaded.clear(); }
}
