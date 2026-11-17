package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.atlas.*;
import io.github.astelev.around_planet.network.*;
import io.github.astelev.around_planet.compat.*;
import net.minecraft.gametest.framework.*;
import java.util.*;
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class FlightToolsGameTests {
    @GameTest(template="empty",templateNamespace="around_planet",batch="flightTools",timeoutTicks=100)
    public static void burnProgressUsesDirectionAndSurvivesSave(GameTestHelper h){
        var data=new SpaceOrbitData();var id=UUID.randomUUID();var path=new SpaceTrajectory("test",0,new SpaceState(new SpaceVector(30000,0,0),new SpaceVector(0,300,0)),4e9);
        data.put(id,path);var node=new ManeuverNode(UUID.randomUUID(),1000,20,0,0,10);data.maneuver(id,node);
        data.impulse(id,799,new SpaceVector(0,1,0));h.assertTrue(data.maneuvers(id).getFirst().magnitude()==20,"Acceleration before warning changed plan");
        var direction=node.delta(data.get(id).orElseThrow().at(1000)).scale(1.0/20);
        data.impulse(id,800,direction.scale(5));h.assertTrue(Math.abs(data.maneuvers(id).getFirst().magnitude()-15)<1e-8,"Correct burn did not reduce remaining delta-v");
        var current=data.maneuvers(id).getFirst();var residual=current.delta(data.get(id).orElseThrow().at(1000));
        data.impulse(id,801,residual.scale(-2.0/15));h.assertTrue(Math.abs(data.maneuvers(id).getFirst().magnitude()-17)<1e-8,"Opposite burn incorrectly reduced delta-v");
        var loaded=SpaceOrbitData.load(data.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(loaded.maneuvers(id).equals(data.maneuvers(id)),"Remaining maneuver lost on reload");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="around_planet",batch="flightTools",timeoutTicks=100)
    public static void atlasChunksStayBoundedAndReconstructHighResolutionImage(GameTestHelper h){
        h.assertTrue(PlanetAtlas.WIDTH==1024&&PlanetAtlas.HEIGHT==512,"Atlas resolution did not double");int[] result=new int[PlanetAtlas.SIZE];
        for(int offset=0;offset<result.length;offset+=PlanetAtlasPayload.Part.CHUNK){int[] source=new int[Math.min(PlanetAtlasPayload.Part.CHUNK,result.length-offset)];for(int i=0;i<source.length;i++)source[i]=offset+i;
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
            try{PlanetAtlasPayload.Part.CODEC.encode(buffer,new PlanetAtlasPayload.Part(131072,offset,source));h.assertTrue(buffer.readableBytes()<300000,"Atlas packet too large");var copy=PlanetAtlasPayload.Part.CODEC.decode(buffer);System.arraycopy(copy.pixels(),0,result,copy.offset(),copy.pixels().length);}finally{buffer.release();}
        }
        for(int i=0;i<result.length;i++)h.assertTrue(result[i]==i,"Atlas chunk reconstruction corrupted pixels");
        var old=new net.minecraft.nbt.CompoundTag();old.putInt("Version",2);old.putIntArray("Pixels",new int[512*256]);old.putInt("Progress",512*256);
        h.assertTrue(!PlanetAtlasData.load(old,h.getLevel().registryAccess()).ready(),"Old low resolution cache was reused");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="around_planet",batch="flightTools",timeoutTicks=100)
    public static void telescopeObservesUnloadedObjectsWithoutAddingAnOrbit(GameTestHelper h){
        var level=h.getLevel();var data=SpaceOrbitData.get(level.getServer());var id=UUID.randomUUID();var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);var oldPos=player.position();var oldSystem=data.system();var block=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));
        try{level.setBlockAndUpdate(block,io.github.astelev.around_planet.maneuver.ManeuverRegistry.TELESCOPE.get().defaultBlockState());player.setPos(block.getX()+1,block.getY()+1,block.getZ()+1);data.put(id,new SpaceTrajectory(CelestialSystem.EARTH,level.getGameTime(),new SpaceState(new SpaceVector(90000,0,0),new SpaceVector(0,200,0)),4e9));int chunks=level.getChunkSource().getLoadedChunksCount();
            TelescopeAccess.authorize(player,block);h.assertTrue(TelescopeAccess.active(player),"Real nearby telescope did not authorize observation");var packet=TelescopeAccess.snapshot(player);h.assertTrue(packet.bodies().stream().anyMatch(b->b.id().equals(id)&&!b.loaded()),"Unloaded ship missing from telescope");h.assertTrue(data.get(player.getUUID()).isEmpty(),"Observer was registered as a fake vessel");h.assertTrue(level.getChunkSource().getLoadedChunksCount()==chunks,"Telescope loaded observed chunks");player.setPos(block.getX()+20,block.getY(),block.getZ());h.assertTrue(!TelescopeAccess.active(player),"Telescope session survived leaving reach");h.succeed();
        }finally{TelescopeAccess.close(player);player.setPos(oldPos);data.remove(id);data.system(oldSystem);level.removeBlock(block,false);}
    }
    @GameTest(template="empty",templateNamespace="around_planet",batch="flightTools",timeoutTicks=150)
    public static void surfaceSensorMeasuresNativeVelocity(GameTestHelper h){h.runAfterDelay(3,()->{
        var level=h.getLevel();var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));level.setBlockAndUpdate(pos,io.github.astelev.around_planet.sensor.OrbitalSensorRegistry.BLOCK.get().defaultBlockState());
        var ship=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(level,pos,List.of(pos),new dev.ryanhcode.sable.companion.math.BoundingBox3i(pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ()));
        try{var sensor=ship.getPlot().getLoadedChunks().stream().flatMap(c->c.getChunk().getBlockEntities().values().stream()).filter(io.github.astelev.around_planet.sensor.OrbitalSensorBlockEntity.class::isInstance).map(io.github.astelev.around_planet.sensor.OrbitalSensorBlockEntity.class::cast).findFirst().orElseThrow();var pipe=container.physicsSystem().getPipeline();pipe.resetVelocity(ship);pipe.addLinearAndAngularVelocity(ship,new org.joml.Vector3d(3,4,0),new org.joml.Vector3d());io.github.astelev.around_planet.sensor.OrbitalSensorBlockEntity.tick(level,sensor.getBlockPos(),sensor.getBlockState(),sensor);h.assertTrue(Math.abs(sensor.surfaceSpeed()-5)<1e-5,"Surface sensor ignored native vessel speed");h.succeed();}finally{if(!ship.isRemoved())container.removeSubLevel(ship,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);}
    });}
}
