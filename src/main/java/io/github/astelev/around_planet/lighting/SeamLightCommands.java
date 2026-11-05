package io.github.astelev.around_planet.lighting;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Explicit, bounded recovery of loaded chunks. Never deletes terrain or waits on lighting futures. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID)
public final class SeamLightCommands {
    private static final TicketType<ChunkPos> REPAIR=TicketType.create("around_planet_relight",Comparator.comparingLong(ChunkPos::toLong),200);
    private static final Set<net.minecraft.server.level.ServerLevel> ACTIVE=Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var root=Commands.literal("around_planet").requires(s->s.hasPermission(2));
        for(String action:List.of("lightcheck","relight"))root.then(Commands.literal(action)
                .then(Commands.argument("chunkX",IntegerArgumentType.integer(-1875000,1874999))
                .then(Commands.argument("chunkZ",IntegerArgumentType.integer(-1875000,1874999)).executes(c->run(c.getSource(),
                        IntegerArgumentType.getInteger(c,"chunkX"),IntegerArgumentType.getInteger(c,"chunkZ"),action.equals("relight"))))));
        event.getDispatcher().register(root);
    }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)ACTIVE.remove(level);
    }
    private static int run(CommandSourceStack source,int x,int z,boolean repair) {
        var level=source.getLevel();var chunk=level.getChunkSource().getChunkNow(x,z);
        if(chunk==null){source.sendFailure(Component.literal("Chunk is not loaded; approach it first."));return 0;}
        var pos=new BlockPos(x*16+8,Math.min(level.getMaxBuildHeight()-1,chunk.getHeight(Heightmap.Types.WORLD_SURFACE,8,8)+1),z*16+8);
        int sky=level.getBrightness(LightLayer.SKY,pos);
        var at=BlockPos.containing(source.getPosition());int localSky=level.getBrightness(LightLayer.SKY,at);
        source.sendSuccess(()->Component.literal("Server chunk "+x+", "+z+": sky="+sky+" at "+pos.toShortString()+", lightCorrect="+chunk.isLightCorrect()+"; source sky="+localSky+" at "+at.toShortString()),false);
        if(!repair)return sky;
        if(!ACTIVE.add(level)){source.sendFailure(Component.literal("A relight is already running in this dimension."));return 0;}
        var engine=level.getChunkSource().getLightEngine();
        var chunks=new ArrayList<net.minecraft.world.level.chunk.LevelChunk>();
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            var loaded=level.getChunkSource().getChunkNow(x+dx,z+dz);if(loaded!=null)chunks.add(loaded);
        }
        var futures=new ArrayList<CompletableFuture<?>>();
        for(var c:chunks) {
            var cp=c.getPos();level.getChunkSource().addRegionTicket(REPAIR,cp,1,cp);
            engine.retainData(cp,false);engine.setLightEnabled(cp,false);
            for(int y=engine.getMinLightSection();y<engine.getMaxLightSection();y++) {
                engine.queueSectionData(LightLayer.SKY,SectionPos.of(cp,y),null);
                engine.queueSectionData(LightLayer.BLOCK,SectionPos.of(cp,y),null);
            }
            for(int y=level.getMinSection();y<level.getMaxSection();y++)engine.updateSectionStatus(SectionPos.of(cp,y),true);
            // First finish removing old storage; initialization must happen in a later light batch.
            futures.add(engine.lightChunk(c,true).thenApply(ignored->{c.setLightCorrect(false);return c;})
                    .thenComposeAsync(ignored->engine.initializeLight(c,false),level.getServer())
                    .thenComposeAsync(ignored->engine.lightChunk(c,false),level.getServer()));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenCompleteAsync((ignored,error)->{
            ACTIVE.remove(level);
            for(var c:chunks) {
                c.setUnsaved(true);
                level.getChunkSource().removeRegionTicket(REPAIR,c.getPos(),1,c.getPos());
                if(error==null) {
                    var packet=new net.minecraft.network.protocol.game.ClientboundLightUpdatePacket(c.getPos(),engine,null,null);
                    for(var player:level.players())if(player.getChunkTrackingView().contains(c.getPos().x,c.getPos().z))player.connection.send(packet);
                }
            }
            if(error!=null){AroundPlanet.LOGGER.error("Chunk relight failed at {},{}",x,z,error);source.sendFailure(Component.literal("Relight failed; see server log."));}
            else source.sendSuccess(()->Component.literal("Relit "+chunks.size()+" loaded chunks; server sky="+level.getBrightness(LightLayer.SKY,pos)),false);
        },level.getServer());
        source.sendSuccess(()->Component.literal("Relighting "+chunks.size()+" loaded chunks asynchronously."),false);return chunks.size();
    }
    private SeamLightCommands() {}
}
