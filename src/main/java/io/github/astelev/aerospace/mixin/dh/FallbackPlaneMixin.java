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
    // Draw into the LOD target after opaque terrain, before SSAO, transparency,
    // fog and compositing. This is not a final screen-color overlay.
    @Inject(method = "renderTerrain(Lcom/seibel/distanthorizons/core/wrapperInterfaces/render/renderPass/IDhTerrainRenderer;Lcom/seibel/distanthorizons/core/render/RenderBufferHandler;Lcom/seibel/distanthorizons/core/render/RenderParams;ZLcom/seibel/distanthorizons/core/wrapperInterfaces/minecraft/IProfilerWrapper;)V", at = @At("TAIL"))
    private void aerospace$fillUnrenderedSurface(@Coerce Object terrainRenderer, @Coerce Object buffers,
                                               @Coerce Object params, boolean opaquePass,
                                               @Coerce Object profiler, CallbackInfo ci) {
        if (opaquePass) FallbackPlaneRenderer.render(params);
    }
}
