package io.github.astelev.around_planet.gametest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import org.joml.Vector3d;
import java.util.*;
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class MassLedgerGameTests {
 @GameTest(template="empty",templateNamespace="around_planet",batch="massLedger",timeoutTicks=150)
 public static void rebuildKeepsFilledMultiblockMassAndCenter(GameTestHelper h){fixture(h,false);}
 @GameTest(template="empty",templateNamespace="around_planet",batch="massLedger",timeoutTicks=150)
 public static void repeatedDrainRefillAndRebuildKeepsDryMass(GameTestHelper h){fixture(h,true);}
 private static void fixture(GameTestHelper h,boolean cycle){h.runAfterDelay(3,()->{
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,3,1));var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:fluid_tank"));var positions=new ArrayList<BlockPos>();
  for(int x=0;x<3;x++)for(int y=0;y<2;y++)for(int z=0;z<3;z++){var at=p.offset(x,y,z);l.setBlockAndUpdate(at,block.defaultBlockState());positions.add(at);}
  var c=SubLevelContainer.getContainer(l);var ship=SubLevelAssemblyHelper.assembleBlocks(l,p,positions,new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX()+2,p.getY()+1,p.getZ()+2));c.addForceLoadTicket(ship,dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType.COMMAND_FORCED,net.minecraft.util.Unit.INSTANCE);
  h.runAfterDelay(30,()->{try{
   var tanks=ship.getPlot().getLoadedChunks().stream().flatMap(ch->ch.getChunk().getBlockEntities().values().stream()).filter(FluidTankBlockEntity.class::isInstance).map(FluidTankBlockEntity.class::cast).toList();repairFixtureControllerLinks(tanks);var tank=tanks.stream().filter(FluidTankBlockEntity::isController).findFirst().orElseThrow();h.assertTrue(tank.getTotalTankSize()==18,"Fixture did not form 3x3x2 tank: "+tank.getTotalTankSize());
   var bounds=ship.getPlot().getBoundingBox();var center=new Vector3d((bounds.minX()+bounds.maxX()+1)/2.0,(bounds.minY()+bounds.maxY()+1)/2.0,(bounds.minZ()+bounds.maxZ()+1)/2.0);
   double dry=ship.getSelfMassTracker().getMass();var fluid=new FluidStack(io.github.astelev.around_planet.fuel.CryogenicFluids.OXYGEN.get(),8000);
   for(int i=0;i<(cycle?4:1);i++){
    tank.getTankInventory().fill(fluid,FluidAction.EXECUTE);double filled=ship.getSelfMassTracker().getMass();h.assertTrue(filled>dry,"Tank filling added no mass");
    if(!cycle){ship.buildMassTracker();h.assertTrue(Math.abs(ship.getSelfMassTracker().getMass()-filled)<1e-6,"Native rebuild changed mass: "+filled+" -> "+ship.getSelfMassTracker().getMass());h.assertTrue(ship.getSelfMassTracker().getCenterOfMass().distance(center)<1e-6,"Rebuild offset symmetric tank center: "+ship.getSelfMassTracker().getCenterOfMass()+" expected="+center);}
    tank.getTankInventory().drain(Integer.MAX_VALUE,FluidAction.EXECUTE);h.assertTrue(Math.abs(ship.getSelfMassTracker().getMass()-dry)<1e-6,"Drain left mass on cycle "+i+": "+ship.getSelfMassTracker().getMass()+" expected="+dry);
    ship.buildMassTracker();h.assertTrue(Math.abs(ship.getSelfMassTracker().getMass()-dry)<1e-6,"Empty rebuild changed dry mass");h.assertTrue(ship.getSelfMassTracker().getCenterOfMass().distance(center)<1e-6,"Empty rebuild offset center");
   }
   io.github.astelev.around_planet.AroundPlanet.LOGGER.info("MASS_LEDGER_PASS cycles={} dry={} center={}",cycle?4:1,dry,ship.getSelfMassTracker().getCenterOfMass());h.succeed();
  }finally{if(!ship.isRemoved())c.removeSubLevel(ship,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);}});
 });}
 @GameTest(template="empty",templateNamespace="around_planet",batch="massCoefficients",timeoutTicks=350)
 public static void repeatedRelocationAndRebuildWithDoubledBlockCoefficient(GameTestHelper h){
  var l=h.getLevel();var c=SubLevelContainer.getContainer(l);var ground=h.absolutePos(new BlockPos(1,3,1));var body=new dev.ryanhcode.sable.sublevel.ServerSubLevel[1];
  try{var config=Class.forName("org.fractalinteractive.ci.pondus_inventory_fi.config.GlobalConfig");var instance=config.getMethod("getInstance").invoke(null);var coefficient=config.getField("blockMassModifier");double old=coefficient.getDouble(instance);coefficient.setDouble(instance,2.0);
   var blocks=new ArrayList<BlockPos>();var state=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:fluid_tank")).defaultBlockState();for(int x=0;x<3;x++)for(int y=0;y<2;y++)for(int z=0;z<3;z++){var at=ground.offset(x,y,z);l.setBlockAndUpdate(at,state);blocks.add(at);}
   Runnable cleanup=()->{try{coefficient.setDouble(instance,old);}catch(IllegalAccessException e){throw new AssertionError(e);}if(body[0]!=null&&!body[0].isRemoved())c.removeSubLevel(body[0],dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);for(var at:blocks)l.removeBlock(at,false);};
   class Cycle {
    int round;double reference;
    void assemble(){body[0]=SubLevelAssemblyHelper.assembleBlocks(l,ground,blocks,new BoundingBox3i(ground.getX(),ground.getY(),ground.getZ(),ground.getX()+2,ground.getY()+1,ground.getZ()+2));c.addForceLoadTicket(body[0],dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType.COMMAND_FORCED,net.minecraft.util.Unit.INSTANCE);h.runAfterDelay(30,this::check);}
    void check(){try{var ship=body[0];var bes=ship.getPlot().getLoadedChunks().stream().flatMap(ch->ch.getChunk().getBlockEntities().values().stream()).filter(FluidTankBlockEntity.class::isInstance).map(FluidTankBlockEntity.class::cast).toList();repairFixtureControllerLinks(bes);var tank=bes.stream().filter(FluidTankBlockEntity::isController).findFirst().orElseThrow();h.assertTrue(tank.getTotalTankSize()==18,"Reassembly lost tank connectivity");
      if(round==0){tank.getTankInventory().fill(new FluidStack(io.github.astelev.around_planet.fuel.CryogenicFluids.OXYGEN.get(),8000),FluidAction.EXECUTE);reference=ship.getSelfMassTracker().getMass();}
      ship.buildMassTracker();h.assertTrue(Math.abs(ship.getSelfMassTracker().getMass()-reference)<1e-6,"Reassembly "+round+" changed total mass: "+ship.getSelfMassTracker().getMass()+" expected="+reference);
      var b=ship.getPlot().getBoundingBox();var center=new Vector3d((b.minX()+b.maxX()+1)/2.0,(b.minY()+b.maxY()+1)/2.0,(b.minZ()+b.maxZ()+1)/2.0);h.assertTrue(ship.getSelfMassTracker().getCenterOfMass().distance(center)<1e-6,"Reassembly changed symmetric COM");
      if(round++==3){h.succeed();cleanup.run();return;}
      var min=new BlockPos(b.minX(),b.minY(),b.minZ());var oldPositions=bes.stream().map(FluidTankBlockEntity::getBlockPos).toList();SubLevelAssemblyHelper.moveBlocks(l,new SubLevelAssemblyHelper.AssemblyTransform(min,ground,0,net.minecraft.world.level.block.Rotation.NONE,l),oldPositions);if(!ship.isRemoved())c.removeSubLevel(ship,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);body[0]=null;h.runAfterDelay(20,this::assemble);
     }catch(Throwable ex){cleanup.run();throw ex;}}
   }
   new Cycle().assemble();
  }catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
 }

 @GameTest(template="empty",templateNamespace="around_planet",batch="massRemoval",timeoutTicks=150)
 public static void wideTankRemovalAfterRebuildRestoresHull(GameTestHelper h){h.runAfterDelay(3,()->{
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,3,1));var positions=new ArrayList<BlockPos>();
  var tankBlock=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:fluid_tank"));
  for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++){var at=p.offset(x,y,z);l.setBlockAndUpdate(at,y==0?net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState():tankBlock.defaultBlockState());positions.add(at);}
  var c=SubLevelContainer.getContainer(l);var ship=SubLevelAssemblyHelper.assembleBlocks(l,p,positions,new BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX()+2,p.getY()+2,p.getZ()+2));c.addForceLoadTicket(ship,dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType.COMMAND_FORCED,net.minecraft.util.Unit.INSTANCE);
  h.runAfterDelay(30,()->{try{
   var cells=ship.getPlot().getLoadedChunks().stream().flatMap(ch->ch.getChunk().getBlockEntities().values().stream()).filter(FluidTankBlockEntity.class::isInstance).map(FluidTankBlockEntity.class::cast).toList();repairFixtureControllerLinks(cells);var tank=cells.stream().filter(FluidTankBlockEntity::isController).findFirst().orElseThrow();h.assertTrue(tank.getTotalTankSize()==18,"Removal fixture did not form tank");
   ship.buildMassTracker();tank.getTankInventory().fill(new FluidStack(io.github.astelev.around_planet.fuel.CryogenicFluids.OXYGEN.get(),8000),FluidAction.EXECUTE);tank.getTankInventory().drain(3000,FluidAction.EXECUTE);ship.buildMassTracker();
   // Remove the controller first, so later removals must use their accounted cell ledgers.
   var ordered=new ArrayList<>(cells);ordered.remove(tank);ordered.addFirst(tank);for(var cell:ordered)l.removeBlock(cell.getBlockPos(),false);
   h.runAfterDelay(2,()->{try{var fresh=dev.ryanhcode.sable.api.physics.mass.MassTracker.build(l,ship.getPlot().getBoundingBox());var actual=ship.getSelfMassTracker();h.assertTrue(Math.abs(actual.getMass()-fresh.getMass())<1e-6,"Wide tank left mass after removal: "+actual.getMass()+" expected="+fresh.getMass());h.assertTrue(actual.getCenterOfMass().distance(fresh.getCenterOfMass())<1e-6,"Removed wide tank left stale COM: "+actual.getCenterOfMass()+" expected="+fresh.getCenterOfMass()+" distance="+actual.getCenterOfMass().distance(fresh.getCenterOfMass()));
   for(int x=0;x<3;x++)for(int y=0;y<3;y++)h.assertTrue(Math.abs(actual.getInertiaTensor().get(x,y)-fresh.getInertiaTensor().get(x,y))<1e-4,"Removed wide tank left stale inertia");h.succeed();
   }finally{if(!ship.isRemoved())c.removeSubLevel(ship,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);}});
  }catch(Throwable failure){if(!ship.isRemoved())c.removeSubLevel(ship,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);throw failure;}});
 });}
 // The low-level Sable moveBlocks API copies Create NBT without Create's contraption
 // StructureTransform. Restore the known fixture's controller links before testing mass.
 private static void repairFixtureControllerLinks(java.util.List<FluidTankBlockEntity> cells) {
  var root=cells.stream().filter(FluidTankBlockEntity::isController).findFirst().orElseThrow();
  for(var cell:cells)if(cell!=root)cell.setController(root.getBlockPos());
 }

}
