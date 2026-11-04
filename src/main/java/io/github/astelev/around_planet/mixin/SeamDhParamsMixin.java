package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.compat.DhPortalViews;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.render.RenderParams",remap=false)
public abstract class SeamDhParamsMixin {
    @Inject(method="update",at=@At("RETURN"))
    private void around_planet$select(CallbackInfo ci) {
        DhPortalViews.selectForPass((com.seibel.distanthorizons.core.render.RenderParams)(Object)this);
    }
}
