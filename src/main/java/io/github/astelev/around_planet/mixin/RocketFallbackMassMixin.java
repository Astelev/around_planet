package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper",remap=false)
public abstract class RocketFallbackMassMixin {
 @Inject(method="getMass",at=@At("RETURN"),cancellable=true)
 private static void around_planet$fuel(net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,CallbackInfoReturnable<Double> cir){if(!io.github.astelev.around_planet.fuel.FuelMassBridge.pondus()&&level.getBlockEntity(pos) instanceof io.github.astelev.around_planet.propulsion.PropulsionBlockEntity be&&be.kind()==io.github.astelev.around_planet.propulsion.PropulsionRegistry.Kind.LIQUID)cir.setReturnValue(cir.getReturnValueD()+io.github.astelev.around_planet.fuel.FuelMassBridge.mass(be));}
}
