package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.SeamImages;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Real client prediction and input; only enabled in an opt-in development run. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamCollisionSmoke {
    private static int ticks,phase,started;
    private static volatile Throwable failure;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.collisionSmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("SEAM_COLLISION server failure",failure);
        if(ticks>1800)throw new AssertionError("SEAM_COLLISION timeout phase="+phase+" x="+(mc.player==null?null:mc.player.getX()));
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null || ticks<100)return;
        if(phase==0) {
            phase=1;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.serverLevel();
                p.stopRiding();p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);p.getAbilities().flying=false;p.onUpdateAbilities();
                for(int x:new int[]{-17,-16,15,16})for(int z=3;z<=4;z++)level.getChunk(x,z);
                for(int x:new int[]{-258,-257,-256,-255,-254,-253,-252,252,253,254,255,256,257,258})
                    for(int z=63;z<=69;z++)for(int y=219;y<=223;y++)
                        level.setBlockAndUpdate(new BlockPos(x,y,z),(y==219?Blocks.STONE:Blocks.AIR).defaultBlockState());
                for(int y=220;y<=221;y++)level.setBlockAndUpdate(new BlockPos(-256,y,64),Blocks.STONE.defaultBlockState());
                p.connection.teleport(254,220,64.5,-90,0);
            });
        } else if(phase==1 && ticks-started>60 && mc.player.getX()>253 && image(mc,256,220,64)==Blocks.STONE) {
            phase=2;started=ticks;mc.options.keyUp.setDown(true);
        } else if(phase==2 && ticks-started>50) {
            require(mc.player.getX()>255.6 && mc.player.getX()<255.71,"Predicted seam wall did not stop the player: "+mc.player.getX());
            mc.options.keyUp.setDown(false);phase=3;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.serverLevel();
                    require(p.getX()>255.6 && p.getX()<255.71,"Server disagrees with predicted wall contact: "+p.getX());
                    for(int y=220;y<=221;y++) {
                        level.setBlockAndUpdate(new BlockPos(-256,y,64),Blocks.AIR.defaultBlockState());
                        level.setBlockAndUpdate(new BlockPos(256,y,64),Blocks.STONE.defaultBlockState());
                        level.setBlockAndUpdate(new BlockPos(-257,y,64),Blocks.STONE.defaultBlockState());
                    }
                    for(int x:new int[]{254,255,-256,-255})level.setBlockAndUpdate(new BlockPos(x,222,64),Blocks.STONE.defaultBlockState());
                }catch(Throwable e){failure=e;}
            });
        } else if(phase==3 && ticks-started>30 && image(mc,256,220,64)==Blocks.AIR) {
            phase=4;started=ticks;mc.options.keyUp.setDown(true);
        } else if(phase==4 && mc.player.getX()<0) {
            require(Math.abs(mc.player.getY()-220)<0.05,"Tunnel crossing lost floor support");
            mc.options.keyUp.setDown(false);mc.player.setYRot(90);phase=5;started=ticks;
        } else if(phase==5 && ticks-started>30 && image(mc,-257,220,64)==Blocks.AIR) {
            mc.options.keyUp.setDown(true);phase=6;started=ticks;
        } else if(phase==6 && mc.player.getX()>0) {
            mc.options.keyUp.setDown(false);phase=7;started=ticks;
            AroundPlanet.LOGGER.info("SEAM_COLLISION_INPUT PASS: predicted wall agrees with server; real walking crosses a two-block tunnel both ways despite contradictory raw guard terrain");
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.serverLevel();
                level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class,new net.minecraft.world.phys.AABB(-300,210,66,300,230,72)).forEach(net.minecraft.world.entity.Entity::discard);
                level.setBlockAndUpdate(new BlockPos(-256,220,68),Blocks.STONE.defaultBlockState());
                var boat=net.minecraft.world.entity.EntityType.BOAT.create(level);boat.setPos(256.1,220,68.8);boat.setNoGravity(true);level.addFreshEntity(boat);
                p.connection.teleport(256.1,220,68.8,0,0);p.startRiding(boat,true);
            });
        } else if(phase==7 && ticks-started>30 && mc.player.isPassenger()) {
            mc.options.keyShift.setDown(true);phase=8;started=ticks;
        } else if(phase==8 && ticks%100==0 && mc.player.isPassenger()) {
            AroundPlanet.LOGGER.info("SEAM_COLLISION_WAIT client x={} vehicle={}",mc.player.getX(),mc.player.getVehicle().getX());
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                AroundPlanet.LOGGER.info("SEAM_COLLISION_WAIT server x={} vehicle={} gate={}",p.getX(),p.getVehicle()==null?null:p.getVehicle().getX(),io.github.astelev.around_planet.transfer.TransferService.diagnostics(p));
            });
        } else if(phase==8 && ticks-started>20 && !mc.player.isPassenger()) {
            mc.options.keyShift.setDown(false);phase=9;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                try {require(!mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().isPassenger(),"Server retained passenger after Shift");}
                catch(Throwable e){failure=e;}
            });
            AroundPlanet.LOGGER.info("SEAM_COLLISION_DISMOUNT PASS: Shift exits a collision-blocked seam boat without reconnect");
        } else if(phase==9 && ticks-started>40)mc.stop();
    }
    private static net.minecraft.world.level.block.Block image(Minecraft mc,int x,int y,int z) {
        var chunk=SeamImages.get(mc.level,x>>4,z>>4);return chunk==null?null:chunk.getBlockState(new BlockPos(x,y,z)).getBlock();
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
