package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.network.SeamChunkPayload;
import io.github.astelev.around_planet.seam.SeamGeometry;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.BitSet;
import java.util.Iterator;

/** Read only by render-region snapshots. Not inserted into the gameplay chunk cache. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamImages {
    private static final ConcurrentHashMap<Long,LevelChunk> IMAGES = new ConcurrentHashMap<>();
    private static volatile ClientLevel world;
    private static volatile LevelChunk empty;
    private static volatile SeamImageLight renderLight;
    public static net.minecraft.world.level.lighting.LevelLightEngine renderLight(ClientLevel level) {
        var light=renderLight;if(light==null || light.level!=level)renderLight=light=new SeamImageLight(level);
        return light;
    }
    static DataLayer lightData(ClientLevel level,LightLayer layer,SectionPos section) {
        var chunk=get(level,section.x(),section.z());
        return chunk instanceof ImageChunk image?image.light(layer,section.y()):null;
    }
    public static LevelChunk get(ClientLevel level,int x,int z) { return level==world ? IMAGES.get(ChunkPos.asLong(x,z)) : null; }
    public static int size() { return IMAGES.size(); }
    public static java.util.List<ChunkPos> positions(ClientLevel level) {
        return level==world?IMAGES.values().stream().map(LevelChunk::getPos).toList():java.util.List.of();
    }
    public static LevelChunk forRender(ClientLevel level,int x,int z) {
        var image=get(level,x,z);
        if(image!=null) return image;
        var fallback=empty;
        if(fallback==null || fallback.getLevel()!=level) {
            fallback=new EmptyLevelChunk(level,new ChunkPos(0,0),level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS));
            empty=fallback;
        }
        return fallback;
    }
    public static int tint(ClientLevel level,net.minecraft.core.BlockPos pos,ColorResolver resolver) {
        var chunk=get(level,pos.getX()>>4,pos.getZ()>>4);
        if(chunk==null) return 0xffffff;
        var biome=chunk.getNoiseBiome(pos.getX()>>2,pos.getY()>>2,pos.getZ()>>2).value();
        var topology=PlanetBorders.get(level.getWorldBorder());
        return resolver.getColor(biome,topology.canonicalX(pos.getX()),pos.getZ());
    }
    public static void accept(SeamChunkPayload payload) {
        var mc=Minecraft.getInstance(); var level=mc.level;
        if(level==null || mc.player==null || !level.dimension().location().equals(payload.dimension())) return;
        var topology=PlanetBorders.get(level.getWorldBorder());
        int range=Math.max(mc.options.getEffectiveRenderDistance()+2,PlanetConfig.RADIUS.getAsInt()+3);
        if(topology==null || SeamGeometry.canonical(topology,payload.x())
                || Math.abs((long)payload.x()-mc.player.chunkPosition().x)>range
                || Math.abs((long)payload.z()-mc.player.chunkPosition().z)>PlanetConfig.RADIUS.getAsInt()+2) return;
        var buf=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()),level.registryAccess());
        try {
            var packet=ClientboundLevelChunkWithLightPacket.STREAM_CODEC.decode(buf);
            if(packet.getX()!=SeamGeometry.sourceX(topology,payload.x()) || packet.getZ()!=payload.z()) return;
            if(world!=level) { IMAGES.clear(); world=level; }
            if(IMAGES.size()>=192 && !IMAGES.containsKey(ChunkPos.asLong(payload.x(),payload.z()))) return;
            var chunk=new ImageChunk(level,new ChunkPos(payload.x(),payload.z()));
            var data=packet.getChunkData();
            chunk.replaceWithPacketData(data.getReadBuffer(),data.getHeightmaps(),data.getBlockEntitiesTagsConsumer(payload.x(),payload.z()));
            var light=packet.getLightData();
            int count=level.getSectionsCount()+2;
            chunk.minLightSection=level.getMinSection()-1;
            chunk.sky=readLight(count,light.getSkyYMask(),light.getEmptySkyYMask(),light.getSkyUpdates().iterator());
            chunk.block=readLight(count,light.getBlockYMask(),light.getEmptyBlockYMask(),light.getBlockUpdates().iterator());
            IMAGES.put(chunk.getPos().toLong(),chunk);
            AlternativeRendererBridge.changed(level);
            mc.levelRenderer.onChunkLoaded(chunk.getPos());
            dirty(level,chunk.getPos());
            SeamMeshCache.changed(level,chunk.getPos());
        } finally { buf.release(); }
    }
    private static DataLayer[] readLight(int count,BitSet full,BitSet empty,Iterator<byte[]> values) {
        var layers=new DataLayer[count];
        for(int i=0;i<count;i++)if(full.get(i)||empty.get(i))
            layers[i]=full.get(i)?new DataLayer(values.next().clone()):new DataLayer();
        return layers;
    }
    private static void dirty(ClientLevel level,ChunkPos pos) {
        for(int y=level.getMinSection();y<level.getMaxSection();y++) level.setSectionDirtyWithNeighbors(pos.x,y,pos.z);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance(); if(mc.level!=world || mc.player==null) return;
        int range=Math.max(mc.options.getEffectiveRenderDistance()+2,PlanetConfig.RADIUS.getAsInt()+3);
        boolean removed=IMAGES.entrySet().removeIf(e -> {
            var p=e.getValue().getPos();
            if(Math.abs(p.x-mc.player.chunkPosition().x)<=range && Math.abs(p.z-mc.player.chunkPosition().z)<=PlanetConfig.RADIUS.getAsInt()+2) return false;
            dirty(mc.level,p); return true;
        });
        if(removed)AlternativeRendererBridge.changed(mc.level);
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if(event.getLevel()==world) { AlternativeRendererBridge.clear(world);IMAGES.clear(); world=null;renderLight=null; }
        if(renderLight!=null && renderLight.level==event.getLevel())renderLight=null;
        var fallback=empty;if(fallback!=null && fallback.getLevel()==event.getLevel())empty=null;
    }
    private static final class ImageChunk extends LevelChunk {
        private DataLayer[] sky,block;
        private int minLightSection;
        ImageChunk(ClientLevel level,ChunkPos pos) { super(level,pos); }
        DataLayer light(LightLayer layer,int sectionY) {
            var data=layer==LightLayer.SKY?sky:block;int i=sectionY-minLightSection;
            return data!=null && i>=0 && i<data.length?data[i]:null;
        }
        @Override public void setBlockEntity(BlockEntity entity) { entity.setLevel(getLevel()); getBlockEntities().put(entity.getBlockPos(),entity); }
        @Override public void setUnsaved(boolean unsaved) {}
    }
    private SeamImages() {}
}
