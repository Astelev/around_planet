package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Matrix4f;
@Pseudo
@Mixin(targets={"dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.DefaultPipeline","dev.nonamecrackers2.simpleclouds.client.dh.pipeline.DhSupportPipeline"},remap=false)
public abstract class ExhaustCloudProjectionMixin {
    @Inject(method="afterSky",at=@At("HEAD"),require=0)
    private void exhaust$vanilla(@Coerce Object mc,@Coerce Object renderer,Matrix4f view,Matrix4f projection,float partial,double x,double y,double z,@Coerce Object frustum,CallbackInfo ci){if(getClass().getName().contains("DefaultPipeline"))io.github.astelev.around_planet.client.ExhaustCloudDepth.capture(projection);}
    @Inject(method="afterDistantHorizonsRender",at=@At("HEAD"),require=0)
    private void exhaust$dh(@Coerce Object mc,@Coerce Object renderer,Matrix4f view,Matrix4f projection,float partial,double x,double y,double z,@Coerce Object frustum,int fbo,CallbackInfo ci){io.github.astelev.around_planet.client.ExhaustCloudDepth.capture(projection);}
}
