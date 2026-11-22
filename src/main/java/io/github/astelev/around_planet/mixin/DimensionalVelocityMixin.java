package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
/** A dimension handoff is continuous flight, not a cold reload with velocity decay. */
@Pseudo @Mixin(targets="dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer",remap=false)
public abstract class DimensionalVelocityMixin {
 @ModifyExpressionValue(method="fullyLoad",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/common/ModConfigSpec$DoubleValue;getAsDouble()D"),require=2)
 private static double around_planet$fullImpulse(double value){return io.github.astelev.around_planet.compat.DimensionalTransfers.active()?1:value;}
}
