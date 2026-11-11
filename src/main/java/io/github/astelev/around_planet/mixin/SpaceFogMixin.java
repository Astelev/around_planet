package io.github.astelev.around_planet.mixin;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.astelev.around_planet.client.SpaceRenderView;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FogRenderer.class)
public abstract class SpaceFogMixin {
    @Shadow private static float fogRed,fogGreen,fogBlue;
    @Inject(method="setupColor",at=@At("TAIL"))
    private static void space$blackBackground(CallbackInfo ci){if(SpaceRenderView.active()){fogRed=0;fogGreen=0;fogBlue=0;RenderSystem.clearColor(0,0,0,0);}}
}
