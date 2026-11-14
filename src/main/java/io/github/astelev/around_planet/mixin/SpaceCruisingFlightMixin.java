package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="io.github.astelev.cruising.flight.FlightHooks",remap=false)
public abstract class SpaceCruisingFlightMixin {
    @Inject(method="isHighShipEntity",at=@At("HEAD"),cancellable=true)
    private static void space$normalChunks(Entity entity,CallbackInfoReturnable<Boolean> ci){if(SpaceCells.contains(entity))ci.setReturnValue(false);}
}
