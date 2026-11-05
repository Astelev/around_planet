package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class EastWestClientSmoke {
    private static int ticks, phase, started;
    private static volatile Throwable serverFailure;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(Boolean.getBoolean("around_planet.visualSmoke") || Boolean.getBoolean("around_planet.entitySmoke")) return;
        var mc=Minecraft.getInstance(); ticks++;
        if(serverFailure!=null) throw new AssertionError("AROUND_EW_SMOKE server packet regression",serverFailure);
        if(ticks>2600) throw new AssertionError("AROUND_EW_SMOKE timeout: phase="+phase+" x="+(mc.player==null?"none":mc.player.getX()));
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null || ticks<100) return;
        mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        mc.player.setNoGravity(true);
        if(phase==3 && ticks%100==0) {
            AroundPlanet.LOGGER.info("VEHICLE regression client: playerX={}, vehicle={}",mc.player.getX(),
                    mc.player.getVehicle()==null?"none":mc.player.getVehicle().getId()+"@"+mc.player.getVehicle().getX());
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var v=p.getVehicle();
                AroundPlanet.LOGGER.info("VEHICLE regression server: vehicle={}, watch={}",v==null?"none":v.getId()+"@"+v.getX(),p.getChunkTrackingView());
                if(v!=null) mc.execute(()->AroundPlanet.LOGGER.info("VEHICLE client lookup: {}",mc.level.getEntity(v.getId())));
            });
        }
        if(phase==0) {
            require(mc.level.getWorldBorder().getMinX()==-264 && mc.level.getWorldBorder().getMaxX()==264,"Client border/config sync failed");
            require(mc.level.getWorldBorder().getMaxZ()>1000000,"Unexpected north/south limit");
            phase=1; started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                p.stopRiding();
                p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class,
                        new net.minecraft.world.phys.AABB(-300,190,25,300,210,40)).forEach(net.minecraft.world.entity.Entity::discard);
                p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                p.getAbilities().flying=true;p.onUpdateAbilities();
                for(int x:new int[]{-17,-16,15,16}) for(int z=-1;z<=1;z++) p.serverLevel().getChunk(x,z);
                p.connection.teleport(256.75,200,0,47,13);
            });
        } else if(phase==1 && ticks-started>40 && Math.abs(mc.player.getX()+255.25)<0.01) {
            require(Math.abs(mc.player.getY()-200)<0.01,"Player height changed: "+mc.player.getY());
            require(Math.abs(mc.player.getYRot()-47)<0.01 && Math.abs(mc.player.getXRot()-13)<0.01,"Player orientation changed");
            phase=2;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                p.teleportTo(-256.125,200,0);
            });
        } else if(phase==2 && ticks-started>40 && Math.abs(mc.player.getX()-255.875)<0.01) {
            phase=30;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                for(int x:new int[]{-17,-16,15,16}) for(int z=1;z<=3;z++) p.serverLevel().getChunk(x,z);
            });
        } else if(phase==30 && ticks-started>40) {
            phase=3;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                // Reused smoke worlds may contain previous parked fixtures; wait for entity NBT loading before clearing them.
                p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class,
                        new net.minecraft.world.phys.AABB(-300,-128,25,300,400,40)).forEach(net.minecraft.world.entity.Entity::discard);
                var boat=net.minecraft.world.entity.EntityType.BOAT.create(p.serverLevel());
                boat.moveTo(256.5,200,32);boat.setNoGravity(true);p.serverLevel().addFreshEntity(boat);
                p.startRiding(boat,true);
            });
        } else if(phase==3 && ticks-started>40 && mc.player.getVehicle()!=null && Math.abs(mc.player.getVehicle().getX()+255.5)<0.02) {
            AroundPlanet.LOGGER.info("AROUND_EW_SMOKE PASS: real player network transfer east/west, fractional offset, height/orientation, client border, boat + real player passenger, and no north/south wrap");
            phase=35;started=ticks;
            // Simulate vanilla dropping a passenger packet before a destination vehicle spawn.
            mc.player.stopRiding();
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var boat=p.getVehicle();
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,new io.github.astelev.around_planet.network.SeamVehiclePayload(
                        p.level().dimension().location(),boat.getId(),boat.getUUID(),boat.getX(),boat.getY(),boat.getZ(),boat.getYRot(),boat.getXRot(),boat.getId(),boat.getUUID()));
            });
        } else if(phase==35 && ticks-started>20 && mc.player.getVehicle()!=null) {
            AroundPlanet.LOGGER.info("AROUND_BOAT_MOUNT_SMOKE PASS: delayed authoritative correction repairs a dropped passenger pairing");
            phase=31;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                var old=p.getVehicle();p.stopRiding();if(old!=null)old.discard();
                var level=p.serverLevel();
                for(int x:new int[]{-256,-255,-254,-253,-252,-251,-250,-249,-248,-247,-246,-245,-244,244,245,246,247,248,249,250,251,252,253,254,255})
                    for(int z=29;z<=35;z++)for(int y=198;y<=202;y++)
                        level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,y,z),(y<200?net.minecraft.world.level.block.Blocks.WATER:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState());
                var boat=net.minecraft.world.entity.EntityType.BOAT.create(level);boat.moveTo(254,199.6,32,-90,0);level.addFreshEntity(boat);
                p.teleportTo(254,200,32);p.setYRot(-90);p.startRiding(boat,true);
            });
        } else if(phase==31 && ticks-started>60 && mc.player.getVehicle()!=null && mc.player.getVehicle().getX()>250) {
            phase=32;started=ticks;mc.options.keyUp.setDown(true);
        } else if(phase==32 && mc.player.getVehicle()!=null && mc.player.getVehicle().getX() < -250) {
            phase=33;started=ticks;
        } else if(phase==33 && ticks-started>20 && mc.player.getVehicle()!=null && mc.player.getVehicle().getX()>-250) {
            mc.player.getVehicle().setYRot(90);mc.player.setYRot(90);phase=36;started=ticks;
        } else if(phase==36 && mc.player.getVehicle()!=null && mc.player.getVehicle().getX()>250) {
            phase=37;started=ticks;
        } else if(phase==37 && ticks-started>20 && mc.player.getVehicle()!=null && mc.player.getVehicle().getX()<250) {
            mc.options.keyUp.setDown(false);mc.options.keyShift.setDown(true);phase=34;started=ticks;
        } else if(phase==34 && ticks-started>20 && !mc.player.isPassenger()) {
            mc.options.keyShift.setDown(false);
            AroundPlanet.LOGGER.info("AROUND_BOAT_INPUT_SMOKE PASS: actual paddling both ways, continued movement and Shift dismount without reconnect");
            phase=4;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                try {require(!p.isPassenger(),"Client dismounted but server still holds rider");}catch(Throwable e){serverFailure=e;}
                var boat=p.getVehicle();p.stopRiding();if(boat!=null) boat.discard();
                p.connection.teleport(255.75,200,0,47,13);
            });
        } else if(phase==4 && ticks-started>40 && Math.abs(mc.player.getX()-255.75)<0.01) {
            phase=5;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(256.125,200,0,false));
                    require(Math.abs(p.getX()+255.875)<0.01,"Movement packet did not wrap immediately at the seam: "+p.getX());
                } catch(Throwable failure) { serverFailure=failure; }
            });
        } else if(phase==5 && ticks-started>40 && Math.abs(mc.player.getX()+255.875)<0.01) {
            phase=6;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                p.connection.teleport(-255.75,200,0,47,13);
            });
        } else if(phase==6 && ticks-started>40 && Math.abs(mc.player.getX()+255.75)<0.01) {
            phase=7;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                try {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(-256.125,200,0,false));
                    require(Math.abs(p.getX()-255.875)<0.01,"West movement packet did not wrap immediately: "+p.getX());
                } catch(Throwable failure) { serverFailure=failure; }
            });
        } else if(phase==7 && ticks-started>40 && Math.abs(mc.player.getX()-255.875)<0.01) {
            require(io.github.astelev.around_planet.border.PlanetBorders.warningDistance(mc.level.getWorldBorder(),mc.player)>10000,
                    "Client vignette still treats traversable seam as danger");
            AroundPlanet.LOGGER.info("AROUND_EW_PACKET_SMOKE PASS: both native movement packets wrap in the same call; no seam vignette");
            phase=8;started=ticks;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                p.connection.teleport(0,200,0,47,13);
            });
        } else if(phase==8 && ticks-started>120) {
            // Let the destination ticket leases expire normally before ending this synthetic world run.
            phase=9;mc.stop();
        }
    }
    private static void require(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
