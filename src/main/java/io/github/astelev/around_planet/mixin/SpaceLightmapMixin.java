package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.client.SpaceRenderView;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Stable space ambient illumination, including zero-sky holding-space samples. No world light edits. */
@Mixin(LightTexture.class)
public abstract class SpaceLightmapMixin {
    @Shadow private boolean updateLightTexture;
    @Unique private boolean planet$space;
    @Inject(method="updateLightTexture",at=@At("HEAD"))
    private void planet$transition(float partial,CallbackInfo ci){boolean next=SpaceRenderView.active();if(next!=planet$space)updateLightTexture=true;planet$space=next;}
    @ModifyArg(method="updateLightTexture",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"),index=2)
    private int planet$ambient(int color){if(!planet$space)return color;return io.github.astelev.around_planet.space.SpaceIllumination.ambient(color);}
}
