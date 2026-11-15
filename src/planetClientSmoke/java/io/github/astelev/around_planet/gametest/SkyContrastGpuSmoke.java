package io.github.astelev.around_planet.gametest;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.github.astelev.around_planet.client.SurfaceSkyRenderer;
import org.joml.Matrix4f;
/** Render the real GLSL to an isolated target: daylight veil, altitude fade and solar aureole. */
public final class SkyContrastGpuSmoke {
 public static void check(){try{var field=SurfaceSkyRenderer.class.getDeclaredField("shader");field.setAccessible(true);var shader=(net.minecraft.client.renderer.ShaderInstance)field.get(null);var target=new TextureTarget(1,1,false,net.minecraft.client.Minecraft.ON_OSX);try{
  shader.safeGetUniform("InverseProjection").set(new Matrix4f());shader.safeGetUniform("RayTransform").set(new Matrix4f());shader.safeGetUniform("SkyColor").set(.3f,.5f,.8f);shader.safeGetUniform("Horizon").set(-.5f);shader.safeGetUniform("Visibility").set(1f);shader.safeGetUniform("Layer").set(1);shader.safeGetUniform("CloudFog").set(0f,0f,0f,0f);shader.safeGetUniform("MoonCenter").set(0f,0f,100f);shader.safeGetUniform("MoonRadius").set(10f);shader.safeGetUniform("MoonTextureX").set(1f,0f,0f);shader.safeGetUniform("MoonTextureZ").set(0f,0f,1f);
  shader.safeGetUniform("Darkness").set(0f);shader.safeGetUniform("SunDirection").set(0f,1f,0f);float[] day=pixel(shader,target);
  shader.safeGetUniform("SunDirection").set(0f,-1f,0f);float[] night=pixel(shader,target);
  shader.safeGetUniform("Darkness").set(1f);shader.safeGetUniform("SunDirection").set(0f,1f,0f);float[] space=pixel(shader,target);
  require(day[2]>.25f&&night[2]<.08f,"Day Moon still occludes atmosphere: "+java.util.Arrays.toString(day)+" / "+java.util.Arrays.toString(night));require(Math.abs(space[2]-night[2])<.015,"Day veil leaked into high-altitude vacuum");
  shader.safeGetUniform("Darkness").set(0f);shader.safeGetUniform("MoonRadius").set(0f);var sun=new org.joml.Vector3f(0,.08f,1).normalize();shader.safeGetUniform("SunDirection").set(sun.x,sun.y,sun.z);float[] halo=pixel(shader,target);require(halo[3]>.08f&&halo[3]<.5f,"Solar halo too narrow or opaque: "+java.util.Arrays.toString(halo));
  io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SKY_CONTRAST_GPU_PASS day={} night={} vacuum={} halo={}",day,night,space,halo);
 }finally{target.destroyBuffers();net.minecraft.client.Minecraft.getInstance().getMainRenderTarget().bindWrite(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.disableBlend();}}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static float[] pixel(net.minecraft.client.renderer.ShaderInstance shader,TextureTarget target){target.setClearColor(0,0,0,0);target.clear(net.minecraft.client.Minecraft.ON_OSX);target.bindWrite(true);RenderSystem.disableDepthTest();RenderSystem.disableCull();RenderSystem.disableBlend();var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);b.addVertex(-1,-1,0);b.addVertex(1,-1,0);b.addVertex(1,1,0);b.addVertex(-1,1,0);try(var v=new VertexBuffer(VertexBuffer.Usage.STATIC)){v.bind();v.upload(b.buildOrThrow());v.drawWithShader(new Matrix4f(),new Matrix4f(),shader);VertexBuffer.unbind();}var value=org.lwjgl.BufferUtils.createFloatBuffer(4);org.lwjgl.opengl.GL11.glReadPixels(0,0,1,1,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_FLOAT,value);return new float[]{value.get(0),value.get(1),value.get(2),value.get(3)};}
 private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
