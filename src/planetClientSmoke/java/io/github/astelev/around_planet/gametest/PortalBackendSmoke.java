package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.portal.PlanetPortals;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;

/** Real native portal rendering and movement, not a direct call to our former teleporter. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class PortalBackendSmoke {
    private static int ticks,phase,started;
    private static volatile Throwable failure;
    private static volatile int mobId;
    private static volatile net.minecraft.world.entity.Mob fixtureMob;
    private static ChunkAccess destinationChunk;
    private static Object clientLevel,renderer;
    private static Object dhOpposite,dhPrimary;
    private static long dhSwaps;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("PORTAL_BACKEND_SMOKE fixture",failure);
        if(ticks>2600)throw new AssertionError("PORTAL_BACKEND_SMOKE timeout phase="+phase+" x="+(mc.player==null?null:mc.player.getX()));
        if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null || ticks<100)return;
        mc.player.setNoGravity(true);
        if(phase==0) {
            phase=1;started=ticks;mc.options.renderDistance().set(8);mc.options.setServerRenderDistance(8);
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.serverLevel();
                    p.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
                    require(PlanetPortals.managed(level).size()==2,"Native EW pair not created");
                    p.stopRiding();p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);
                    level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                    for(int x:new int[]{-16,-15,14,15})for(int z=-2;z<=2;z++)level.getChunk(x,z);
                    SableCollisionRegression.verify(level,PlanetPortals.managed(level).stream().filter(p0->PlanetPortals.EAST.equals(p0.portalTag)).findFirst().orElseThrow());
                    for(int x=-246;x<=-244;x++)for(int y=80;y<=84;y++)for(int z=5;z<=11;z++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.RED_CONCRETE.defaultBlockState());
                    var mob=net.minecraft.world.entity.EntityType.SLIME.create(level);mob.setSize(4,true);mob.setNoAi(true);mob.setNoGravity(true);
                    mob.moveTo(-248.5,80,12.5);level.addFreshEntity(mob);mobId=mob.getId();fixtureMob=mob;
                    p.connection.teleport(210.5,82.5,8.5,-90,0);
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==1 && ticks-started>100) {
            long portals=GlobalPortalStorage.getGlobalPortals(mc.level).stream().filter(PlanetPortals::owned).count();
            if(ticks%100==0) {
                AroundPlanet.LOGGER.info("PORTAL_BACKEND_SMOKE waiting: portals={} sourceBlock={} nativeClientMob={}",portals,mc.level.getBlockState(new BlockPos(-245,82,8)),mc.level.getEntity(mobId));
                mc.getSingleplayerServer().execute(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var e=p.serverLevel().getEntity(mobId);
                    AroundPlanet.LOGGER.info("PORTAL_BACKEND_SMOKE server mob={} fixture={} reason={} player={} periodicDistance={} sourceWatch={}",e,fixtureMob,fixtureMob.getRemovalReason(),p.position(),io.github.astelev.around_planet.portal.PortalMobDistance.forDespawn(p,fixtureMob,p.distanceToSqr(fixtureMob)),qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking.isPlayerWatchingChunk(p,p.level().dimension(),-16,0));
                });
            }
            if(portals!=2 || !mc.level.getBlockState(new BlockPos(-245,82,8)).is(Blocks.RED_CONCRETE) || mc.level.getEntity(mobId)==null || !red(mc))return;
            if(net.neoforged.fml.ModList.get().isLoaded("distanthorizons")) {
                var dh=io.github.astelev.around_planet.compat.DhPortalViews.snapshot();
                if(ticks%100==0)AroundPlanet.LOGGER.info("DH_PORTAL_SMOKE waiting: {}",dh);
                if(dh==null || dh.portalPasses()==0 || dh.oppositeBuffers()==0)return;
                dhOpposite=dh.opposite();dhPrimary=dh.primary();dhSwaps=dh.swaps();
                DhClipRegression.verify();
                AroundPlanet.LOGGER.info("DH_PORTAL_SMOKE prepared: passes={} oppositeBuffers={}",dh.portalPasses(),dh.oppositeBuffers());
            }
            clientLevel=mc.level;renderer=mc.levelRenderer;
            AroundPlanet.LOGGER.info("PORTAL_BACKEND_SMOKE GPU PASS: native opposite coast, canonical chunk cache and real non-persistent remote mob");
            phase=2;started=ticks;
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().connection.teleport(254,640,8.5,-90,0));
        } else if(phase==2 && ticks-started>60) {
            destinationChunk=mc.level.getChunkSource().getChunk(-16,0,ChunkStatus.FULL,false);if(destinationChunk==null)return;
            phase=3;started=ticks;mc.options.keyUp.setDown(true);
        } else if(phase==3 && mc.player.getX()<-250) {
            mc.options.keyUp.setDown(false);
            require(Math.abs(mc.player.getY()-640)<0.1,"Altitude changed");
            require(mc.level==clientLevel && mc.levelRenderer==renderer,"Same-dimension context was recreated");
            require(mc.level.getChunkSource().getChunk(-16,0,ChunkStatus.FULL,false)==destinationChunk,"Preloaded destination chunk was discarded");
            AroundPlanet.LOGGER.info("PORTAL_BACKEND_SMOKE east PASS: actual client crossing at Y=640, same level/renderer/destination chunk retained");
            phase=4;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.connection.teleport(-254,640,8.5,90,0);
            });
        } else if(phase==4 && ticks-started>60) {
            if(dhOpposite!=null) {
                var dh=io.github.astelev.around_planet.compat.DhPortalViews.snapshot();
                require(dh!=null && dh.primary()==dhOpposite && dh.swaps()>dhSwaps,"Prepared DH tree was discarded on east crossing");
                AroundPlanet.LOGGER.info("DH_PORTAL_SMOKE east PASS: preloaded opposite tree promoted without rebuild");
            }
            mc.options.keyUp.setDown(true);phase=5;started=ticks;
        } else if(phase==5 && mc.player.getX()>250) {
            mc.options.keyUp.setDown(false);
            require(mc.level==clientLevel && mc.levelRenderer==renderer,"West crossing recreated context");
            phase=6;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    require(p.getX()>250 && p.getX()<256,"Server did not accept west crossing");
                    p.stopRiding();var level=p.serverLevel();
                    level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class,
                            new net.minecraft.world.phys.AABB(-270,190,25,270,210,40)).forEach(net.minecraft.world.entity.Entity::discard);
                    for(int x:new int[]{-256,-255,-254,-253,-252,-251,-250,-249,-248,-247,-246,-245,-244,244,245,246,247,248,249,250,251,252,253,254,255})
                        for(int z=29;z<=35;z++)for(int y=198;y<=202;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),(y<200?Blocks.WATER:Blocks.AIR).defaultBlockState());
                    var boat=net.minecraft.world.entity.EntityType.BOAT.create(level);boat.moveTo(254,199.6,32,-90,0);level.addFreshEntity(boat);
                    p.connection.teleport(254,200,32,-90,0);p.startRiding(boat,true);
                }catch(Throwable e){failure=e;}
            });
            AroundPlanet.LOGGER.info("PORTAL_BACKEND_SMOKE PASS: both native crossings, high altitude, GPU image and retained canonical cache");
        } else if(phase==6 && ticks-started>60 && mc.player.getVehicle()!=null && mc.player.getVehicle().getX()>250) {
            if(dhPrimary!=null) {
                var dh=io.github.astelev.around_planet.compat.DhPortalViews.snapshot();
                require(dh!=null && dh.primary()==dhPrimary && dh.swaps()>=dhSwaps+2,"Original DH tree was discarded on reverse crossing");
                AroundPlanet.LOGGER.info("DH_PORTAL_SMOKE west PASS: original tree retained for reverse crossing");
            }
            phase=7;started=ticks;mc.options.keyUp.setDown(true);
        } else if(phase==7 && mc.player.getVehicle()!=null && mc.player.getVehicle().getX()<-250) {
            phase=8;started=ticks;
        } else if(phase==8 && ticks-started>20 && mc.player.getVehicle()!=null && mc.player.getVehicle().getX()>-250) {
            mc.options.keyUp.setDown(false);mc.options.keyShift.setDown(true);phase=9;started=ticks;
        } else if(phase==9 && ticks-started>20 && !mc.player.isPassenger()) {
            mc.options.keyShift.setDown(false);phase=10;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    require(!p.isPassenger(),"Server still holds boat passenger");p.connection.teleport(254,640,8.5,-90,0);
                }catch(Throwable e){failure=e;}
            });
            AroundPlanet.LOGGER.info("PORTAL_BACKEND_BOAT_SMOKE PASS: native paddling through global portal, continued movement and Shift dismount");
        } else if(phase==10 && ticks-started>140){phase=11;mc.stop();}
    }
    private static boolean red(Minecraft mc) {
        mc.getMainRenderTarget().bindWrite(false);var pixel=BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(mc.getWindow().getWidth()/2+10,mc.getWindow().getHeight()/2-10,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
        int r=pixel.get(0)&255,g=pixel.get(1)&255,b=pixel.get(2)&255;
        if(ticks%100==0)AroundPlanet.LOGGER.info("PORTAL_BACKEND_SMOKE pixel {},{},{}",r,g,b);
        return r>40 && r>g*1.4 && r>b*1.4;
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
