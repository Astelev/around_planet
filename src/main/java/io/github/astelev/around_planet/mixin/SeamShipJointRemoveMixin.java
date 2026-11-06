package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.compat.ShipAssemblies;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.physics.impl.rapier.constraint.RapierConstraintHandle",remap=false)
public abstract class SeamShipJointRemoveMixin {
    @Inject(method="remove",at=@At("RETURN"))
    private void around_planet$removed(CallbackInfo ci){ShipAssemblies.removed(this);}
}
