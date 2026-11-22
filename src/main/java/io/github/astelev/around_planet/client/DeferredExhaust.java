package io.github.astelev.around_planet.client;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
/** One bounded mesh for the current world pass, replayed after clouds and optional AeroWorks fog. */
public final class DeferredExhaust {
    private static final ByteBufferBuilder STORAGE=new ByteBufferBuilder(65536);
    private static BufferBuilder builder;private static MeshData pending;
    private static final Matrix4f worldView=new Matrix4f(),worldProjection=new Matrix4f();
    private static State geometryState;public static long frame,draws;
    public static void begin(Matrix4f view,Matrix4f projection){frame++;if(pending!=null){pending.close();pending=null;}builder=null;STORAGE.clear();worldView.set(view);worldProjection.set(projection);geometryState=null;}
    public static VertexConsumer vertices(){if(builder==null){builder=new BufferBuilder(STORAGE,VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);geometryState=new State();}return builder;}
    public static void finish(){if(builder==null)return;pending=builder.build();builder=null;if(pending==null)return;var mesh=pending;var captured=geometryState;var view=new Matrix4f(worldView);var proj=new Matrix4f(worldProjection);AeroFogBridge.submit(()->{if(pending!=mesh)return;pending=null;draw(mesh,captured,view,proj);});}
    private static void draw(MeshData mesh,State captured,Matrix4f view,Matrix4f projection){
        var before=new State();boolean scope=SurfaceRenderState.radius()>0;
        try{captured.apply();Minecraft.getInstance().getMainRenderTarget().bindWrite(true);if(!scope)SurfaceRenderState.enter(view,projection);
            var shader=PropulsionExhaustRenderer.shader;if(shader==null){mesh.close();return;}ExhaustCloudDepth.bind(shader);shader.safeGetUniform("InverseJetProjection").set(new Matrix4f(captured.projection).invert());
            ExhaustRenderType.TYPE.setupRenderState();try{BufferUploader.drawWithShader(mesh);draws++;}finally{ExhaustRenderType.TYPE.clearRenderState();}
        }finally{if(!scope)SurfaceRenderState.exit();before.apply();}
    }
    private static final class State {
        final Matrix4f model=new Matrix4f(RenderSystem.getModelViewMatrix()),projection=new Matrix4f(RenderSystem.getProjectionMatrix());
        final VertexSorting sorting=RenderSystem.getVertexSorting();final float start=RenderSystem.getShaderFogStart(),end=RenderSystem.getShaderFogEnd();
        final float[] fog=RenderSystem.getShaderFogColor().clone(),color=RenderSystem.getShaderColor().clone();final com.mojang.blaze3d.shaders.FogShape shape=RenderSystem.getShaderFogShape();
        final boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),write=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),func=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        final int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcA=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstA=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        final int[] viewport=new int[4];State(){GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);}
        void apply(){RenderSystem.getModelViewStack().set(model);RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);RenderSystem.setShaderFogStart(start);RenderSystem.setShaderFogEnd(end);RenderSystem.setShaderFogShape(shape);RenderSystem.setShaderFogColor(fog[0],fog[1],fog[2],fog[3]);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);RenderSystem.depthMask(mask);RenderSystem.depthFunc(func);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();RenderSystem.blendFuncSeparate(src,dst,srcA,dstA);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,write);RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);}
    }
    private DeferredExhaust(){}
}
