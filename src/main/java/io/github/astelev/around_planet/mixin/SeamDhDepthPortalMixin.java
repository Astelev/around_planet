package io.github.astelev.around_planet.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.portal.Portal;
import java.util.List;

@Mixin(targets="qouteall.imm_ptl.core.render.renderer.PortalRenderer",remap=false)
public abstract class SeamDhDepthPortalMixin {
    @Inject(method="getPortalsToRender",at=@At("RETURN"))
    private void around_planet$opaqueLodDepth(CallbackInfoReturnable<List<Portal>> cir) {
        if(net.neoforged.fml.ModList.get().isLoaded("distanthorizons")
                && cir.getReturnValue().stream().anyMatch(io.github.astelev.around_planet.portal.PlanetPortals::owned))
            io.github.astelev.around_planet.compat.DhPortalDepth.beforePortals();
    }
}
