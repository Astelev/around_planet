package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.network.SeamChunkPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.DataLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.Arrays;

/** Real server light corruption/recovery plus client snapshot isolation, at ten chunks inside the seam. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamLightSmoke {
    private static int ticks,phase,started;
    private static volatile int serverSky=-1,sourceSky,sourceBlock;
    private static volatile byte[] payload;
    private static volatile Throwable failure;
    private static final BlockPos PROBE=new BlockPos(-88,100,56),SOURCE=new BlockPos(248,102,56);
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.lightSmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("SEAM_LIGHT_SMOKE server failure",failure);
        if(ticks>2200)throw new AssertionError("SEAM_LIGHT_SMOKE timeout phase="+phase+" serverSky="+serverSky);
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null || ticks<100)return;
        mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);mc.player.setNoGravity(true);
        var server=mc.getSingleplayerServer();
        if(phase==0) {
            phase=1;started=ticks;mc.options.renderDistance().set(12);mc.options.setServerRenderDistance(12);
            server.execute(()->guard(()->{
                var p=server.getPlayerList().getPlayers().getFirst();p.stopRiding();
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
                var level=p.serverLevel();server.getPlayerList().setViewDistance(12);level.setDayTime(6000);
                for(int x=-7;x<=-5;x++)for(int z=2;z<=4;z++)level.getChunk(x,z);
                level.getChunk(15,3);
                for(int x=-96;x< -80;x++)for(int z=48;z<64;z++)level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());
                for(int x=244;x<=252;x++)for(int z=52;z<=60;z++)level.setBlockAndUpdate(new BlockPos(x,106,z),Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(SOURCE.below(),Blocks.GLOWSTONE.defaultBlockState());
                p.connection.teleport(-88.5,102,56,0,0);
            }));
        } else if(phase==1 && ticks-started>80) {
            phase=2;started=ticks;
            server.execute(()->guard(()->{
                var level=server.overworld();require(level.getBrightness(LightLayer.SKY,PROBE)==15,"Healthy open-air server light was not 15");
                var engine=level.getChunkSource().getLightEngine();
                for(int x=-7;x<=-5;x++)for(int z=2;z<=4;z++)for(int y=engine.getMinLightSection();y<engine.getMaxLightSection();y++)
                    engine.queueSectionData(LightLayer.SKY,SectionPos.of(x,y,z),new DataLayer());
                engine.lightChunk(level.getChunk(-6,3),true).whenCompleteAsync((c,error)->guard(()->{
                    if(error!=null)throw new AssertionError(error);
                    serverSky=level.getBrightness(LightLayer.SKY,PROBE);
                    require(serverSky==0,"Synthetic real server darkness was not reproduced: "+serverSky);
                    require(c.isLightCorrect(),"Regression requires apparently valid stored lighting, not pending generation");
                    AroundPlanet.LOGGER.info("SEAM_LIGHT_DARK_SERVER PASS: open-air sky=0 with lightCorrect=true");
                    var source=server.getPlayerList().getPlayers().getFirst().createCommandSourceStack().withPermission(4);
                    server.getCommands().getDispatcher().execute("around_planet relight -6 3",source);
                }),server);
            }));
        } else if(phase==2 && ticks-started>60) {
            if(ticks%20==0)server.execute(()->guard(()->serverSky=server.overworld().getBrightness(LightLayer.SKY,PROBE)));
            if(serverSky!=15 || mc.level.getBrightness(LightLayer.SKY,PROBE)!=15)return;
            phase=3;started=ticks;
            server.execute(()->guard(()->{
                var level=server.overworld();require(level.getBlockState(PROBE.below()).is(Blocks.STONE),"Relight changed terrain");
                var chunk=level.getChunk(15,3);require(chunk.isLightCorrect(),"Source lighting not ready");
                sourceSky=level.getBrightness(LightLayer.SKY,SOURCE);sourceBlock=level.getBrightness(LightLayer.BLOCK,SOURCE);
                var packet=new ClientboundLevelChunkWithLightPacket(chunk,level.getChunkSource().getLightEngine(),null,null);
                var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
                try {ClientboundLevelChunkWithLightPacket.STREAM_CODEC.encode(buf,packet);payload=new byte[buf.readableBytes()];buf.readBytes(payload);}
                finally {buf.release();}
            }));
            AroundPlanet.LOGGER.info("SEAM_LIGHT_REPAIR PASS: native server/client sky=15 restored, blocks retained");
        } else if(phase==3 && payload!=null) {
            var engine=mc.level.getLightEngine();while(engine.hasLightWork())engine.runLightUpdates();
            long before=hash(mc,-6,3)^hash(mc,-17,3);
            SeamImages.accept(new SeamChunkPayload(mc.level.dimension().location(),-17,3,payload));
            require(!engine.hasLightWork(),"Visual snapshot queued work in the gameplay light engine");
            require(before==(hash(mc,-6,3)^hash(mc,-17,3)),"Visual snapshot mutated gameplay lighting");
            var virtual=new BlockPos(-264,102,56);var render=SeamImages.renderLight(mc.level);
            require(render.getLayerListener(LightLayer.SKY).getLightValue(virtual)==sourceSky,"Render sky differs from source");
            require(sourceBlock>0 && render.getLayerListener(LightLayer.BLOCK).getLightValue(virtual)==sourceBlock,"Render block light differs from source");
            var section=SectionPos.of(virtual);
            var region=new net.minecraft.client.renderer.chunk.RenderRegionCache().createRegion(mc.level,section,false);
            require(region!=null && region.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(virtual)==sourceBlock,
                    "Vanilla compiler did not read isolated block light");
            if(net.neoforged.fml.ModList.get().isLoaded("sodium")) {
                try {
                    var clone=Class.forName("net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSection");
                    var read=clone.getDeclaredMethod("copyLightArray",net.minecraft.world.level.Level.class,LightLayer.class,SectionPos.class);read.setAccessible(true);
                    var data=(DataLayer)read.invoke(null,mc.level,LightLayer.BLOCK,section);
                    require(data.get(virtual.getX()&15,virtual.getY()&15,virtual.getZ()&15)==sourceBlock,"Sodium clone did not read isolated block light");
                } catch(ReflectiveOperationException error){throw new AssertionError(error);}
            }
            var pos=mc.player.position();mc.player.setPos(0,102,56);SeamImages.tick(new ClientTickEvent.Post());mc.player.setPos(pos);
            require(SeamImages.get(mc.level,-17,3)==null,"Image did not expire in isolation control");
            require(!engine.hasLightWork() && before==(hash(mc,-6,3)^hash(mc,-17,3)),"Image eviction cleared gameplay light");
            AroundPlanet.LOGGER.info("SEAM_LIGHT_ISOLATION PASS: sky/block source values retained; snapshot accept/eviction leave native lighting untouched");
            phase=4;started=ticks;server.execute(()->server.getPlayerList().getPlayers().getFirst().connection.teleport(0,200,0,0,0));
        } else if(phase==4 && ticks-started>120){phase=5;mc.stop();}
    }
    private static long hash(Minecraft mc,int x,int z) {
        long result=1;var engine=mc.level.getLightEngine();
        for(var layer:LightLayer.values())for(int y=engine.getMinLightSection();y<engine.getMaxLightSection();y++) {
            var data=engine.getLayerListener(layer).getDataLayerData(SectionPos.of(x,y,z));
            result=31*result+(data==null?0:Arrays.hashCode(data.getData()));
        }
        return result;
    }
    private static void guard(Checked work){try{work.run();}catch(Throwable e){failure=e;}}
    private interface Checked {void run() throws Exception;}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
