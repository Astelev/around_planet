package io.github.astelev.around_planet.client;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import org.joml.Matrix4f;
import java.io.IOException;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class SurfaceSkyRenderer {
    private static ShaderInstance shader;private static VertexBuffer quad;
    @SubscribeEvent public static void shaders(RegisterShadersEvent event)throws IOException{
        CurvatureShaders.clear();event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("around_planet","surface_sky"),DefaultVertexFormat.POSITION),s->shader=s);
    }
    public static boolean render(Matrix4f view,Matrix4f projection,float partial,Camera camera,boolean foggy,Runnable fog){
        var mc=Minecraft.getInstance();if(shader==null || foggy || camera.getFluidInCamera()!=FogType.NONE || !SurfaceRenderState.enabled() || !PlanetConfig.SURFACE_SKY.getAsBoolean())return false;
        fog.run();SurfaceRenderState.sky(true);float fogStart=RenderSystem.getShaderFogStart(),fogEnd=RenderSystem.getShaderFogEnd();
        try{
            if(quad==null){var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);b.addVertex(-1,-1,0);b.addVertex(1,-1,0);b.addVertex(1,1,0);b.addVertex(-1,1,0);quad=new VertexBuffer(VertexBuffer.Usage.STATIC);quad.bind();quad.upload(b.buildOrThrow());VertexBuffer.unbind();}
            var sky=mc.level.getSkyColor(camera.getPosition(),partial);double dark=SurfaceRenderState.darkness(camera.getPosition().y);var surfaceFrame=PlanetDaylight.spin(mc.level,partial).frame(new PlanetScale(PlanetConfig.PLANET_SIZE.get()).geometry(),camera.getPosition().x,camera.getPosition().z);var sun=surfaceFrame.toLocal(SpaceEnvironment.SUN_DIRECTION);
            shader.safeGetUniform("InverseProjection").set(new Matrix4f(projection).invert());shader.safeGetUniform("RayTransform").set(new Matrix4f(view).invert());
            shader.safeGetUniform("SkyColor").set((float)sky.x,(float)sky.y,(float)sky.z);shader.safeGetUniform("SunDirection").set((float)sun.x(),(float)sun.y(),(float)sun.z());
            shader.safeGetUniform("Darkness").set((float)dark);shader.safeGetUniform("Horizon").set(PlanetConfig.SURFACE_CURVE.getAsBoolean()?(float)SurfaceVisualMath.horizon(camera.getPosition().y,new PlanetScale(PlanetConfig.PLANET_SIZE.get()).radius()):0);
            float legacyFog=AeroFogBridge.legacySkyStrength();float[] fogColor=RenderSystem.getShaderFogColor();shader.safeGetUniform("CloudFog").set(fogColor[0],fogColor[1],fogColor[2],legacyFog);
            var system=CelestialSky.current();if(system!=null){var position=camera.getPosition();var moon=MoonAppearance.at(system,new PlanetScale(PlanetConfig.PLANET_SIZE.get()).geometry(),PlanetDaylight.spin(mc.level,partial),new SpaceVector(position.x,position.y,position.z),mc.level.getGameTime()+partial);shader.safeGetUniform("MoonCenter").set((float)moon.center().x(),(float)moon.center().y(),(float)moon.center().z());shader.safeGetUniform("MoonRadius").set((float)moon.radius());}else shader.safeGetUniform("MoonRadius").set(0f);
            var textureX=surfaceFrame.toLocal(new SpaceVector(1,0,0));var textureZ=surfaceFrame.toLocal(new SpaceVector(0,0,1));shader.safeGetUniform("MoonTextureX").set((float)textureX.x(),(float)textureX.y(),(float)textureX.z());shader.safeGetUniform("MoonTextureZ").set((float)textureZ.x(),(float)textureZ.y(),(float)textureZ.z());shader.safeGetUniform("Visibility").set((1-mc.level.getRainLevel(partial))*(1-legacyFog));
            RenderSystem.setShaderFogStart(Float.MAX_VALUE/2);RenderSystem.setShaderFogEnd(Float.MAX_VALUE);RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);
            layer(0);
            float stars=(float)Math.max(mc.level.getStarBrightness(partial)*2,dark)*(1-mc.level.getRainLevel(partial));
            stars*=1-legacyFog;if(stars>0.001)SpaceSkyRenderer.stars(new Matrix4f(view).mul(SpaceSkyRenderer.inverse(surfaceFrame)),projection,stars);
            layer(1);SurfaceRenderState.skyFrames++;return true;
        }finally{VertexBuffer.unbind();RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShaderFogStart(fogStart);RenderSystem.setShaderFogEnd(fogEnd);RenderSystem.setShader(GameRenderer::getPositionColorShader);SurfaceRenderState.sky(false);}
    }
    private static void layer(int layer){shader.safeGetUniform("Layer").set(layer);RenderSystem.setShader(()->shader);quad.bind();quad.drawWithShader(new Matrix4f(),new Matrix4f(),shader);VertexBuffer.unbind();}
    private SurfaceSkyRenderer(){}
}
