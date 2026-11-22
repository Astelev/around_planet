package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.client.SpaceRenderView;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.api.internal.ClientApi",remap=false)
public abstract class SpaceDhRenderMixin {
    // Stop before framebuffer setup/compositing, not halfway through an OpenGL pass.
    @Inject(method={"renderLods","renderDeferredLodsForShaders","renderFadeOpaque","renderFadeTransparent"},at=@At("HEAD"),cancellable=true)
    private void space$noLods(CallbackInfo ci){if(SpaceRenderView.active()){SpaceRenderView.dhSkips++;ci.cancel();}}
}
