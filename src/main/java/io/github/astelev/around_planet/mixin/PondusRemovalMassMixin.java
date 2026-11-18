package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
@Pseudo @Mixin(targets="dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem",remap=false)
public abstract class PondusRemovalMassMixin {
 @WrapOperation(method="updateMassDataFromBlockChange",at=@At(value="INVOKE",target="Ldev/ryanhcode/sable/physics/config/block_properties/PhysicsBlockPropertyHelper;getMass(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)D",ordinal=0))
 private double around_planet$previousMass(net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState old,Operation<Double> original,@Local(argsOnly=true,ordinal=1)net.minecraft.world.level.block.state.BlockState next){double mass=original.call(level,pos,old);return old.getBlock()!=next.getBlock()&&level instanceof net.minecraft.world.level.Level world?io.github.astelev.around_planet.compat.PondusRemovalMass.previous(world,pos,old,mass):mass;}
 @org.spongepowered.asm.mixin.injection.Inject(method="updateMassDataFromBlockChange",at=@At("RETURN"))
 private void around_planet$rebuildMultiblock(dev.ryanhcode.sable.sublevel.SubLevel sub,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState old,net.minecraft.world.level.block.state.BlockState next,boolean update,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
  if(io.github.astelev.around_planet.fuel.FuelMassBridge.pondus()&&sub instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel ship&&!ship.isRemoved())io.github.astelev.around_planet.compat.PondusStructuralMass.changed(ship,old,next);
 }

}
