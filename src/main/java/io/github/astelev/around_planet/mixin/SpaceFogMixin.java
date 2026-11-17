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
        double d=io.github.astelev.around_planet.client.SurfaceRenderState.darkness(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y)*(1-io.github.astelev.around_planet.client.AeroFogBridge.legacySkyStrength());float f=(float)(1-.992*d);fogRed*=f;fogGreen*=f;fogBlue*=f;if(d>0){
            var mc=net.minecraft.client.Minecraft.getInstance();var camera=mc.gameRenderer.getMainCamera();var pos=camera.getPosition();
            var planet=new io.github.astelev.around_planet.space.PlanetScale(io.github.astelev.around_planet.PlanetConfig.PLANET_SIZE.get()).geometry();
            var frame=planet.frame(pos.x,pos.z);var look=camera.getLookVector();
            var normal=io.github.astelev.around_planet.space.SurfaceVisualMath.planetNormal(new io.github.astelev.around_planet.space.SpaceVector(look.x(),look.y(),look.z()),pos.y,planet.radius()).orElse(new io.github.astelev.around_planet.space.SpaceVector(0,1,0));
            var fixed=frame.toCosmic(normal);int color=io.github.astelev.around_planet.client.PlanetAtlasTexture.ready()?io.github.astelev.around_planet.client.PlanetAtlasTexture.sample(fixed.x(),fixed.y(),fixed.z()):0x47663D;
            var sun=io.github.astelev.around_planet.space.PlanetDaylight.spin(mc.level).frame(planet,pos.x,pos.z).toLocal(io.github.astelev.around_planet.space.SpaceEnvironment.SUN_DIRECTION);
            float light=(float)(.045+.955*Math.max(0,normal.dot(sun))),mix=(float)d;
            fogRed=fogRed*(1-mix)+((color>>16&255)/255f*.8f+.15f*.2f)*light*mix;
            fogGreen=fogGreen*(1-mix)+((color>>8&255)/255f*.8f+.32f*.2f)*light*mix;
            fogBlue=fogBlue*(1-mix)+((color&255)/255f*.8f+.55f*.2f)*light*mix;
        }
        RenderSystem.clearColor(fogRed,fogGreen,fogBlue,0);
    }}
}
