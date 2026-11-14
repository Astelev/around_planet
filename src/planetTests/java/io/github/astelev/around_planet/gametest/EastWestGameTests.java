package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.topology.EastWestTopology;
import io.github.astelev.around_planet.transfer.TransferService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;

@GameTestHolder(AroundPlanet.MOD_ID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class EastWestGameTests {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) { event.register(EastWestGameTests.class);event.register(SeamInteractionGameTests.class);event.register(GenerationSeamGameTests.class);event.register(SpaceOrbitGameTests.class);event.register(PlanetRotationGameTests.class);if(net.neoforged.fml.ModList.get().isLoaded("sable")){event.register(ShipSeamGameTests.class);event.register(SpaceSensorGameTests.class);event.register(PropulsionGameTests.class);} }

    private static void playerPreloadCannotBeStarved(GameTestHelper h) {
        var level=h.getLevel();var stop=new net.neoforged.neoforge.event.server.ServerStoppingEvent(level.getServer());
        TransferService.stopping(stop);
        try {
            for(int z=0;z<16;z++) {
                var item=new ItemEntity(level,255,200,z*16+8,new ItemStack(Items.STICK));
                TransferService.check(item);
            }
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setPos(255,200,4001*16+8);TransferService.check(player);
            String ticket;
            try {
                var debug=net.minecraft.server.level.DistanceManager.class.getDeclaredMethod("getTicketDebugString",long.class);debug.setAccessible(true);
                ticket=(String)debug.invoke(level.getChunkSource().chunkMap.getDistanceManager(),new net.minecraft.world.level.ChunkPos(-16,4001).toLong());
            } catch(ReflectiveOperationException error){throw new AssertionError(error);}
            h.assertTrue(ticket.contains("around_planet_destination"),"Player destination was starved by 16 stationary entity preloads: "+ticket);
        } finally {TransferService.stopping(stop);}
    }

    private static void stitchedCollisionsAndEscape(GameTestHelper h) {
        var level=h.getLevel();
        var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        java.util.function.BiConsumer<BlockPos,net.minecraft.world.level.block.state.BlockState> put=(pos,state)->{
            saved.putIfAbsent(pos,level.getBlockState(pos));level.setBlockAndUpdate(pos,state);
        };
        var air=Blocks.AIR.defaultBlockState();var stone=Blocks.STONE.defaultBlockState();
        try {
            level.getChunk(15,77);level.getChunk(-16,77);
            h.assertTrue(level.getChunkSource().getChunkNow(-17,77)==null,"Unused guard column must start absent");
            var straddling=EntityType.BOAT.create(level);straddling.setPos(256.1,250,77*16+8);
            TransferService.check(straddling);
            h.assertTrue(straddling.getX()<0,"Straddling hull waited for a nonexistent guard chunk instead of its canonical source");
            h.assertTrue(level.getChunkSource().getChunkNow(-17,77)==null,"Collision test generated the raw guard column");
            straddling.discard();
            for(int x:new int[]{-258,-257,-256,-255,-254,-253,253,254,255,256,257,258})
                for(int z=30;z<=34;z++)for(int y=218;y<=223;y++)put.accept(new BlockPos(x,y,z),air);
            for(int direction:new int[]{1,-1}) {
                double start=direction>0?255.5:-255.5;
                int ghost=direction>0?256:-257, oppositeGhost=direction>0?-257:256;
                // Both raw guard columns disagree with their visible periodic images.
                for(int y=220;y<=221;y++) {
                    put.accept(new BlockPos(ghost,y,30),stone);
                    put.accept(new BlockPos(oppositeGhost,y,30),stone);
                }
                var walker=EntityType.ZOMBIE.create(level);walker.setPos(start,220,30.5);
                walker.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(direction*0.7,0,0));
                double before=walker.getX();
                h.assertTrue(direction>0?before>256:before<-256,"Invisible guard terrain blocked the open seam passage");
                TransferService.check(walker);
                h.assertTrue(Math.abs(walker.getX()-(before-direction*512))<1e-6,
                        "A straddling destination hull tested the wrong side of the seam");
                walker.discard();
                int realWall=direction>0?-256:255;
                for(int y=220;y<=221;y++)put.accept(new BlockPos(realWall,y,31),stone);
                var wallWalker=EntityType.ZOMBIE.create(level);wallWalker.setPos(start,220,31.5);
                wallWalker.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(direction*1.0,0,0.2));
                double stopped=wallWalker.getX();
                h.assertTrue(direction>0?stopped<=255.71:stopped>=-255.71,"Opposite-side wall did not stop ordinary movement before crossing");
                h.assertTrue(Math.abs(wallWalker.getZ()-31.7)<1e-6,"Wall prevented tangential sliding");
                for(int i=0;i<3;i++)TransferService.check(wallWalker);
                h.assertTrue(wallWalker.getX()==stopped,"Stationary wall contact caused a teleport correction");
                wallWalker.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(-direction*0.4,0,0));
                h.assertTrue(Math.abs(wallWalker.getX()-stopped+direction*0.4)<1e-6,"Could not back away from the seam wall");
                wallWalker.discard();
                for(int y=220;y<=221;y++) {
                    put.accept(new BlockPos(ghost,y,30),air);put.accept(new BlockPos(oppositeGhost,y,30),air);
                    put.accept(new BlockPos(realWall,y,31),air);
                }
            }
            // Exact floor and two-block ceiling in a narrow seam tunnel.
            for(int x:new int[]{254,255,-256,-255}) {
                put.accept(new BlockPos(x,219,32),stone);put.accept(new BlockPos(x,222,32),stone);
                for(int y=220;y<=221;y++) {put.accept(new BlockPos(x,y,31),stone);put.accept(new BlockPos(x,y,33),stone);}
            }
            var tunnel=EntityType.ZOMBIE.create(level);tunnel.setPos(255.5,220,32.5);
            tunnel.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0.7,0,0));TransferService.check(tunnel);
            h.assertTrue(tunnel.getX()<0,"Could not cross an open two-block-high seam tunnel");
            tunnel.discard();
            // Even an already embedded vehicle must be able to move away, without repeated repositioning.
            var boat=EntityType.BOAT.create(level);boat.setPos(256.1,220,34.8);
            var rider=EntityType.ZOMBIE.create(level);rider.setPos(256.1,220,34.8);rider.startRiding(boat,true);
            put.accept(new BlockPos(-256,220,34),stone);
            for(int i=0;i<3;i++)TransferService.check(boat);
            h.assertTrue(Math.abs(boat.getX()+255.9)<1e-6 && rider.getVehicle()==boat,"An existing overlap stranded the vehicle in an untracked guard column");
            rider.stopRiding();h.assertTrue(!rider.isPassenger(),"Could not dismount a pending seam vehicle");
            boat.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(-1,0,0));
            h.assertTrue(boat.getX()<-256.8,"Embedded seam vehicle could not reverse out");
            boat.discard();rider.discard();
        } finally {saved.forEach(level::setBlockAndUpdate);}
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void northSouthWallsPreserveEastWestSeam(GameTestHelper h) {
        var border=new net.minecraft.world.level.border.WorldBorder();
        double originalSize=border.getSize();
        PlanetBorders.bind(border,new EastWestTopology(512),128);
        try {
            h.assertTrue(border.getMinZ()==-128 && border.getMaxZ()==128,"Solid north/south bounds missing");
            h.assertTrue(border.isWithinBounds(256.75,0) && border.isWithinBounds(-256.75,0),"North/south border blocked the east/west crossing corridor");
            h.assertTrue(!border.isWithinBounds(0,128.5) && !border.isWithinBounds(0,-128.5),"North/south wall is traversable");
            for(double z:new double[]{128,-129})
                h.assertTrue(Shapes.joinIsNotEmpty(border.getCollisionShape(),Shapes.box(0,190,z,1,191,z+1),BooleanOp.AND),"North/south wall has no collision");
            h.assertTrue(!Shapes.joinIsNotEmpty(border.getCollisionShape(),Shapes.box(0,190,-127,1,191,127),BooleanOp.AND),"Border obstructs the interior");
            var probe=new ItemEntity(h.getLevel(),0,200,127,new ItemStack(Items.STICK));
            h.assertTrue(PlanetBorders.warningDistance(border,probe)==1,"South wall danger warning missing");
            probe.setPos(0,200,-127);h.assertTrue(PlanetBorders.warningDistance(border,probe)==1,"North wall danger warning missing");
            probe.setPos(256.75,200,0);h.assertTrue(PlanetBorders.warningDistance(border,probe)==128,"Invisible east/west guard triggers danger warning");
            border.setSize(100);
            h.assertTrue(border.getMinZ()==-50 && border.getMaxZ()==50 && border.getMinX()==-50,"Tighter vanilla border ignored");
            border.setSize(originalSize);PlanetBorders.unbind(border);
            h.assertTrue(border.getMaxZ()>1000000,"Disabling planet border did not restore vanilla bounds");
            PlanetBorders.bind(border,new EastWestTopology(512),128);border.setCenter(0,1000);border.setSize(20);
            h.assertTrue(border.getMinZ()==990 && border.getMaxZ()==1010,"Disjoint vanilla Z border inverted");
            h.succeed();
        } finally {PlanetBorders.unbind(border);}
    }

    @GameTest(template="empty",timeoutTicks=400)
    public static void realChunksEntitiesVehiclesAndDirectionalBorder(GameTestHelper h) {
        var level=h.getLevel(); var border=level.getWorldBorder();
        double zMin=border.getMinZ(),zMax=border.getMaxZ(),savedSize=border.getSize();
        var entities=new ArrayList<Entity>();
        var obstacle=new BlockPos(-256,200,10);
        var oldBlock=level.getBlockState(obstacle);
        PlanetBorders.bind(border,new EastWestTopology(512));
        try {
            playerPreloadCannotBeStarved(h);
            h.assertTrue(border.getMinX()==-256-PlanetBorders.guardMargin() && border.getMaxX()==256+PlanetBorders.guardMargin(),"E/W guard missing");
            h.assertTrue(border.getMinZ()==zMin && border.getMaxZ()==zMax,"North/south border was changed");
            h.assertTrue(border.getSize()==savedSize,"Saved vanilla border size was overwritten");
            h.assertTrue(Shapes.joinIsNotEmpty(border.getCollisionShape(),Shapes.box(border.getMaxX()+1,190,0,border.getMaxX()+2,191,1),BooleanOp.AND),"Guard has no collision");
            h.assertTrue(!Shapes.joinIsNotEmpty(border.getCollisionShape(),Shapes.box(0,190,1000,1,191,1001),BooleanOp.AND),"Unexpected north/south wall");
            var displaced=new net.minecraft.world.level.border.WorldBorder();
            displaced.setCenter(1000,0);displaced.setSize(20);PlanetBorders.bind(displaced,new EastWestTopology(512));
            h.assertTrue(displaced.getMinX()==990 && displaced.getMaxX()==1010,"Disjoint vanilla border was inverted");
            displaced.getCollisionShape();PlanetBorders.unbind(displaced);
            // Synchronous loads here are test fixtures only, never the transfer implementation.
            for(int x:new int[]{-17,-16,15,16}) for(int z=0;z<=2;z++) level.getChunk(x,z);
            stitchedCollisionsAndEscape(h);
            var east=new ItemEntity(level,256.75,200,5,new ItemStack(Items.STICK));
            h.assertTrue(PlanetBorders.warningDistance(border,east)>10000,"Wrap seam triggers danger vignette");
            east.setNoGravity(true); east.setYRot(37); east.setDeltaMovement(4,0.25,0.5); level.addFreshEntity(east); entities.add(east);
            TransferService.tick(new EntityTickEvent.Post(east));
            h.assertTrue(east.getX()==-255.25 && east.getY()==200 && east.getZ()==5,"East overshoot/Y/Z changed");
            h.assertTrue(east.getDeltaMovement().equals(new Vec3(4,0.25,0.5)) && east.getYRot()==37,"Velocity or yaw changed");
            var tighter=new net.minecraft.world.level.border.WorldBorder();tighter.setSize(400);
            PlanetBorders.bind(tighter,new EastWestTopology(512));
            var warningProbe=new ItemEntity(level,199,200,5,new ItemStack(Items.STICK));
            h.assertTrue(PlanetBorders.warningDistance(tighter,warningProbe)==1,"Real vanilla border warning was suppressed");
            PlanetBorders.unbind(tighter);
            var west=new ItemEntity(level,-256.125,200,8,new ItemStack(Items.STICK)); level.addFreshEntity(west); entities.add(west);
            TransferService.tick(new EntityTickEvent.Post(west));
            h.assertTrue(west.getX()==255.875,"West fractional offset lost");
            var inactive=new ItemEntity(level,257,200,25,new ItemStack(Items.STICK));
            level.addFreshEntity(inactive);entities.add(inactive);
            // View-distance filtering can leave the guard chunk without entity ticks.
            // The join event plus level retry must transfer it without EntityTickEvent.Post.
            TransferService.retry(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(()->true,level));
            h.assertTrue(inactive.getX()==-255,"Entity in a non-ticking guard chunk was stranded");
            var blocked=new ItemEntity(level,256.5,200,10.5,new ItemStack(Items.STICK)); level.addFreshEntity(blocked); entities.add(blocked);
            level.setBlockAndUpdate(obstacle,Blocks.STONE.defaultBlockState());
            TransferService.tick(new EntityTickEvent.Post(blocked));
            h.assertTrue(blocked.getX()==-255.5,"Rebasing an existing overlap stranded the entity outside the canonical world");
            level.getChunk(15,62500); level.getChunk(16,62500);
            var unloaded=new ItemEntity(level,257,200,1000000.5,new ItemStack(Items.STICK)); level.addFreshEntity(unloaded); entities.add(unloaded);
            h.assertTrue(level.getChunkSource().getChunkNow(-16,62500)==null,"Unloaded probe must start absent");
            TransferService.tick(new EntityTickEvent.Post(unloaded));
            h.assertTrue(unloaded.getX()>0 && unloaded.getX()<=256,"Entity entered unloaded destination");
            h.assertTrue(level.getChunkSource().getChunkNow(-16,62500)==null,"Transfer synchronously generated terrain");
            PlanetBorders.bind(border,EastWestTopology.atBoundary(65536));
            level.getChunk(-4096,0);level.getChunk(4096,0);
            var defaultEdge=new ItemEntity(level,65536.75,200,5,new ItemStack(Items.STICK));
            level.addFreshEntity(defaultEdge);entities.add(defaultEdge);
            TransferService.tick(new EntityTickEvent.Post(defaultEdge));
            h.assertTrue(defaultEdge.getX()==-65535.25,"Default 65536 boundary wrapped to zero or used a delayed threshold");
            PlanetBorders.bind(border,new EastWestTopology(512));
            var boat=EntityType.BOAT.create(level); boat.moveTo(256.5,200,20); level.addFreshEntity(boat);entities.add(boat);
            var pig=EntityType.PIG.create(level); pig.moveTo(256.5,200,20); level.addFreshEntity(pig);entities.add(pig);
            h.assertTrue(pig.startRiding(boat,true),"Could not attach test rider");
            boat.setDeltaMovement(2,0,0); pig.setDeltaMovement(0.1,0,0);
            TransferService.tick(new EntityTickEvent.Post(boat));
            h.assertTrue(boat.getX()==-255.5 && pig.getX()<0 && pig.getVehicle()==boat,"Vehicle/rider group separated");
            h.assertTrue(boat.getDeltaMovement().x==2 && pig.getDeltaMovement().x==0.1,"Vehicle/rider velocities changed");
            var approachingPlayer=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            approachingPlayer.setPos(255,200,1000000.5);
            TransferService.check(approachingPlayer);
            h.succeedWhen(() -> {
                for(int d=0;d<=3;d++)for(int dz=-3;dz<=3;dz++) {
                    var chunk=level.getChunkSource().getChunkNow(-16+d,62500+dz);
                    h.assertTrue(chunk!=null && chunk.isLightCorrect(),"Preload did not finish the outer preview strip: "+(-16+d)+","+(62500+dz));
                }
            });
        } finally {
            entities.forEach(Entity::discard); level.setBlockAndUpdate(obstacle,oldBlock);
            PlanetBorders.unbind(border);
        }
    }
}
