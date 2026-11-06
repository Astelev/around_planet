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
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) { event.register(EastWestGameTests.class); }

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
            h.assertTrue(border.getMinX()==-264 && border.getMaxX()==264,"E/W guard missing");
            h.assertTrue(border.getMinZ()==zMin && border.getMaxZ()==zMax,"North/south border was changed");
            h.assertTrue(border.getSize()==savedSize,"Saved vanilla border size was overwritten");
            h.assertTrue(Shapes.joinIsNotEmpty(border.getCollisionShape(),Shapes.box(265,190,0,266,191,1),BooleanOp.AND),"Guard has no collision");
            h.assertTrue(!Shapes.joinIsNotEmpty(border.getCollisionShape(),Shapes.box(0,190,1000,1,191,1001),BooleanOp.AND),"Unexpected north/south wall");
            var displaced=new net.minecraft.world.level.border.WorldBorder();
            displaced.setCenter(1000,0);displaced.setSize(20);PlanetBorders.bind(displaced,new EastWestTopology(512));
            h.assertTrue(displaced.getMinX()==990 && displaced.getMaxX()==1010,"Disjoint vanilla border was inverted");
            displaced.getCollisionShape();PlanetBorders.unbind(displaced);
            // Synchronous loads here are test fixtures only, never the transfer implementation.
            for(int x:new int[]{-17,-16,15,16}) for(int z=0;z<=2;z++) level.getChunk(x,z);
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
            h.assertTrue(blocked.getX()>0 && blocked.getX()<=256,"Entity transferred into solid terrain");
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
