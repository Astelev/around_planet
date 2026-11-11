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
    private static int ticks,phase,started;private static boolean captured;private static volatile Throwable failure;
    private static volatile ServerSubLevel freeShip;private static volatile ServerSubLevel ship;private static volatile Entity seat;private static volatile UUID id;private static Object mesh,clientIdentity,inventory;
    private static volatile boolean live,kicked;private static volatile long kickAt;private static volatile SpaceCells.Cell cell;private static volatile double baseline;
    @SubscribeEvent public static void drive(net.neoforged.neoforge.event.tick.LevelTickEvent.Pre event) {
        if(!Boolean.getBoolean("around_planet.spaceEntrySmoke") || !live || kicked || ship==null || event.getLevel()!=ship.getLevel() || ship.getLevel().getGameTime()<kickAt)return;
        try {SubLevelContainer.getContainer(ship.getLevel()).physicsSystem().getPipeline().addLinearAndAngularVelocity(ship,new Vector3d(2,0,0),new Vector3d());kicked=true;}catch(Throwable e){failure=e;}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("around_planet.spaceEntrySmoke"))return;
        var mc=Minecraft.getInstance();ticks++;
        if(failure!=null)throw new AssertionError("SPACE_SMOKE server",failure);
        if(ticks>2200)throw new AssertionError("SPACE_SMOKE timeout phase="+phase);
        if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null || ticks<100)return;
        if(ticks%100==0)AroundPlanet.LOGGER.info("SPACE_SMOKE phase={} player={} cell={} shipY={} mounted={} seatClient={} seatRemoved={}",phase,mc.player.position(),SpaceCells.contains(mc.player),client(mc)==null?null:client(mc).logicalPose().position().y,mc.player.isPassenger(),seat==null?null:mc.level.getEntity(seat.getId()),seat==null?null:seat.isRemoved());
        if(phase==1 && ticks-started>40 && ticks%20==0 && client(mc)!=null && seat!=null && !mc.player.isPassenger() ) {
            // A programmatically spawned seat may precede client plot tracking on a slow renderer.
            boolean missing=mc.level.getEntity(seat.getId())==null;
            server(mc,()->{var p=ship.getLevel().players().getFirst();if(missing)new net.minecraft.server.level.ServerEntity(ship.getLevel(),seat,1,true,p.connection::send).addPairing(p);if(p.getVehicle()!=seat)p.startRiding(seat,true);seat.positionRider(p);p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(seat));});
        }
        if(phase==0) {
            phase=1;started=ticks;server(mc,()->{
                PlanetConfig.SPACE.set(true);PlanetConfig.ENABLED.set(true);PlanetConfig.SPACE_GRAVITY.set(0.0);
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
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);mc.player.setXRot(30);mc.player.setYRot(45);
            phase=20;started=ticks;server(mc,()->{
                var p=ship.getLevel().players().getFirst();require(!(Boolean)reflect("io.github.astelev.cruising.flight.FlightHooks","isHighShipEntity",new Class<?>[]{Entity.class},p),"Cruising skipped player's terrain in cell");
                require((Double)reflect("io.github.astelev.cruising.flight.ConnectedFlightAssembly","currentMinY",new Class<?>[]{dev.ryanhcode.sable.sublevel.SubLevel.class},ship)==Double.NEGATIVE_INFINITY,"Cruising ship ticket exclusion not active");
                var b=ship.getPlot().getBoundingBox();require(ship.getLevel().getBlockEntity(new BlockPos(b.minX(),b.minY(),b.minZ()))==inventory,"Native block entity replaced");
                cell=SpaceCells.get(ship.getLevel()).forShip(id);
                var orbits=io.github.astelev.around_planet.space.SpaceOrbitData.get(ship.getLevel().getServer());baseline=orbits.navigation(cell.id()).orElseThrow().frame().toLocal(orbits.sample(cell.id(),ship.getLevel().getGameTime()).velocity()).x();kickAt=ship.getLevel().getGameTime()+20;
                var c=SubLevelContainer.getContainer(ship.getLevel());
                c.removeForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);live=true;c.physicsSystem().setPaused(false);
            });
        }else if(phase==20 && ticks-started>100 && !captured) {
            captured=true;net.minecraft.client.Screenshot.grab(mc.gameDirectory,"space-navigation-alpha3.png",mc.getMainRenderTarget(),message->{});
        }else if(phase==20 && ticks-started>200) {
            require(client(mc)==clientIdentity && mc.player.isPassenger() && SpaceCells.contains(mc.player),"Native simulation unloaded occupied cell without forced ticket");
            require(Math.abs(mc.player.getY()-cell.y())<2,"Player/ship fell out of space");
            require(io.github.astelev.around_planet.client.SpaceRenderView.terrainSkips>0 && io.github.astelev.around_planet.client.SpaceRenderView.dhSkips>0 && io.github.astelev.around_planet.client.SpaceRenderView.cloudSkips>0 && io.github.astelev.around_planet.client.SpaceRenderView.skySkips>0,"Background render hooks did not run");
            if(net.neoforged.fml.ModList.get().isLoaded("simpleclouds"))require(io.github.astelev.around_planet.client.SpaceRenderView.simpleCloudSkips>0,"Simple Clouds render path was not suppressed");
            require(io.github.astelev.around_planet.client.SpaceRenderView.shipPasses>0,"Ship render callbacks were suppressed");
            server(mc,()->{
                var center=ship.logicalPose().transformPosition(new Vector3d(ship.getSelfMassTracker().getCenterOfMass()));require(center.distance(new Vector3d(cell.x(),cell.y(),cell.z()))<.02,"Owner frame drifted");
                var orbit=io.github.astelev.around_planet.space.SpaceOrbitData.get(ship.getLevel().getServer());double vx=orbit.navigation(cell.id()).orElseThrow().frame().toLocal(orbit.sample(cell.id(),ship.getLevel().getGameTime()).velocity()).x();require(Math.abs(vx-baseline-2)<.02,"Engine impulse did not enter virtual velocity: "+vx);
            });
            require(io.github.astelev.around_planet.client.SpaceHud.current()!=null && io.github.astelev.around_planet.client.SpaceHud.current().active(),"Cosmic HUD telemetry not received");
            AroundPlanet.LOGGER.info("SPACE_SMOKE_ACTIVE PASS: 10 seconds of native physics without a hover driver; stable owner frame, virtual impulse, background hidden and ship render retained");
            phase=3;started=ticks;server(mc,()->{
                var level=ship.getLevel();var c=SubLevelContainer.getContainer(level);var pos=new BlockPos(43,280,43);level.setBlockAndUpdate(pos,Blocks.IRON_BLOCK.defaultBlockState());
                freeShip=SubLevelAssemblyHelper.assembleBlocks(level,pos,List.of(pos),new BoundingBox3i(43,280,43,43,280,43));freeShip.setName("around_space_smoke");c.addForceLoadTicket(freeShip,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);
                c.physicsSystem().getPipeline().teleport(freeShip,new Vector3d(cell.x()+10,cell.y()+8,cell.z()),freeShip.logicalPose().orientation());freeShip.updateBoundingBox();freeShip.updateLastPose();
                var orbit=io.github.astelev.around_planet.space.SpaceOrbitData.get(level.getServer());var nav=orbit.navigation(cell.id()).orElseThrow();var planet=nav.planet();
                orbit.put(cell.id(),new io.github.astelev.around_planet.space.SpaceTrajectory("around_planet:overworld",level.getGameTime(),new io.github.astelev.around_planet.space.SpaceState(planet.position(80,737,60),planet.frame(80,60).toCosmic(new io.github.astelev.around_planet.space.SpaceVector(10,-20,2))),0));
            });
        }else if(phase==3 && ticks-started>60 && !SpaceCells.contains(mc.player) && mc.player.getY()<PlanetConfig.SPACE_ENTRY.getAsInt()) {
            require(mc.player.isPassenger(),"Return detached passenger");require(client(mc)==clientIdentity && client(mc).getRenderData()==mesh,"Return recreated ship");
            require(Math.abs(mc.player.getX()-80)<100 && Math.abs(mc.player.getZ()-60)<100,"Return lost surface location");
            require((Boolean)reflect("io.github.astelev.cruising.client.ClientFlightView","isActive",new Class<?>[]{net.minecraft.client.multiplayer.ClientLevel.class},mc.level),"Cruising height mode did not resume outside space");
            server(mc,()->require(freeShip.logicalPose().position().y<1100,"Free construction stayed in the space cell"));
            net.minecraft.client.Screenshot.grab(mc.gameDirectory,"space-return-alpha3.png",mc.getMainRenderTarget(),message->{});
            AroundPlanet.LOGGER.info("SPACE_SMOKE_RETURN PASS: automatic spherical reentry, free construction, original ship/plot/seat/passenger retained");
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            phase=4;started=ticks;live=false;server(mc,()->{var p=ship.getLevel().players().getFirst();p.stopRiding();require(Sable.HELPER.getContaining(p)==null,"Dismount leaked plot position");p.teleportTo(0,280,0);p.getAbilities().flying=true;p.onUpdateAbilities();seat.discard();var c=SubLevelContainer.getContainer(ship.getLevel());c.removeForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);if(freeShip!=null){c.removeForceLoadTicket(freeShip,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);c.removeSubLevel(freeShip,SubLevelRemovalReason.REMOVED);}c.physicsSystem().setPaused(false);});
        }else if(phase==4 && ticks-started>40)mc.stop();
    }
    private static Object reflect(String name,String method,Class<?>[] types,Object...args) {try{return Class.forName(name).getMethod(method,types).invoke(null,args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static ClientSubLevel client(Minecraft mc){return id==null?null:(ClientSubLevel)SubLevelContainer.getContainer(mc.level).getSubLevel(id);}
    @FunctionalInterface private interface Action{void run()throws Exception;}
    private static void server(Minecraft mc,Action a){mc.getSingleplayerServer().execute(()->{try{a.run();}catch(Throwable e){failure=e;}});}
    private static void require(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
