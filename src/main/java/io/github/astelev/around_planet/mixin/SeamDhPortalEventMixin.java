package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.compat.DhPortalViews;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractImmersivePortalsAccessor$BeforeRenderEvent",remap=false)
public abstract class SeamDhPortalEventMixin {
    @Inject(method="beforeRender",at=@At("HEAD"),cancellable=true)
    private void around_planet$allowPreparedView(CallbackInfo ci) {if(DhPortalViews.ready())ci.cancel();}
}
