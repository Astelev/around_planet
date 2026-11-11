package io.github.astelev.around_planet.gametest;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.compat.SpaceShipTransfers;
import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3d;
import java.util.*;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SpaceEntrySmoke {
    private static int ticks,phase,started;private static volatile Throwable failure;
    private static volatile ServerSubLevel ship;private static volatile Entity seat;private static volatile UUID id;private static Object mesh,clientIdentity,inventory;
    private static volatile boolean live;
    @SubscribeEvent public static void drive(net.neoforged.neoforge.event.tick.LevelTickEvent.Pre event) {
        if(!Boolean.getBoolean("around_planet.spaceEntrySmoke") || !live || ship==null || event.getLevel()!=ship.getLevel())return;
        try {
            var pipeline=SubLevelContainer.getContainer(ship.getLevel()).physicsSystem().getPipeline();
            var v=pipeline.getLinearVelocity(ship,new Vector3d());var w=pipeline.getAngularVelocity(ship,new Vector3d());
            pipeline.addLinearAndAngularVelocity(ship,v.negate(),w.negate());
        }catch(Throwable e){failure=e;}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.spaceEntrySmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("SPACE_SMOKE server",failure);
        if(ticks>2200)throw new AssertionError("SPACE_SMOKE timeout phase="+phase);
        if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null || ticks<100)return;
        if(ticks%100==0)AroundPlanet.LOGGER.info("SPACE_SMOKE phase={} player={} cell={} shipY={}",phase,mc.player.position(),SpaceCells.contains(mc.player),client(mc)==null?null:client(mc).logicalPose().position().y);
        if(phase==0) {
            phase=1;started=ticks;server(mc,()->{
                PlanetConfig.SPACE.set(true);PlanetConfig.ENABLED.set(true);
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.stopRiding();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setNoGravity(true);
                var level=p.serverLevel();var c=SubLevelContainer.getContainer(level);
                for(var old:new ArrayList<>(c.getAllSubLevels()))if("around_space_smoke".equals(old.getName())) {var cell=SpaceCells.get(level).forShip(old.getUniqueId());if(cell!=null)SpaceCells.get(level).remove(level,cell);c.removeSubLevel(old,SubLevelRemovalReason.REMOVED);}
                var pos=new BlockPos(40,280,40);level.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
                ship=SubLevelAssemblyHelper.assembleBlocks(level,pos,List.of(pos),new BoundingBox3i(40,280,40,40,280,40));id=ship.getUniqueId();ship.setName("around_space_smoke");
                c.addForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);c.physicsSystem().setPaused(true);
                var pipeline=c.physicsSystem().getPipeline();pipeline.teleport(ship,new Vector3d(50,PlanetConfig.SPACE_ENTRY.getAsInt()-4,50),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();
                var b=ship.getPlot().getBoundingBox();var seatBlock=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:white_seat"));
                level.setBlockAndUpdate(new BlockPos(b.minX(),b.minY()+1,b.minZ()),seatBlock.defaultBlockState());
                seat=(Entity)Class.forName("com.simibubi.create.content.contraptions.actors.seat.SeatEntity").getConstructor(Level.class).newInstance(level);
                seat.setPos(b.minX()+.5,b.minY()+1,b.minZ()+.5);level.addFreshEntity(seat);
                inventory=level.getBlockEntity(new BlockPos(b.minX(),b.minY(),b.minZ()));require(inventory!=null,"Chest did not survive assembly");
                p.teleportTo(50,PlanetConfig.SPACE_ENTRY.getAsInt()-2,50);p.startRiding(seat,true);seat.positionRider(p);
            });
        }else if(phase==1 && ticks-started>60 && client(mc)!=null && mc.player.isPassenger()) {
            mesh=client(mc).getRenderData();clientIdentity=client(mc);phase=2;started=ticks;
            server(mc,()->{var c=SubLevelContainer.getContainer(ship.getLevel());var center=ship.logicalPose().transformPosition(new Vector3d(ship.getSelfMassTracker().getCenterOfMass()));
                c.physicsSystem().getPipeline().teleport(ship,new Vector3d(ship.logicalPose().position()).add(0,PlanetConfig.SPACE_ENTRY.getAsInt()+2-center.y,0),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();seat.positionRider(ship.getLevel().players().getFirst());});
        }else if(phase==2 && ticks-started>60 && SpaceCells.contains(mc.player) && client(mc)!=null) {
            var chunk=mc.level.getChunkSource().getChunk(mc.player.chunkPosition().x,mc.player.chunkPosition().z,ChunkStatus.FULL,false);
            if(chunk==null || chunk instanceof net.minecraft.world.level.chunk.EmptyLevelChunk)return;
            require(mc.player.isPassenger() && mc.player.getVehicle().getUUID().equals(seat.getUUID()),"Seat mount changed");
            require(client(mc)==clientIdentity && client(mc).getRenderData()==mesh,"Space entry recreated ship/GPU mesh");
            require(!(Boolean)reflect("io.github.astelev.cruising.client.ClientFlightView","isActive",new Class<?>[]{net.minecraft.client.multiplayer.ClientLevel.class},mc.level),"Cruising temporary client view active in space");
            require(!(Boolean)reflect("io.github.astelev.cruising.compat.dh.DhFlightState","bypassMotionPause",new Class<?>[0]),"Cruising DH height mode active in space");
            AroundPlanet.LOGGER.info("SPACE_SMOKE_ENTER PASS: native ship/seat/mesh identity retained, full client chunks, Cruising height mode disabled");
            phase=20;started=ticks;server(mc,()->{
                var p=ship.getLevel().players().getFirst();require(!(Boolean)reflect("io.github.astelev.cruising.flight.FlightHooks","isHighShipEntity",new Class<?>[]{Entity.class},p),"Cruising skipped player's terrain in cell");
                require((Double)reflect("io.github.astelev.cruising.flight.ConnectedFlightAssembly","currentMinY",new Class<?>[]{dev.ryanhcode.sable.sublevel.SubLevel.class},ship)==Double.NEGATIVE_INFINITY,"Cruising ship ticket exclusion not active");
                var b=ship.getPlot().getBoundingBox();require(ship.getLevel().getBlockEntity(new BlockPos(b.minX(),b.minY(),b.minZ()))==inventory,"Native block entity replaced");
                var c=SubLevelContainer.getContainer(ship.getLevel());
                c.removeForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);live=true;c.physicsSystem().setPaused(false);
            });
        }else if(phase==20 && ticks-started>60) {
            require(client(mc)==clientIdentity && mc.player.isPassenger() && SpaceCells.contains(mc.player),"Native simulation unloaded occupied cell without forced ticket");
            AroundPlanet.LOGGER.info("SPACE_SMOKE_ACTIVE PASS: native physics/UDP running, no forced ship ticket, seated player retains full chunks and the same ship");
            phase=3;started=ticks;server(mc,()->require(SpaceShipTransfers.requestReturn(ship.getLevel().players().getFirst()),"Return request rejected"));
        }else if(phase==3 && ticks-started>60 && !SpaceCells.contains(mc.player) && mc.player.getY()<PlanetConfig.SPACE_ENTRY.getAsInt()) {
            require(mc.player.isPassenger(),"Return detached passenger");require(client(mc)==clientIdentity && client(mc).getRenderData()==mesh,"Return recreated ship");
            require(Math.abs(mc.player.getX()-50)<2 && Math.abs(mc.player.getZ()-50)<2,"Return lost surface location");
            require((Boolean)reflect("io.github.astelev.cruising.client.ClientFlightView","isActive",new Class<?>[]{net.minecraft.client.multiplayer.ClientLevel.class},mc.level),"Cruising height mode did not resume outside space");
            AroundPlanet.LOGGER.info("SPACE_SMOKE_RETURN PASS: same ship, plot, seat and passenger returned to the saved surface location");
            phase=4;started=ticks;live=false;server(mc,()->{var p=ship.getLevel().players().getFirst();p.stopRiding();require(Sable.HELPER.getContaining(p)==null,"Dismount leaked plot position");p.teleportTo(0,280,0);p.getAbilities().flying=true;p.onUpdateAbilities();seat.discard();var c=SubLevelContainer.getContainer(ship.getLevel());c.removeForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);c.physicsSystem().setPaused(false);});
        }else if(phase==4 && ticks-started>40)mc.stop();
    }
    private static Object reflect(String name,String method,Class<?>[] types,Object...args) {try{return Class.forName(name).getMethod(method,types).invoke(null,args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static ClientSubLevel client(Minecraft mc){return id==null?null:(ClientSubLevel)SubLevelContainer.getContainer(mc.level).getSubLevel(id);}
    @FunctionalInterface private interface Action{void run()throws Exception;}
    private static void server(Minecraft mc,Action a){mc.getSingleplayerServer().execute(()->{try{a.run();}catch(Throwable e){failure=e;}});}
    private static void require(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
