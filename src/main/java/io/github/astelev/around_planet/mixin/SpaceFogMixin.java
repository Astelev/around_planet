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
    private static void space$blackBackground(CallbackInfo ci){if(SpaceRenderView.active()){fogRed=.002f;fogGreen=.003f;fogBlue=.008f;RenderSystem.clearColor(fogRed,fogGreen,fogBlue,0);}else if(io.github.astelev.around_planet.client.SurfaceRenderState.enabled() && io.github.astelev.around_planet.PlanetConfig.SURFACE_SKY.getAsBoolean() && net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera()==net.minecraft.world.level.material.FogType.NONE){
        double d=io.github.astelev.around_planet.client.SurfaceRenderState.darkness(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y);float f=(float)(1-.992*d);fogRed*=f;fogGreen*=f;fogBlue*=f;RenderSystem.clearColor(fogRed,fogGreen,fogBlue,0);
    }}
}
