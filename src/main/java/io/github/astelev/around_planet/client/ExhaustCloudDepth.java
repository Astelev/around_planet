package io.github.astelev.around_planet.client;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.joml.Matrix4f;
/** Optional Simple Clouds buffers: copied DH and vanilla depth use different projections. */
public final class ExhaustCloudDepth {
    public static long boundFrames;
    private static final Matrix4f INVERSE=new Matrix4f();private static long frame=-1;
    public static void capture(Matrix4f projection){INVERSE.set(projection).invert();frame=DeferredExhaust.frame;}
    public static void bind(ShaderInstance shader){
        shader.safeGetUniform("HasCloudDepth").set(0);
        if(frame!=DeferredExhaust.frame||SpaceRenderView.active()||!net.neoforged.fml.ModList.get().isLoaded("simpleclouds"))return;
        try{
            var type=Class.forName("dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer");var renderer=type.getMethod("getInstance").invoke(null);if(renderer==null)return;
            var cloud=(RenderTarget)type.getMethod("getCloudTarget").invoke(renderer);var transparency=type.getMethod("getCloudTransparencyTarget").invoke(renderer);if(cloud==null||cloud.getDepthTextureId()<0)return;
            shader.setSampler("CloudDepth",cloud.getDepthTextureId());shader.setSampler("CloudColor",cloud.getColorTextureId());
            shader.safeGetUniform("HasCloudTransparency").set(0);
            if(transparency!=null){int reveal=(Integer)transparency.getClass().getMethod("getRevealageTextureId").invoke(transparency);shader.setSampler("CloudReveal",reveal);shader.safeGetUniform("HasCloudTransparency").set(1);}
            shader.safeGetUniform("InverseCloudProjection").set(INVERSE);var target=Minecraft.getInstance().getMainRenderTarget();shader.safeGetUniform("ViewportSize").set((float)target.width,(float)target.height);shader.safeGetUniform("HasCloudDepth").set(1);boundFrames++;
        }catch(ReflectiveOperationException|LinkageError e){shader.safeGetUniform("HasCloudDepth").set(0);}
    }
    private ExhaustCloudDepth(){}
}
