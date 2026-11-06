package io.github.astelev.around_planet.mixin;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.astelev.around_planet.compat.ShipAssemblies;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline",remap=false)
public abstract class SeamShipJointsMixin {
    @Shadow @Final private ServerLevel level;
    @Inject(method="addConstraint",at=@At("RETURN"))
    private void around_planet$joint(PhysicsPipelineBody a,PhysicsPipelineBody b,PhysicsConstraintConfiguration<?> config,CallbackInfoReturnable<PhysicsConstraintHandle> cir){ShipAssemblies.added(level,a,b,cir.getReturnValue());}
    @Inject(method="remove(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;)V",at=@At("HEAD"))
    private void around_planet$removed(ServerSubLevel ship,CallbackInfo ci){ShipAssemblies.bodyRemoved(level,ship);}
    @Inject(method="dispose",at=@At("HEAD"))
    private void around_planet$dispose(CallbackInfo ci){ShipAssemblies.clear(level);}
}
