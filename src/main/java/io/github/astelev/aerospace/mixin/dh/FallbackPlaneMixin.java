package io.github.astelev.aerospace.mixin.dh;

import io.github.astelev.aerospace.client.FallbackPlaneRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.render.renderer.LodRenderer", remap = false)
public abstract class FallbackPlaneMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void aerospace$fillUnrenderedPixels(@Coerce Object params, @Coerce Object profiler, CallbackInfo ci) {
        FallbackPlaneRenderer.render(params);
    }
}
