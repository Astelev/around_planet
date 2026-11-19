package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.client.SpaceRenderView;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="dev.nonamecrackers2.simpleclouds.client.dh.pipeline.DhSupportPipeline",remap=false)
public abstract class SpaceCloudDhRenderMixin {
    @Inject(method={"prepare","afterSky","beforeWeather","afterLevel","beforeDistantHorizonsApplyShader","afterDistantHorizonsRender"},at=@At("HEAD"),cancellable=true)
    private void space$noCloudDhPipeline(CallbackInfo ci){if(SpaceRenderView.active()){SpaceRenderView.cloudSkips++;SpaceRenderView.simpleCloudSkips++;ci.cancel();}}
}
