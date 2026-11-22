package io.github.astelev.around_planet.compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
/** Use Pondus' already-accounted inventory contribution if a removed controller is no longer readable. */
public final class PondusRemovalMass {
 private static java.lang.reflect.Method cached,entry,base;
 public static double previous(Level level,BlockPos pos,BlockState old,double computed){
  if(!io.github.astelev.around_planet.fuel.FuelMassBridge.pondus()||!old.hasBlockEntity())return computed;
  var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(old.getBlock()).toString();if(!id.equals("create:fluid_tank")&&!id.equals("around_planet:liquid_rocket_engine"))return computed;
  try{if(cached==null){String root="org.fractalinteractive.ci.pondus_inventory_fi.";cached=Class.forName(root+"mass.InventoryMassCache").getMethod("getCached",Level.class,BlockPos.class);entry=Class.forName(root+"mass.MassRegistry").getMethod("getBlockMass",net.minecraft.world.level.block.Block.class,BlockState.class);base=Class.forName(root+"config.BlockMassEntry").getMethod("getBaseMass");}
   var accounted=(Double)cached.invoke(null,level,pos);if(accounted==null||!Double.isFinite(accounted)||accounted<=0)return computed;
   var definition=entry.invoke(null,old.getBlock(),old);double dry=definition!=null?(double)base.invoke(definition):(Double)((dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension)old).sable$getProperty(dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes.MASS.get());
   return Math.max(computed,dry+accounted);
  }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot read Pondus removal ledger",e);}
 }
 private PondusRemovalMass(){}
}
