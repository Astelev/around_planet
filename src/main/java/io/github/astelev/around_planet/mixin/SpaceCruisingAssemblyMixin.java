package io.github.astelev.around_planet.mixin;
import dev.ryanhcode.sable.sublevel.SubLevel;
import io.github.astelev.around_planet.space.SpaceCells;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="io.github.astelev.cruising.flight.ConnectedFlightAssembly",remap=false)
public abstract class SpaceCruisingAssemblyMixin {
    @Inject(method="currentMinY",at=@At("HEAD"),cancellable=true)
    private static void space$realTerrainTickets(SubLevel ship,CallbackInfoReturnable<Double> ci){if(SpaceCells.containsShip(ship.getLevel(),ship.getUniqueId()))ci.setReturnValue(Double.NEGATIVE_INFINITY);}
}
