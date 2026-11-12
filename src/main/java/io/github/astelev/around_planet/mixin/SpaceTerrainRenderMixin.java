package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.astelev.around_planet.client.SpaceRenderView;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=LevelRenderer.class,priority=1100)
public abstract class SpaceTerrainRenderMixin {
    @Unique private static final ObjectArrayList<SectionRenderDispatcher.RenderSection> space$empty=new ObjectArrayList<>();
    // Do not cancel renderSectionLayer: Sable's own layer callbacks must still run.
    @ModifyExpressionValue(method="renderSectionLayer",require=0,at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/LevelRenderer;visibleSections:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> space$hideTerrain(ObjectArrayList<SectionRenderDispatcher.RenderSection> original){if(SpaceRenderView.active()){SpaceRenderView.terrainSkips++;return space$empty;}return original;}
    @Inject(method={"renderClouds","renderSnowAndRain","renderWorldBorder"},at=@At("HEAD"),cancellable=true)
    private void space$noClouds(CallbackInfo ci){if(SpaceRenderView.active()){SpaceRenderView.cloudSkips++;ci.cancel();}}
    @Inject(method="renderSky",at=@At("HEAD"),cancellable=true)
    private void space$noSky(org.joml.Matrix4f view,org.joml.Matrix4f projection,float partial,net.minecraft.client.Camera camera,boolean foggy,Runnable fog,CallbackInfo ci){if(SpaceRenderView.active()){SpaceRenderView.skySkips++;io.github.astelev.around_planet.client.SpaceSkyRenderer.render(view,projection,partial,camera);ci.cancel();}else if(io.github.astelev.around_planet.client.SurfaceSkyRenderer.render(view,projection,partial,camera,foggy,fog))ci.cancel();}
}
