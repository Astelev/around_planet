package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.client.AlternativeRendererBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** A late image replaces renderer-only empty padding while the camera is above Sodium's occlusion graph. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamArrivalSmoke {
    private static int ticks,phase,started,arrived,sectionY;
    private static volatile Throwable failure;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.arrivalSmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError(failure);
        if(ticks>1600)throw new AssertionError("SEAM_ARRIVAL timeout phase="+phase);
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null || ticks<100)return;
        mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);mc.player.setNoGravity(true);
        var server=mc.getSingleplayerServer();
        if(phase==0) {
            phase=10;started=ticks;
            server.execute(()->server.getPlayerList().getPlayers().getFirst().connection.teleport(0,400,0,0,0));
        } else if(phase==10 && ticks-started>140 && SeamImages.size()==0) {
            io.github.astelev.around_planet.client.SeamMeshCache.clear();
            phase=1;started=ticks;sectionY=(mc.level.getMaxBuildHeight()-32)>>4;
            server.execute(()->{
                try {
                    var p=server.getPlayerList().getPlayers().getFirst();p.stopRiding();
                    p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);
                    server.getPlayerList().setViewDistance(12);
                    var level=p.serverLevel();level.getChunk(-16,7);level.getChunk(-16,8);
                    for(int x=-246;x<=-244;x++)for(int y=level.getMaxBuildHeight()-32;y<=level.getMaxBuildHeight()-28;y++)for(int z=117;z<=123;z++)
                        level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.RED_CONCRETE.defaultBlockState());
                    for(int x=-246;x<=-244;x++)for(int y=level.getMaxBuildHeight()-32;y<=level.getMaxBuildHeight()-28;y++)for(int z=133;z<=139;z++)
                        level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.LIME_CONCRETE.defaultBlockState());
                    level.getChunk(-16,8).setLightCorrect(false);
                    p.connection.teleport(210.5,level.getMaxBuildHeight()+16,120.5,-74,39);
                }catch(Throwable error){failure=error;}
            });
        } else if(phase==1 && ticks-started>40) {
            if(ticks%100==0)AroundPlanet.LOGGER.info("ARRIVAL wait firstData={} firstGeo={} padded={} lateData={}",
                    SeamImages.get(mc.level,16,7)!=null,AlternativeRendererBridge.hasGeometry(16,sectionY,7),
                    AlternativeRendererBridge.isReady(16,sectionY,8),SeamImages.get(mc.level,16,8)!=null);
            if(SeamImages.get(mc.level,16,7)==null || !Boolean.TRUE.equals(AlternativeRendererBridge.hasGeometry(16,sectionY,7)))return;
            if(!Boolean.TRUE.equals(AlternativeRendererBridge.isReady(16,sectionY,8)))return;
            require(SeamImages.get(mc.level,16,8)==null,"Delayed image arrived before the padding control");
            require(!Boolean.TRUE.equals(AlternativeRendererBridge.hasGeometry(16,sectionY,8)),"Padding already had geometry");
            phase=2;started=ticks;
            server.execute(()->server.overworld().getChunk(-16,8).setLightCorrect(true));
        } else if(phase==2) {
            if(SeamImages.get(mc.level,16,8)==null)return;
            if(arrived==0)arrived=ticks;
            require(ticks-arrived<80,"Sodium kept the late image as empty padding for 4 seconds after data arrival");
            if(!Boolean.TRUE.equals(AlternativeRendererBridge.hasGeometry(16,sectionY,8)))return;
            AroundPlanet.LOGGER.info("SEAM_LATE_IMAGE PASS: native Sodium geometry ready {} ticks after data arrived, with camera above the graph",ticks-arrived);
            phase=3;started=ticks;server.execute(()->server.getPlayerList().getPlayers().getFirst().connection.teleport(0,200,0,0,0));
        } else if(phase==3 && ticks-started>120){phase=4;mc.stop();}
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
