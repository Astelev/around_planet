package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.solid.*;
import io.github.astelev.around_planet.propulsion.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class SolidRocketGameTests {
 @GameTest(template="empty",templateNamespace="around_planet",batch="solid",timeoutTicks=100)
 public static void chargeLoadsByHandAndCannotBeDuplicated(GameTestHelper h){
  var level=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(p,SolidRegistry.COLUMN.get().defaultBlockState());
  var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SolidFuelHands"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var stack=new ItemStack(SolidRegistry.FUEL.get(),2);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
  try{var state=level.getBlockState(p);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p),Direction.EAST,p,false);
   h.assertTrue(state.useItemOn(stack,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit).consumesAction(),"Fuel right click passed through");
   var be=(SolidColumnBlockEntity)level.getBlockEntity(p);h.assertTrue(be.fuel()==64&&stack.getCount()==1,"Charge did not load exactly one block");
   state.useItemOn(stack,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);h.assertTrue(stack.getCount()==1&&be.fuel()==64,"Already loaded column duplicated/consumed fuel");
   be.consume(19.5);var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(p)).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,ItemStack.EMPTY).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY,be);
   var drops=state.getDrops(params);var drop=drops.stream().filter(s->s.is(SolidRegistry.COLUMN.get().asItem())).findFirst().orElseThrow();var tag=drop.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
   h.assertTrue(tag!=null&&Math.abs(tag.copyTag().getDouble("SolidFuelUnits")-44.5)<1e-9,"Partial charge lost or refilled by block drop");
   var restored=new SolidColumnBlockEntity(p,state);restored.loadWithComponents(be.saveWithFullMetadata(level.registryAccess()),level.registryAccess());h.assertTrue(restored.fuel()==44.5,"Fuel fraction not persistent");h.succeed();
  }finally{player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);level.removeBlock(p,false);}
 }
 @GameTest(template="empty",templateNamespace="around_planet",batch="solid",timeoutTicks=100)
 public static void motorBurnsFarEndAndCannotBeSwitchedOff(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));l.setBlockAndUpdate(p,PropulsionRegistry.SOLID.get().defaultBlockState());for(int i=1;i<=2;i++){l.setBlockAndUpdate(p.above(i),SolidRegistry.COLUMN.get().defaultBlockState());((SolidColumnBlockEntity)l.getBlockEntity(p.above(i))).loadFuel();}
  try{var be=(PropulsionBlockEntity)l.getBlockEntity(p);var near=(SolidColumnBlockEntity)l.getBlockEntity(p.above());var far=(SolidColumnBlockEntity)l.getBlockEntity(p.above(2));
   be.solid.tick(15);h.assertTrue(be.solid.active(),"Motor did not latch");be.solid.tick(0);double kg=SolidMassBridge.kgPerPortion(far),initial=far.fuel();double impulse=be.burn(1);
   h.assertTrue(be.solid.active()&&near.fuel()==64&&Math.abs(far.fuel()-(initial-SolidPropellant.FLOW/kg))<1e-6,"Burn did not begin at far end or stopped with redstone");h.assertTrue(Math.abs(impulse-SolidPropellant.FLOW*SolidPropellant.exhaustSpeed())<1e-6,"Thrust disconnected from consumed mass");
   h.assertTrue(!near.canLoad()&&!far.canLoad(),"Running column allowed loading");
   var tag=be.saveWithFullMetadata(l.registryAccess());var copy=new PropulsionBlockEntity(p,be.getBlockState());copy.loadWithComponents(tag,l.registryAccess());h.assertTrue(copy.solid.active(),"Ignition latch lost on save");
   be.burn(10000);h.assertTrue(near.fuel()==0&&far.fuel()==0&&!be.solid.active(),"Fuel not exhausted");be.solid.tick(15);h.assertTrue(!be.solid.active()&&be.burn(1)==0,"Empty motor generated impulse");h.succeed();
  }finally{for(int i=0;i<=2;i++)l.removeBlock(p.above(i),false);}
 }
 @GameTest(template="empty",templateNamespace="around_planet",batch="solid",timeoutTicks=100)
 public static void horizontalStacksRespectAxisAndRejectSharedColumns(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));l.setBlockAndUpdate(p,PropulsionRegistry.SOLID.get().defaultBlockState().setValue(BlockStateProperties.FACING,Direction.EAST));
  l.setBlockAndUpdate(p.east(),SolidRegistry.COLUMN.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));l.setBlockAndUpdate(p.east(2),PropulsionRegistry.SOLID.get().defaultBlockState().setValue(BlockStateProperties.FACING,Direction.WEST));
  try{var column=(SolidColumnBlockEntity)l.getBlockEntity(p.east());column.loadFuel();var first=(PropulsionBlockEntity)l.getBlockEntity(p);var second=(PropulsionBlockEntity)l.getBlockEntity(p.east(2));first.solid.tick(15);second.solid.tick(15);h.assertTrue(first.solid.active()&&!second.solid.active(),"Two nozzles burned the same charge");first.burn(.1);h.assertTrue(column.fuel()<64&&column.burnPositive(),"Horizontal stack did not burn");first.stop();l.setBlockAndUpdate(p.east(),l.getBlockState(p.east()).setValue(BlockStateProperties.AXIS,Direction.Axis.Z));second.solid.tick(0);second.solid.tick(15);h.assertTrue(!second.solid.active(),"Cross-axis column connected");h.succeed();}
  finally{for(int i=0;i<3;i++)l.removeBlock(p.east(i),false);}
 }
 @GameTest(template="empty",templateNamespace="around_planet",batch="solidMass",timeoutTicks=150)
 public static void solidNativeMassFollowsStockInventoryOnBurnAndRemoval(GameTestHelper h){h.runAfterDelay(3,()->{
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));l.setBlockAndUpdate(p,PropulsionRegistry.SOLID.get().defaultBlockState());l.setBlockAndUpdate(p.above(),SolidRegistry.COLUMN.get().defaultBlockState());((SolidColumnBlockEntity)l.getBlockEntity(p.above())).loadFuel();
  var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(l);var ship=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(l,p,List.of(p,p.above()),new dev.ryanhcode.sable.companion.math.BoundingBox3i(p.getX(),p.getY(),p.getZ(),p.getX(),p.getY()+1,p.getZ()));
  try{var column=ship.getPlot().getLoadedChunks().stream().flatMap(c->c.getChunk().getBlockEntities().values().stream()).filter(SolidColumnBlockEntity.class::isInstance).map(SolidColumnBlockEntity.class::cast).findFirst().orElseThrow();double kg=SolidMassBridge.kgPerPortion(column),before=ship.getSelfMassTracker().getMass();column.consume(32);double after=ship.getSelfMassTracker().getMass();h.assertTrue(Math.abs(before-after-32*kg)<1e-5,"Pondus did not account consumed solid fuel: "+before+" -> "+after+" kg="+kg);
   column.consume(32);h.assertTrue(Math.abs(ship.getSelfMassTracker().getMass()-(before-64*kg))<1e-5,"Burnout retained fuel mass");column.loadFuel();h.assertTrue(Math.abs(ship.getSelfMassTracker().getMass()-before)<1e-5,"Refill did not restore fuel mass");l.removeBlock(column.getBlockPos(),false);h.assertTrue(ship.getSelfMassTracker().getMass()<before-64*kg,"Removing charged column retained mass");h.succeed();
  }finally{if(!ship.isRemoved())container.removeSubLevel(ship,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);}
 });}
}
