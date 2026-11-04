package io.github.astelev.around_planet.compat;

import com.seibel.distanthorizons.common.render.openGl.GlDhMetaRenderer_neoforge;
import com.seibel.distanthorizons.common.render.openGl.GlDhTerrainRenderer_neoforge;
import com.seibel.distanthorizons.common.render.openGl.glObject.GLState;
import com.seibel.distanthorizons.common.render.openGl.terrain.GlBlockTextureAtlas;
import com.seibel.distanthorizons.core.config.Config.Client.Advanced.Graphics.Texture;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL20;

/** Draw terrain into IP's current stencil target, never DH's full-screen fog/composite. */
public final class DhPortalTerrain {
    public static void render(RenderParams params,IProfilerWrapper profiler) {
        var meta=GlDhMetaRenderer_neoforge.INSTANCE;
        var previousShader=meta.shaderProgramForThisFrame;
        boolean clamp=GL11.glIsEnabled(GL32.GL_DEPTH_CLAMP);
        boolean clip=GL11.glIsEnabled(GL30.GL_CLIP_DISTANCE0);
        try(var saved=new GLState()) {
            // DH's ordinary overdraw near plane (and IP's oblique near plane) must
            // not cull the thin area next to the seam from the portal render list.
            if(qouteall.imm_ptl.core.render.context_management.RenderStates.basicProjectionMatrix!=null)
                params.dhProjectionMatrix.set(com.seibel.distanthorizons.common.wrappers.McObjectConverter_neoforge.convert(
                        qouteall.imm_ptl.core.render.context_management.RenderStates.basicProjectionMatrix));
            com.seibel.distanthorizons.core.util.RenderUtil.setClipPlanes(params.dhProjectionMatrix,0.05F,
                    com.seibel.distanthorizons.core.util.RenderUtil.getFarClipPlaneDistanceInBlocks(),false);
            params.renderBufferHandler.buildRenderList(params);
            // Match the depth already in the portal target. Depth clamp keeps distant
            // terrain visible beyond vanilla's far plane without hiding nearer blocks.
            params.dhProjectionMatrix.set(params.mcProjectionMatrix);
            params.apiCopy.update(params);
            var renderer=GlDhTerrainRenderer_neoforge.INSTANCE;
            var shader=renderer.getTerrainShaderProgram();
            shader.tryInit();shader.bind();shader.fillUniformData(params);
            nearCoverage(shader.uClipDistance,shader.uDitherDhRendering);
            shader.setUniform(shader.getUniformLocation("apPortalNearClip"),true);
            // Portal terrain has no DH temporal resolve; jitter would disagree with chunk pixels/depth.
            shader.setUniform(shader.getUniformLocation("uFrameMod8"),-1.0F);
            DhPortalChunks.bind(shader,params);
            try {
                meta.shaderProgramForThisFrame=shader;
                meta.bindLightmap(params.lightmap);
                if(Texture.enableTexturedLods.get()) {
                    GlBlockTextureAtlas.INSTANCE.uploadPendingTiles();GlBlockTextureAtlas.INSTANCE.bind();
                }
                GL11.glEnable(GL32.GL_DEPTH_CLAMP);
                // Depth clamp also disables the ordinary near plane. Restore a shader
                // eye-plane cut so triangles crossing the camera cannot become curtains.
                GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
                GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthFunc(GL11.GL_LEQUAL);GL11.glDepthMask(true);
                if(params.renderPass!=com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass.TRANSPARENT)
                    renderer.render(params,true,params.renderBufferHandler.getColumnRenderBuffers(),profiler);
                if(params.renderPass!=com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass.OPAQUE)
                    renderer.render(params,false,params.renderBufferHandler.getColumnRenderBuffers(),profiler);
            } finally {DhPortalChunks.unbind(shader);}
        } finally {
            meta.shaderProgramForThisFrame=previousShader;
            if(clamp)GL11.glEnable(GL32.GL_DEPTH_CLAMP);else GL11.glDisable(GL32.GL_DEPTH_CLAMP);
            if(clip)GL11.glEnable(GL30.GL_CLIP_DISTANCE0);else GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
        }
    }
    public static void nearCoverage(int clipUniform,int ditherUniform) {
        // The fork still leaves a +16-block fragment exclusion even after its
        // near-clip patch. Keep it in the main view, but never leave this hole
        // in a portal whose real chunks have already been clipped at the seam.
        GL20.glUniform1f(clipUniform,0.0F);
        GL20.glUniform1i(ditherUniform,0);
    }
    private DhPortalTerrain() {}
}
