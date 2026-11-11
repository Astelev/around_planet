package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.client.SpaceRenderView;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer",remap=false)
public abstract class SpaceCloudRenderMixin {
    @Inject(method={"renderBeforeLevel","renderAfterSky","renderBeforeWeather","renderAfterLevel","renderWeather","doScreenSpaceWorldFog","doFinalCompositePass","doStormPostProcessing","doCloudShadowProcessing"},at=@At("HEAD"),cancellable=true)
    private void space$noCloudPipeline(CallbackInfo ci){if(SpaceRenderView.active()){SpaceRenderView.cloudSkips++;SpaceRenderView.simpleCloudSkips++;ci.cancel();}}
}
