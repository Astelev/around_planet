package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.compat.*;
import dev.ryanhcode.sable.api.sublevel.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3d;
import org.joml.Quaterniond;
import java.util.*;
@PrefixGameTestTemplate(false)
public final class SpaceRendezvousGameTests {
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=160)
    public static void loadedFramesMergeWithoutLosingRelativeMotionAndChildren(GameTestHelper h){run(h,false);}
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=160)
    public static void heldFramesWakeInsideReceiverAndStayLoaded(GameTestHelper h){run(h,true);}
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=160)
    public static void twoUnloadedFramesDoNotWakeEachOther(GameTestHelper h){run(h,true,1);}
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=160)
    public static void fiftyMetersPerSecondIsNotAnEncounter(GameTestHelper h){run(h,false,2);}
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=160)
    public static void dormantOrbitalBindingJoinsReceiverWithInternalMotion(GameTestHelper h){run(h,true,3);}
    private static void run(GameTestHelper h,boolean held){run(h,held,0);}
    private static void run(GameTestHelper h,boolean held,int reject){var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var pipe=c.physicsSystem().getPipeline();var cells=SpaceCells.get(level);var data=SpaceOrbitData.get(level.getServer());var all=new ArrayList<UUID>();var hostId=new UUID(0,held?13:10);var sourceId=new UUID(0,held?12:11);SpaceCells.Cell host=null,source=null;double oldDistance=io.github.astelev.around_planet.PlanetConfig.RENDEZVOUS_DISTANCE.get();
        try{io.github.astelev.around_planet.PlanetConfig.RENDEZVOUS_DISTANCE.set(96.0);double hx=held?16000:12000,sx=hx+1024;for(int i=0;i<3;i++){var p=h.absolutePos(new BlockPos(1+2*i,2,1));level.setBlockAndUpdate(p,Blocks.IRON_BLOCK.defaultBlockState());var ship=SubLevelAssemblyHelper.assembleBlocks(level,p,List.of(p),new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX(),p.getY(),p.getZ()));all.add(ship.getUniqueId());pipe.teleport(ship,new Vector3d(i==0?hx:sx+(i==2?8:0),6144,0),new Quaterniond());ship.updateBoundingBox();ship.updateLastPose();}
            host=new SpaceCells.Cell(hostId,held?71:69,hx,6144,0,0,2200,0,List.of(all.get(0)));source=new SpaceCells.Cell(sourceId,held?72:70,sx,6144,0,0,2200,0,List.of(all.get(1)));cells.add(level,host);cells.add(level,source);
            var planet=new PlanetScale(131072).geometry();var rotated=new SpaceFrame(new SpaceVector(0,1,0),new SpaceVector(-1,0,0),new SpaceVector(0,0,1));var initial=new SpaceState(new SpaceVector(50000,0,0),new SpaceVector(0,500,0));data.put(hostId,new SpaceTrajectory("rendezvous-test-"+held,level.getGameTime(),initial,0));data.navigate(hostId,new SpaceNavigation(planet,SpaceFrame.IDENTITY));data.put(sourceId,new SpaceTrajectory("rendezvous-test-"+held,level.getGameTime(),new SpaceState(initial.position().add(new SpaceVector(30,0,0)),initial.velocity().add(new SpaceVector(0,reject==2?50:5,0))),0));data.navigate(sourceId,new SpaceNavigation(planet,rotated));
            var child=(ServerSubLevel)c.getSubLevel(all.get(2));pipe.addLinearAndAngularVelocity(child,new Vector3d(1,0,0),new Vector3d(.1,.2,.3));
            if(reject==3){cells.remove(level,source);SpaceDormantData.get(level.getServer()).put(new SpaceDormantData.Binding(sourceId,"Dormant group",new SpaceVector(sx,6144,0),2200,List.of(new SpaceDormantData.Member(all.get(1),new SpaceVector(sx,6144,0),new SpaceVector(0,0,0),new SpaceVector(0,0,0)),new SpaceDormantData.Member(all.get(2),new SpaceVector(sx+8,6144,0),new SpaceVector(1,0,0),new SpaceVector(.1,.2,.3)))));}
            if(held){for(var id:List.of(all.get(1),all.get(2))){var ship=(ServerSubLevel)c.getSubLevel(id);c.getHoldingChunkMap().moveToUnloaded(ship,new ChunkPos((int)sx/16,0));}h.assertTrue(c.getSubLevel(all.get(1))==null,"Source did not unload");var addresses=SpaceCargoData.get(level.getServer());h.assertTrue(addresses.all().stream().anyMatch(r->r.ship().equals(all.get(2))),"Held free child address missing");var copy=SpaceCargoData.load(addresses.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess()),level.registryAccess());h.assertTrue(copy.all().equals(addresses.all()),"Holding addresses did not persist");}
            if(reject==1)c.getHoldingChunkMap().moveToUnloaded((ServerSubLevel)c.getSubLevel(all.get(0)),new ChunkPos((int)hx/16,0));
            for(int x=(int)hx/16-2;x<=(int)hx/16+5;x++)for(int z=-2;z<=2;z++)level.getChunk(x,z);
            SpaceRendezvous.check(level,c);if(reject==1||reject==2){h.assertTrue(data.get(sourceId).isPresent()&&data.get(hostId).isPresent(),"Ineligible frames merged");if(reject==1)h.assertTrue(c.getSubLevel(all.get(0))==null&&c.getSubLevel(all.get(1))==null,"Unobserved encounter loaded native ships");h.succeed();return;}h.assertTrue(data.get(sourceId).isEmpty()&&cells.forShip(all.get(1))==null,"Source orbit not merged");h.assertTrue(data.get(hostId).orElseThrow().initial().equals(initial),"Receiver orbit changed");
            var arrival=(ServerSubLevel)c.getSubLevel(all.get(1));child=(ServerSubLevel)c.getSubLevel(all.get(2));h.assertTrue(arrival!=null&&child!=null,"Not all bodies woke");h.assertTrue(arrival.logicalPose().position().distance(new Vector3d(hx+30,6144,0))<1e-6,"Wrong encounter position");h.assertTrue(child.logicalPose().position().distance(new Vector3d(hx+30,6152,0))<1e-6,"Child offset was not rotated");h.assertTrue(pipe.getLinearVelocity(arrival,new Vector3d()).distance(new Vector3d(0,5,0))<1e-6,"Relative arrival velocity lost");h.assertTrue(pipe.getLinearVelocity(child,new Vector3d()).distance(new Vector3d(0,6,0))<1e-6,"Internal velocity lost");h.assertTrue(pipe.getAngularVelocity(child,new Vector3d()).distance(new Vector3d(-.2,.1,.3))<1e-6,"Angular velocity not rotated");h.assertTrue(c.collectForceLoadedSubLevels().contains(arrival),"Arrival not kept loaded");
            SpaceRendezvous.check(level,c);h.assertTrue(c.collectForceLoadedSubLevels().contains(arrival),"Arrival lease expired while receiver loaded");h.succeed();
        }finally{io.github.astelev.around_planet.PlanetConfig.RENDEZVOUS_DISTANCE.set(oldDistance);if(host!=null)cells.remove(level,host);if(source!=null)cells.remove(level,source);data.remove(hostId);data.remove(sourceId);SpaceDormantData.get(level.getServer()).remove(sourceId);for(var id:all){SpaceCargoData.get(level.getServer()).remove(id);var s=c.getSubLevel(id);if(s==null){var record=c.getHoldingChunkMap().getHoldingSubLevel(id);if(record!=null)c.getHoldingChunkMap().loadHoldingSubLevel(record);s=c.getSubLevel(id);}if(s!=null)c.removeSubLevel(s,SubLevelRemovalReason.REMOVED);}SpaceRendezvous.clear(level);}
    }
}
