package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Integer fluid capabilities cannot represent the precise combustion remainder. Pondus uses it here. */
@Pseudo @Mixin(targets="org.fractalinteractive.ci.pondus_inventory_fi.mass.InventoryMassCalculator",remap=false)
public abstract class RocketPondusMassMixin {
 @Inject(method="calculateInventoryMassOnly",at=@At("HEAD"),cancellable=true)
 private static void around_planet$preciseFuel(net.minecraft.world.level.block.entity.BlockEntity be,@org.spongepowered.asm.mixin.injection.Coerce Object entry,CallbackInfoReturnable<Double> cir){if(be instanceof io.github.astelev.around_planet.propulsion.PropulsionBlockEntity engine&&engine.kind()==io.github.astelev.around_planet.propulsion.PropulsionRegistry.Kind.LIQUID)cir.setReturnValue(io.github.astelev.around_planet.fuel.FuelMassBridge.mass(engine));}
}
