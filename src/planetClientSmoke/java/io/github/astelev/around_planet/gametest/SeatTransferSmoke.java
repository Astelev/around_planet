package io.github.astelev.around_planet.gametest;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.compat.ShipTransferService;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3d;

/** Exercises Sable's native mounted-player packet hooks with a real Create seat and client. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeatTransferSmoke {
    private static int ticks,phase,started;
    private static volatile Throwable failure;
    private static volatile ServerSubLevel ship;
    private static volatile Entity seat;
    private static Vec3 plotPosition;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.seatTransferSmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("SEAT_TRANSFER server failed",failure);
        if(ticks>2000)throw new AssertionError("SEAT_TRANSFER timeout phase="+phase);
        if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null || ticks<100)return;
        if(ticks%100==0)AroundPlanet.LOGGER.info("SEAT_TRANSFER phase={} player={} vehicle={}",phase,mc.player.position(),mc.player.getVehicle()==null?null:mc.player.getVehicle().position());
        if(phase==0) {
            phase=1;started=ticks;server(mc,()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=p.serverLevel();var container=SubLevelContainer.getContainer(level);
                p.stopRiding();p.setNoGravity(true);
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                for(int x:new int[]{-17,-16,15,16})for(int z=8;z<=10;z++)level.getChunk(x,z);
                var anchor=new BlockPos(20,280,150);level.setBlockAndUpdate(anchor,Blocks.IRON_BLOCK.defaultBlockState());
                ship=SubLevelAssemblyHelper.assembleBlocks(level,anchor,java.util.List.of(anchor),new BoundingBox3i(20,280,150,20,280,150));
                container.addForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);
                container.physicsSystem().setPaused(true);
                container.physicsSystem().getPipeline().teleport(ship,new Vector3d(255,280,150),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();
                var b=ship.getPlot().getBoundingBox();
                seat=(Entity)Class.forName("com.simibubi.create.content.contraptions.actors.seat.SeatEntity").getConstructor(Level.class).newInstance(level);
                // SeatEntity.tick requires a seat block, so create the real block under it.
                var seatBlock=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:white_seat"));
                level.setBlockAndUpdate(new BlockPos(b.minX(),b.minY()+1,b.minZ()),seatBlock.defaultBlockState());
                seat.setPos(b.minX()+0.5,b.minY()+1,b.minZ()+0.5);plotPosition=seat.position();level.addFreshEntity(seat);
                p.teleportTo(255,282,150);p.startRiding(seat,true);seat.positionRider(p);
            });
        } else if(phase==1 && ticks-started>60 && mc.player.isPassenger()) {
            require(Math.abs(mc.player.getX())<300,"Initial mount leaked plot coordinates");
            phase=2;started=ticks;cross(mc,257);
        } else if(phase==2 && ticks-started>60 && mc.player.getX()<0) {
            check(mc);AroundPlanet.LOGGER.info("SEAT_TRANSFER_EAST PASS: player stayed mounted; plot seat unchanged; client is in destination world coordinates");
            phase=3;started=ticks;cross(mc,-257);
        } else if(phase==3 && ticks-started>60 && mc.player.getX()>0) {
            check(mc);AroundPlanet.LOGGER.info("SEAT_TRANSFER_WEST PASS: reverse mounted transfer keeps server and client in world space");
            phase=4;started=ticks;
            server(mc,()->{var p=ship.getLevel().players().getFirst();p.stopRiding();require(Sable.HELPER.getContaining(p)==null && Math.abs(p.getX())<300,"Dismount put player into storage plot");});
        } else if(phase==4 && ticks-started>40 && !mc.player.isPassenger()) {
            require(Math.abs(mc.player.getX())<300,"Client dismount leaked plot coordinates");
            AroundPlanet.LOGGER.info("SEAT_TRANSFER_DISMOUNT PASS: client and server can dismount safely after both crossings");
            phase=5;started=ticks;server(mc,()->{
                var p=ship.getLevel().players().getFirst();p.teleportTo(0,285,150);p.getAbilities().flying=true;p.onUpdateAbilities();
                seat.discard();var container=SubLevelContainer.getContainer(ship.getLevel());container.removeForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);container.physicsSystem().setPaused(false);
            });
        } else if(phase==5 && ticks-started>30)mc.stop();
    }
    private static void cross(Minecraft mc,double x) {server(mc,()->{
        var c=SubLevelContainer.getContainer(ship.getLevel());var v=new Vector3d(ship.logicalPose().position());v.x=x;
        c.physicsSystem().getPipeline().teleport(ship,v,ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();
        seat.positionRider(ship.getLevel().players().getFirst());ShipTransferService.check(ship.getLevel(),c);
        require(seat.position().equals(plotPosition),"Server translated native seat");
        var p=ship.getLevel().players().getFirst();require(p.getVehicle()==seat && Sable.HELPER.getContaining(p)==null && Math.abs(p.getX())<300,"Server mounted player is outside world coordinates");
    });}
    private static void check(Minecraft mc) {
        require(mc.player.isPassenger(),"Client lost seat mount");
        require(mc.player.getVehicle().position().distanceTo(plotPosition)<1e-6,"Client seat moved out of plot");
        require(Sable.HELPER.getContaining(mc.player)==null,"Client player is in storage coordinates");
    }
    @FunctionalInterface private interface Action {void run() throws Exception;}
    private static void server(Minecraft mc,Action action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable e){failure=e;}});}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
