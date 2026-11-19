package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.compat.*;
import dev.ryanhcode.sable.api.sublevel.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import java.util.*;
import org.joml.Vector3d;
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class SpaceImpactGameTests {
 @GameTest(template="empty",templateNamespace="around_planet",batch="impactEarth",timeoutTicks=120)
 public static void unloadedEarthImpactDeletesOnlyItsOwnStoredBodies(GameTestHelper h){run(h,false,false);}
 @GameTest(template="empty",templateNamespace="around_planet",batch="impactMoon",timeoutTicks=120)
 public static void unloadedMoonBindingIsRemovedInsteadOfParking(GameTestHelper h){run(h,true,false);}
 @GameTest(template="empty",templateNamespace="around_planet",batch="impactLoaded",timeoutTicks=120)
 public static void awakeMembersProtectWholeReturningSpace(GameTestHelper h){run(h,false,true);}
 @GameTest(template="empty",templateNamespace="around_planet",batch="impactColdCoast",timeoutTicks=120)
 public static void coldCircularCoastDoesNotImpactAlongOldEpochChord(GameTestHelper h){run(h,false,false,true);}
 private static void run(GameTestHelper h,boolean moon,boolean awake){run(h,moon,awake,false);}
 private static void run(GameTestHelper h,boolean moon,boolean awake,boolean cold){h.runAfterDelay(3,()->{var level=h.getLevel();var c=SubLevelContainer.getContainer(level);var cells=SpaceCells.get(level);var data=SpaceOrbitData.get(level.getServer());var cargo=SpaceCargoData.get(level.getServer());var id=UUID.randomUUID();var nativeIds=new ArrayList<UUID>();SpaceCells.Cell cell=null;var oldSystem=data.system();try{
  for(int i=0;i<3;i++){var p=h.absolutePos(new BlockPos(1+i*2,2,1));level.setBlockAndUpdate(p,Blocks.IRON_BLOCK.defaultBlockState());var ship=SubLevelAssemblyHelper.assembleBlocks(level,p,List.of(p),new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX(),p.getY(),p.getZ()));nativeIds.add(ship.getUniqueId());c.physicsSystem().getPipeline().teleport(ship,new Vector3d(i==2?22000:20000+i*4,9000,0),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();}
  cell=new SpaceCells.Cell(id,77,20000,9000,0,0,2200,0,List.of(nativeIds.get(0),nativeIds.get(1)));cells.add(level,cell);
  var planet=new PlanetScale(131072).geometry();var sys=new CelestialSystem(planet.radius(),9.81*planet.radius()*planet.radius(),0);data.system(sys);var body=moon?MoonSurface.geometry(sys):planet;double altitude=moon?io.github.astelev.around_planet.PlanetConfig.MOON_LANDING.get():SpaceAtmosphere.returnAltitude();
  data.put(id,new SpaceTrajectory(moon?CelestialSystem.MOON:CelestialSystem.EARTH,level.getGameTime(),new SpaceState(new SpaceVector(0,body.radius()+altitude-1,0),new SpaceVector(0,-30,0)),0));data.navigate(id,new SpaceNavigation(planet,SpaceFrame.IDENTITY));
  var chunk=new ChunkPos(1250,0);for(int i=awake?1:0;i<3;i++)c.getHoldingChunkMap().moveToUnloaded((ServerSubLevel)c.getSubLevel(nativeIds.get(i)),chunk);
  h.assertTrue(cargo.all().stream().anyMatch(r->r.ship().equals(nativeIds.get(1))),"Fixture has no serialized address");
  if(moon){cells.remove(level,cell);SpaceDormantData.get(level.getServer()).put(new SpaceDormantData.Binding(id,"impact",new SpaceVector(20000,9000,0),2200,nativeIds.subList(0,2).stream().map(ship->new SpaceDormantData.Member(ship,SpaceVector.ZERO,SpaceVector.ZERO,SpaceVector.ZERO)).toList()));}
  if(cold){double r=planet.radius()+8000,mu=sys.earthMu();long halfPeriod=Math.round(Math.PI*Math.sqrt(r*r*r/mu)*20);data.put(id,new SpaceTrajectory(CelestialSystem.EARTH,level.getGameTime()-halfPeriod,new SpaceState(new SpaceVector(0,r,0),new SpaceVector(Math.sqrt(mu/r),0,0)),mu));}
  c.getHoldingChunkMap().saveAll();c.getHoldingChunkMap().getStorage().flush();
  var removedPointers=new HashSet<dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer>();if(!awake&&!cold)for(var ship:nativeIds.subList(0,2)){var record=c.getHoldingChunkMap().getHoldingSubLevel(ship);h.assertTrue(record!=null&&record.pointer()!=null,"Fixture did not persist native pointer");removedPointers.add(record.pointer().local());}
  SpaceUnloadedImpact.check(level,c);
  if(awake||cold){h.assertTrue(data.get(id).isPresent()&&cells.forShip(nativeIds.getFirst())!=null&&c.getHoldingChunkMap().getHoldingSubLevel(nativeIds.get(1))!=null,"Loaded group was deleted");}
  else{h.assertTrue(data.get(id).isEmpty()&&SpaceDormantData.get(level.getServer()).get(id)==null&&cells.forShip(nativeIds.getFirst())==null,"Impacted orbit remained registered");for(var ship:nativeIds.subList(0,2)){h.assertTrue(c.getHoldingChunkMap().getHoldingSubLevel(ship)==null&&!((SpaceHoldingAccess)c.getHoldingChunkMap()).around_planet$queued(ship),"Impacted native body remained in holding");h.assertTrue(cargo.all().stream().noneMatch(r->r.ship().equals(ship)),"Cargo address remained");}}
  h.assertTrue(c.getHoldingChunkMap().getHoldingSubLevel(nativeIds.get(2))!=null,"Unrelated body in same holding chunk was deleted");
  c.getHoldingChunkMap().saveAll();c.getHoldingChunkMap().getStorage().flush();var saved=c.getHoldingChunkMap().getStorage().attemptLoadHoldingChunk(chunk);
  if(!awake&&!cold&&saved!=null)h.assertTrue(saved.getSubLevelPointers().stream().noneMatch(removedPointers::contains),"Deleted body pointer was saved again: removed="+removedPointers+" saved="+saved.getSubLevelPointers());
  h.succeed();
 }catch(java.io.IOException ex){throw new AssertionError(ex);}finally{if(cell!=null)cells.remove(level,cell);data.remove(id);data.system(oldSystem);SpaceDormantData.get(level.getServer()).remove(id);SpaceUnloadedImpact.clear(level);for(var ship:nativeIds){cargo.remove(ship);var nativeBody=c.getSubLevel(ship);if(nativeBody!=null)c.removeSubLevel(nativeBody,SubLevelRemovalReason.REMOVED);else ((SpaceHoldingAccess)c.getHoldingChunkMap()).around_planet$discard(ship,new ChunkPos(1250,0));}}});}
}
