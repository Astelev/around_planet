package io.github.astelev.around_planet.gametest;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import io.github.astelev.around_planet.compat.DeepSeasAir;
import io.github.astelev.around_planet.space.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import com.maxenonyme.createsubmarine.submarine.block.entity.OxygeneDiffuserBlockEntity;
import com.maxenonyme.createsubmarine.submarine.compartment.CompartmentTracker;
import java.util.*;
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class DeepSeasVacuumGameTests {
 @GameTest(template="empty",templateNamespace="around_planet",batch="deepSeas",timeoutTicks=300)
 public static void realDiffuserSuppliesOnlyItsSealedRoomAndStopsOnLeak(GameTestHelper h){h.runAfterDelay(3,()->{
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,3,1));var blocks=new ArrayList<BlockPos>();
  for(int x=0;x<=8;x++)for(int y=0;y<=4;y++)for(int z=0;z<=4;z++)if(x==0||x==4||x==8||y==0||y==4||z==0||z==4){var at=p.offset(x,y,z);l.setBlockAndUpdate(at,Blocks.IRON_BLOCK.defaultBlockState());blocks.add(at);}
  var machine=p.offset(2,1,2);var power=machine.west();l.setBlockAndUpdate(power,Blocks.REDSTONE_BLOCK.defaultBlockState());blocks.add(power);
  l.setBlockAndUpdate(machine,net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create_submarine:oxygene_diffuser")).defaultBlockState());blocks.add(machine);
  var oxy=net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("create_submarine:oxygen"));((OxygeneDiffuserBlockEntity)l.getBlockEntity(machine)).oxygenTank.fill(new net.neoforged.neoforge.fluids.FluidStack(oxy,10000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
  var c=SubLevelContainer.getContainer(l);var ship=SubLevelAssemblyHelper.assembleBlocks(l,p,blocks,new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX()+8,p.getY()+4,p.getZ()+4));c.physicsSystem().getPipeline().teleport(ship,new org.joml.Vector3d(12000,6144,0),ship.logicalPose().orientation());ship.updateBoundingBox();ship.updateLastPose();c.physicsSystem().getPipeline().resetVelocity(ship);c.addForceLoadTicket(ship,dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType.COMMAND_FORCED,net.minecraft.util.Unit.INSTANCE);
  var cell=new SpaceCells.Cell(UUID.randomUUID(),0,12000,6144,0,0,2200,0,List.of(ship.getUniqueId()));SpaceCells.get(l).add(l,cell);var data=SpaceOrbitData.get(l.getServer());var planet=new PlanetScale(io.github.astelev.around_planet.PlanetConfig.PLANET_SIZE.get()).geometry();data.navigate(cell.id(),new SpaceNavigation(planet,SpaceFrame.IDENTITY));double r=planet.radius()+10000,mu=9.81*planet.radius()*planet.radius();data.put(cell.id(),new SpaceTrajectory(CelestialSystem.EARTH,l.getGameTime(),new SpaceState(new SpaceVector(0,r,0),new SpaceVector(Math.sqrt(mu/r),0,0)),mu));
  var diffuser=ship.getPlot().getLoadedChunks().stream().flatMap(ch->ch.getChunk().getBlockEntities().values().stream()).filter(OxygeneDiffuserBlockEntity.class::isInstance).map(OxygeneDiffuserBlockEntity.class::cast).findFirst().orElseThrow();
  var observer=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RoomAir"));observer.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
  h.runAfterDelay(160,()->{
   try{
    var local=diffuser.getBlockPos();var first=ship.logicalPose().transformPosition(new org.joml.Vector3d(local.getX()+.5,local.getY()+1.1,local.getZ()+.5));observer.setPos(first.x,first.y-observer.getEyeHeight(),first.z);
    h.assertTrue(CompartmentTracker.hasAnySealed(ship.getUniqueId()),"Deep Seas did not scan the real powered cabin");h.assertTrue(DeepSeasAir.breathable(observer),"Oxygenated sealed room was not breathable");
    observer.setPos(local.getX()+.5,local.getY()+1.1-observer.getEyeHeight(),local.getZ()+.5);h.assertTrue(io.github.astelev.around_planet.lifesupport.VacuumEnvironment.outside(observer)&&DeepSeasAir.breathable(observer),"Sable plot coordinates were mistaken for surface air or another room");
    var second=ship.logicalPose().transformPosition(new org.joml.Vector3d(local.getX()+4.5,local.getY()+1.1,local.getZ()+.5));observer.setPos(second.x,second.y-observer.getEyeHeight(),second.z);h.assertTrue(!DeepSeasAir.breathable(observer),"Diffuser supplied a different sealed room");observer.setPos(first.x,first.y-observer.getEyeHeight(),first.z);
    var fluid=diffuser.oxygenTank.getFluid().copy();diffuser.oxygenTank.drain(Integer.MAX_VALUE,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);h.assertTrue(!DeepSeasAir.breathable(observer),"Empty diffuser supplied air");diffuser.oxygenTank.fill(fluid,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
    var powerAt=local.west();l.removeBlock(powerAt,false);h.assertTrue(!DeepSeasAir.breathable(observer),"Unpowered diffuser supplied air");l.setBlockAndUpdate(powerAt,Blocks.REDSTONE_BLOCK.defaultBlockState());
    l.removeBlock(local.offset(-2,1,0),false);
   }catch(Throwable ex){cleanup(l,c,ship,cell);throw ex;}
   h.runAfterDelay(40,()->{try{var local=diffuser.getBlockPos();var at=ship.logicalPose().transformPosition(new org.joml.Vector3d(local.getX()+.5,local.getY()+1.1,local.getZ()+.5));observer.setPos(at.x,at.y-observer.getEyeHeight(),at.z);h.assertTrue(!DeepSeasAir.breathable(observer),"Breached room remained breathable");h.succeed();}finally{cleanup(l,c,ship,cell);}});
  });
 });}
 private static void cleanup(net.minecraft.server.level.ServerLevel l,dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer c,dev.ryanhcode.sable.sublevel.ServerSubLevel ship,SpaceCells.Cell cell){SpaceCells.get(l).remove(l,cell);SpaceOrbitData.get(l.getServer()).remove(cell.id());if(!ship.isRemoved())c.removeSubLevel(ship,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);DeepSeasAir.clear(l);}
}
