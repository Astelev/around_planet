package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.portal.PlanetPortals;
import io.github.astelev.around_planet.transfer.ShipPassengerGuard;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.portal.Portal;

/** Whole-ship global traversal is not installed yet; don't detach its standing or seated crew. */
@Mixin(value=Portal.class,remap=false)
public abstract class ManagedPortalPassengerMixin {
    @Inject(method="canTeleportEntity",at=@At("RETURN"),cancellable=true)
    private void around_planet$shipCrew(Entity entity,CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue() && PlanetPortals.owned((Portal)(Object)this)
                && entity.getSelfAndPassengers().anyMatch(ShipPassengerGuard::attached))cir.setReturnValue(false);
    }
}
