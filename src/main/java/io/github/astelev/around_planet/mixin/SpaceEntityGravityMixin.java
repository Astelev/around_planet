package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class SpaceEntityGravityMixin {
    @Inject(method="getGravity",at=@At("HEAD"),cancellable=true)
    private void space$zeroGravity(CallbackInfoReturnable<Double> ci){var e=(Entity)(Object)this;if(SpaceCells.contains(e)){e.fallDistance=0;ci.setReturnValue(0.0);}}
}
