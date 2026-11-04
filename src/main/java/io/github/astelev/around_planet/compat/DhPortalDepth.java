package io.github.astelev.around_planet.compat;

import com.mojang.blaze3d.platform.GlStateManager;
import com.seibel.distanthorizons.common.render.openGl.GlDhTerrainRenderer_neoforge;
import com.seibel.distanthorizons.common.render.openGl.glObject.GLState;
import com.seibel.distanthorizons.common.render.openGl.glObject.buffer.GLVertexBuffer;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper_neoforge;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.util.math.DhMat4f;
import com.seibel.distanthorizons.core.util.math.DhVec3f;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

/** DH's fullscreen color copy has no depth; prime opaque terrain before portal masks. */
public final class DhPortalDepth {
    private static RenderParams pending;
    private static int frame=-1,primed=-1;
    public static long passes,opaqueDraws;
    public static void capture(RenderParams params) {
        if(!PortalRendering.isRendering()) {pending=params;frame=RenderStates.frameIndex;}
    }
    public static void clear() {pending=null;frame=-1;primed=-1;DhPortalChunks.clear();}
    public static void beforePortals() {
        if(PortalRendering.isRendering() || frame!=RenderStates.frameIndex || primed==frame)return;
        var params=pending;pending=null;
        var mc=Minecraft.getInstance();
        if(params==null || params.renderBufferHandler==null || mc.level==null
                || !PlanetConfig.SPEC.isLoaded() || !PlanetConfig.DH_PORTAL_RENDER.getAsBoolean())return;
        var topology=PlanetBorders.get(mc.level.getWorldBorder());
        if(topology==null || topology.distanceToSeam(params.exactCameraPosition.x)>
                Math.max(PlanetConfig.DH_PREVIEW_DISTANCE.getAsInt(),mc.options.getEffectiveRenderDistance()*64))return;
        var iris=(com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor)
                com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector.INSTANCE.get(
                        com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor.class);
        if(iris!=null && iris.isShaderPackInUse())return;
        int target=MinecraftRenderWrapper_neoforge.INSTANCE.getTargetFramebuffer();
        if(target<0)return;
        var shader=GlDhTerrainRenderer_neoforge.INSTANCE.getTerrainShaderProgram();
        if(com.seibel.distanthorizons.common.render.openGl.GlDhMetaRenderer_neoforge.INSTANCE.shaderProgramForThisFrame!=shader)return;
        var buffers=params.renderBufferHandler.getColumnRenderBuffers();
        boolean opaque=false;
        search: for(int i=0;i<buffers.size();i++) {
            var buffer=buffers.get(i);if(!buffer.buffersUploaded)continue;
            for(var wrapper:buffer.vboOpaqueWrappers)if(wrapper instanceof GLVertexBuffer vbo && vbo.getVertexCount()>0) {
                opaque=true;break search;
            }
        }
        if(!opaque)return; // No GL state queries/pass for an empty or transparent-only view.
        var original=new DhMat4f(params.dhProjectionMatrix);original.multiply(params.dhModelViewMatrix);
        var projection=new DhMat4f(params.mcProjectionMatrix);projection.multiply(params.dhModelViewMatrix);
        boolean clamp=GL11.glIsEnabled(GL32.GL_DEPTH_CLAMP),clip=GL11.glIsEnabled(GL30.GL_CLIP_DISTANCE0);
        depthOnly(()->{
            try {
                GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER,target);
                shader.bind();shader.setUniform(shader.uCombinedMatrix,projection);
                shader.setUniform(shader.getUniformLocation("apPortalNearClip"),true);
                GL11.glEnable(GL32.GL_DEPTH_CLAMP);GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
                for(int i=0;i<buffers.size();i++) {
                    var buffer=buffers.get(i);if(!buffer.buffersUploaded)continue;
                    shader.setModelOffsetPos(new DhVec3f(
                            (float)(buffer.minCornerBlockPos.getX()-params.exactCameraPosition.x),
                            (float)(buffer.minCornerBlockPos.getY()-params.exactCameraPosition.y),
                            (float)(buffer.minCornerBlockPos.getZ()-params.exactCameraPosition.z)));
                    for(var wrapper:buffer.vboOpaqueWrappers) {
                        if(!(wrapper instanceof GLVertexBuffer vbo))continue;
                        var ibo=vbo.getQuadIBO();
                        long vertices=vbo.renderStampLock.readLock(),indices=ibo.renderStampLock.readLock();
                        try {
                            if(vbo.getVertexCount()==0 || vbo.getId()==0 || ibo.getId()==0)continue;
                            vbo.bind();ibo.bind();shader.bindVertexBuffer(vbo.getId());
                            GL11.glDrawElements(GL11.GL_TRIANGLES,(int)(vbo.getVertexCount()*1.5),ibo.getGlType(),0L);
                            opaqueDraws++;
                            vbo.unbind();ibo.unbind();
                        } finally {vbo.renderStampLock.unlock(vertices);ibo.renderStampLock.unlock(indices);}
                    }
                }
            } finally {
                shader.setUniform(shader.uCombinedMatrix,original);
                shader.setUniform(shader.getUniformLocation("apPortalNearClip"),false);
                if(clamp)GL11.glEnable(GL32.GL_DEPTH_CLAMP);else GL11.glDisable(GL32.GL_DEPTH_CLAMP);
                if(clip)GL11.glEnable(GL30.GL_CLIP_DISTANCE0);else GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
            }
        });
        primed=frame;passes++;
    }
    public static void depthOnly(Runnable draw) {
        var mask=BufferUtils.createByteBuffer(4);GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK,mask);
        try(var saved=new GLState()) {
            GlStateManager._colorMask(false,false,false,false);
            GlStateManager._enableDepthTest();GlStateManager._depthFunc(GL11.GL_LEQUAL);GlStateManager._depthMask(true);
            draw.run();
        } finally {GlStateManager._colorMask(mask.get(0)!=0,mask.get(1)!=0,mask.get(2)!=0,mask.get(3)!=0);}
    }
    private DhPortalDepth() {}
}
