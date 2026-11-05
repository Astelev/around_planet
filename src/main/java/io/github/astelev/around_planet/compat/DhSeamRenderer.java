package io.github.astelev.around_planet.compat;

import com.seibel.distanthorizons.common.render.openGl.GlDhMetaRenderer_neoforge;
import com.seibel.distanthorizons.common.render.openGl.GlDhTerrainRenderer_neoforge;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/** Append translated terrain to DH's own depth/color target, before its post-processing and vanilla fade. */
public final class DhSeamRenderer {
    public static long passes,opaqueDraws;
    public static void append(RenderParams main,boolean opaque,IProfilerWrapper profiler) {
        var mc=Minecraft.getInstance();
        if(mc.level==null || main.renderBufferHandler==null || !PlanetConfig.SPEC.isLoaded()
                || !PlanetConfig.DH_SEAM.getAsBoolean() || PlanetBorders.get(mc.level.getWorldBorder())==null
                || DhSeamViews.shaderPackActive()
                || com.seibel.distanthorizons.core.config.Config.Client.Advanced.Debugging.renderWireframe.get())return;
        var shader=GlDhTerrainRenderer_neoforge.INSTANCE.getTerrainShaderProgram();
        if(GlDhMetaRenderer_neoforge.INSTANCE.shaderProgramForThisFrame!=shader)return;
        var view=DhSeamViews.select(main);if(view==null)return;var p=view.params();
        p.renderBufferHandler.buildRenderList(p);
        var buffers=p.renderBufferHandler.getColumnRenderBuffers();if(buffers.size()==0)return;
        shader.bind();int program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        float near=GL20.glGetUniformf(program,shader.uClipDistance);
        int dither=GL20.glGetUniformi(program,shader.uDitherDhRendering);
        try {
            // Keep native projection, TAA jitter, target and depth: both views are a single DH scene.
            // No second fillUniformData call, which would advance the native TAA sequence.
            shader.setUniform(shader.uClipDistance,0.0F);shader.setUniform(shader.uDitherDhRendering,false);
            var topology=PlanetBorders.get(mc.level.getWorldBorder());
            shader.setUniform(shader.getUniformLocation("apSeamMinX"),(float)(topology.west()-p.exactCameraPosition.x));
            shader.setUniform(shader.getUniformLocation("apSeamMaxX"),(float)(topology.east()-p.exactCameraPosition.x));
            DhSeamCoverage.bind(shader,p,view.shiftChunks());
            try {
                // Call the shader directly: re-entering the renderer would recurse into this append hook.
                shader.render(p,opaque,buffers,profiler);passes++;
                if(opaque)for(int i=0;i<buffers.size();i++)if(buffers.get(i).buffersUploaded)opaqueDraws++;
            } finally {DhSeamCoverage.unbind(shader);}
        } finally {
            shader.bind();shader.setUniform(shader.uClipDistance,near);shader.setUniform(shader.uDitherDhRendering,dither!=0);
            var topology=PlanetBorders.get(mc.level.getWorldBorder());
            shader.setUniform(shader.getUniformLocation("apSeamMinX"),(float)(topology.west()-main.exactCameraPosition.x));
            shader.setUniform(shader.getUniformLocation("apSeamMaxX"),(float)(topology.east()-main.exactCameraPosition.x));
        }
    }
    private DhSeamRenderer() {}
}
