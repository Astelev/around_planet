package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.propulsion.*;
import io.github.astelev.around_planet.fuel.FuelMassBridge;
import io.github.astelev.around_planet.compat.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import org.joml.Vector3d;
import java.util.*;
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class RsuFuelGameTests {
 @GameTest(template="empty",templateNamespace="around_planet",batch="rsuMeter",timeoutTicks=100)
 public static void arbitraryFluidMeteringPersistsAndNeverRoundsFreeFuel(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));l.setBlockAndUpdate(p,PropulsionRegistry.DISTRIBUTOR.get().defaultBlockState());var be=(PropulsionBlockEntity)l.getBlockEntity(p);
  var cap=l.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,p,Direction.UP);h.assertTrue(cap==be.rsuFuel.tank,"Distributor fluid capability missing");
  for(var fluid:new net.minecraft.world.level.material.Fluid[]{Fluids.WATER,Fluids.LAVA,io.github.astelev.around_planet.fuel.CryogenicFluids.HYDROGEN.get()}){
   int before=be.rsuFuel.tank.getFluidAmount();h.assertTrue(cap.fill(new FluidStack(fluid,1000),FluidAction.SIMULATE)==1000&&be.rsuFuel.tank.getFluidAmount()==before,"Simulated fill changed inventory");cap.fill(new FluidStack(fluid,1000),FluidAction.EXECUTE);
   double density=FuelMassBridge.rsuKgPerMb(be,fluid),oldCredit=be.rsuFuel.credit(),sum=0;for(int i=0;i<100;i++)sum+=be.rsuFuel.consume(.000173);
   double balance=(1000-be.rsuFuel.tank.getFluidAmount())*density*800+oldCredit-be.rsuFuel.credit();h.assertTrue(Math.abs(sum-balance)<1e-8,"Substep rounding created/lost fuel impulse");
   var saved=be.saveWithoutMetadata(l.registryAccess());int amount=be.rsuFuel.tank.getFluidAmount();double credit=be.rsuFuel.credit();be.loadWithComponents(saved,l.registryAccess());h.assertTrue(be.rsuFuel.tank.getFluidAmount()==amount&&be.rsuFuel.credit()==credit,"Reload lost tank or paid impulse");
   cap.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);double rest=be.rsuFuel.consume(1e9);h.assertTrue(Math.abs(rest-credit)<1e-8&&be.rsuFuel.consume(1)==0,"Empty tank generated impulse");
  }
  l.removeBlock(p,false);h.succeed();
 }
 private static ServerSubLevel assemble(GameTestHelper h,BlockPos p,boolean distributor){var l=h.getLevel();l.setBlockAndUpdate(p,PropulsionRegistry.RSU.get().defaultBlockState());var blocks=new ArrayList<BlockPos>();blocks.add(p);if(distributor){l.setBlockAndUpdate(p.above(),PropulsionRegistry.DISTRIBUTOR.get().defaultBlockState());blocks.add(p.above());l.setBlockAndUpdate(p.east(),PropulsionRegistry.RSU.get().defaultBlockState());blocks.add(p.east());}return SubLevelAssemblyHelper.assembleBlocks(l,p,blocks,new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX()+(distributor?1:0),p.getY()+(distributor?1:0),p.getZ()));}
 private static PropulsionBlockEntity machine(ServerSubLevel ship,PropulsionRegistry.Kind kind){return ship.getPlot().getLoadedChunks().stream().flatMap(x->x.getChunk().getBlockEntities().values().stream()).filter(PropulsionBlockEntity.class::isInstance).map(PropulsionBlockEntity.class::cast).filter(be->be.kind()==kind).findFirst().orElseThrow();}
 @GameTest(template="empty",templateNamespace="around_planet",batch="rsuShip",timeoutTicks=120)
 public static void fuelStaysOnShipOppositeJetsConsumeAndStarvationScalesAllPorts(GameTestHelper h){h.runAfterDelay(3,()->{
  var l=h.getLevel();var c=SubLevelContainer.getContainer(l);var ship=assemble(h,h.absolutePos(new BlockPos(1,2,1)),true);var other=assemble(h,h.absolutePos(new BlockPos(4,2,1)),false);
  try{
   var rsu=machine(ship,PropulsionRegistry.Kind.RSU);var source=machine(ship,PropulsionRegistry.Kind.DISTRIBUTOR);var foreign=machine(other,PropulsionRegistry.Kind.RSU);var second=ship.getPlot().getLoadedChunks().stream().flatMap(x->x.getChunk().getBlockEntities().values().stream()).filter(PropulsionBlockEntity.class::isInstance).map(PropulsionBlockEntity.class::cast).filter(be->be.kind()==PropulsionRegistry.Kind.RSU&&be!=rsu).findFirst().orElseThrow();
   for(var be:List.of(rsu,source,foreign,second))PropulsionBlockEntity.tick(l,be.getBlockPos(),be.getBlockState(),be);
   rsu.configure(500);foreign.configure(500);rsu.receiveSignal(0,15);foreign.receiveSignal(0,15);var pipe=c.physicsSystem().getPipeline();pipe.resetVelocity(ship);pipe.resetVelocity(other);
   PropulsionPhysics.step(c.physicsSystem(),.05);h.assertTrue(pipe.getLinearVelocity(ship,new Vector3d()).length()<1e-9&&rsu.rsuPower()==0,"Unfuelled RSU generated force");
   double dry=ship.getMassTracker().getMass();source.rsuFuel.tank.fill(new FluidStack(Fluids.WATER,8000),FluidAction.EXECUTE);if(!FuelMassBridge.pondus()){ship.updateMergedMassData(0);h.assertTrue(Math.abs(ship.getMassTracker().getMass()-dry-8)<1e-6,"Fallback fill mass disagrees with tank");}rsu.receiveSignal(2,15);int initial=source.rsuFuel.tank.getFluidAmount();double credit=source.rsuFuel.credit();double density=FuelMassBridge.rsuKgPerMb(source,Fluids.WATER);
   PropulsionPhysics.step(c.physicsSystem(),.05);double spent=(initial-source.rsuFuel.tank.getFluidAmount())*density*800+credit-source.rsuFuel.credit();h.assertTrue(Math.abs(spent-50)<1e-7&&rsu.rsuPower()==1,"Opposite jets did not each pay for thrust");h.assertTrue(pipe.getLinearVelocity(ship,new Vector3d()).length()<1e-7,"Opposite jets failed to cancel");h.assertTrue(foreign.rsuPower()==0&&pipe.getLinearVelocity(other,new Vector3d()).length()<1e-9,"Different ship stole propellant");
   source.loadWithComponents(source.saveWithoutMetadata(l.registryAccess()),l.registryAccess());source.rsuFuel.tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);if(!FuelMassBridge.pondus()){ship.updateMergedMassData(0);h.assertTrue(Math.abs(ship.getMassTracker().getMass()-dry)<1e-6,"Fallback drain after reload retained fuel mass");}source.rsuFuel.consume(1e12);source.rsuFuel.tank.fill(new FluidStack(Fluids.WATER,1),FluidAction.EXECUTE);second.configure(500);second.receiveSignal(0,15);second.receiveSignal(2,15);double request=500*4*.05;double expected=Math.min(1,density*800/request);
   PropulsionPhysics.step(c.physicsSystem(),.05);h.assertTrue(Math.abs(rsu.rsuPower()-expected)<1e-9&&Math.abs(second.rsuPower()-expected)<1e-9&&source.rsuFuel.tank.isEmpty(),"Starvation did not proportionally scale all jets");h.assertTrue(pipe.getLinearVelocity(ship,new Vector3d()).length()<1e-7,"Starvation unbalanced opposite ports");
   h.succeed();
  }finally{c.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);c.removeSubLevel(other,SubLevelRemovalReason.REMOVED);}
 });}
}
