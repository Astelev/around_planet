package io.github.astelev.around_planet.mixin;

import com.seibel.distanthorizons.core.render.RenderParams;
import io.github.astelev.around_planet.compat.DhPortalDepth;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.common.render.openGl.GlDhMetaRenderer_neoforge",remap=false)
public abstract class SeamDhDepthCaptureMixin {
    @Inject(method="copyToMcTexture",at=@At("TAIL"))
    private void around_planet$capture(RenderParams params,CallbackInfo ci) {DhPortalDepth.capture(params);}
}
