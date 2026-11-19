package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import org.joml.Matrix4f;
import java.io.IOException;
/** One bounded ray/sphere sky pass. Ground clips the atmosphere; works outside and inside the shell. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class AtmosphereRenderer {
    private static ShaderInstance shader;private static VertexBuffer quad;public static long frames;
    @SubscribeEvent public static void shaders(RegisterShadersEvent event)throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("around_planet","atmosphere"),DefaultVertexFormat.POSITION),s->shader=s);
    }
    public static void render(Matrix4f sky,Matrix4f projection,SpaceVector observer,double radius,double top){
        if(shader==null || top<=0)return;
        if(quad==null){var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);b.addVertex(-1,-1,0);b.addVertex(1,-1,0);b.addVertex(1,1,0);b.addVertex(-1,1,0);quad=new VertexBuffer(VertexBuffer.Usage.STATIC);quad.bind();quad.upload(b.buildOrThrow());VertexBuffer.unbind();}
        shader.safeGetUniform("InverseProjection").set(new Matrix4f(projection).invert());shader.safeGetUniform("RayTransform").set(new Matrix4f(sky).invert());
        shader.safeGetUniform("Observer").set((float)(observer.x()/radius),(float)(observer.y()/radius),(float)(observer.z()/radius));
        var sun=SpaceEnvironment.SUN_DIRECTION;shader.safeGetUniform("SunDirection").set((float)sun.x(),(float)sun.y(),(float)sun.z());shader.safeGetUniform("OuterRadius").set((float)(1+top/radius));
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.setShader(()->shader);
        quad.bind();quad.drawWithShader(new Matrix4f(),new Matrix4f(),shader);VertexBuffer.unbind();frames++;
    }
    private AtmosphereRenderer(){}
}
