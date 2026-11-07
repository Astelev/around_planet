package io.github.astelev.around_planet.gametest;

import com.mojang.blaze3d.platform.GlStateManager;
import com.seibel.distanthorizons.common.render.openGl.glObject.GLState;
import com.seibel.distanthorizons.common.render.openGl.glObject.shader.GlShader;
import io.github.astelev.around_planet.AroundPlanet;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** The actual patched DH fragment shader must keep the middle of a crossing tile. */
final class DhSeamGpuRegression {
    static void verify() {
        String vertex="""
                #version 330 core
                out vec4 vPos; out vec4 vertexColor; out vec3 vertexWorldPos; out vec3 vBlockPos;
                flat out uint vNormalIndex; flat out uint vTextureTileId;
                void main() {
                    vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2)*2.0-1.0;
                    gl_Position=vec4(p,0.0,1.0);
                    vPos=vec4(0.0);vertexColor=vec4(1.0,0.0,0.0,1.0);
                    vertexWorldPos=vec3(p.x*2.0,0.0,0.25);vBlockPos=vec3(0.0);
                    vNormalIndex=0u;vTextureTileId=0u;
                }
                """;
        int vs=compile(GL20.GL_VERTEX_SHADER,vertex);
        int fs=compile(GL20.GL_FRAGMENT_SHADER,GlShader.loadFile("assets/distanthorizons/shaders/terrain/gl/frag.frag",false));
        int program=GL20.glCreateProgram();
        int fbo=GL30.glGenFramebuffers(),texture=GL11.glGenTextures(),vao=GL30.glGenVertexArrays();
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        boolean externalClip=GL11.glIsEnabled(GL30.GL_CLIP_DISTANCE0);
        try(var saved=new GLState()) {
            GlStateManager._glBindVertexArray(vao);
            GlStateManager._bindTexture(texture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,16,16,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,(java.nio.ByteBuffer)null);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,fbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);
            GL11.glViewport(0,0,16,16);
            GL11.glDisable(GL30.GL_CLIP_DISTANCE0);GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_STENCIL_TEST);GL11.glDisable(GL11.GL_BLEND);
            GL11.glColorMask(true,true,true,true);
            GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL20.glAttachShader(program,vs);GL20.glAttachShader(program,fs);GL20.glLinkProgram(program);
            if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new AssertionError(GL20.glGetProgramInfoLog(program));
            GlStateManager._glUseProgram(program);
            GL20.glUseProgram(program);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"apChunkMask"),7);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"apSeamEnabled"),1);
            GL20.glUniform1f(GL20.glGetUniformLocation(program,"apSeamMinX"),-1);
            GL20.glUniform1f(GL20.glGetUniformLocation(program,"apSeamMaxX"),1);
            int near=GL20.glGetUniformLocation(program,"uClipDistance");
            int dither=GL20.glGetUniformLocation(program,"uDitherDhRendering");
            GL20.glUniform1f(near,16);
            GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);
            require(red(8)<5,"Native 16-block near exclusion was not reproduced");
            GL20.glUniform1f(near,0.0F);GL20.glUniform1i(dither,0);
            GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);
            require(red(8)>240,"Canonical part of crossing LOD tile was removed");
            require(red(2)<5 && red(14)<5,"Noncanonical part of LOD tile leaked across the seam");
            chunkMask(program);
            nativeMaskEdge();
            AroundPlanet.LOGGER.info("DH_SEAM_CLIP_GPU PASS: native near hole reproduced then filled; both canonical X planes retained");
        } finally {
            if(externalClip)GL11.glEnable(GL30.GL_CLIP_DISTANCE0);else GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
            GL11.glClearColor(clear[0],clear[1],clear[2],clear[3]);
            GL20.glDeleteProgram(program);GL20.glDeleteShader(vs);GL20.glDeleteShader(fs);
            GL30.glDeleteFramebuffers(fbo);GL11.glDeleteTextures(texture);GL30.glDeleteVertexArrays(vao);
        }
    }
    private static void chunkMask(int program) {
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager._activeTexture(GL13.GL_TEXTURE7);GL13.glActiveTexture(GL13.GL_TEXTURE7);
        int previous=GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D),texture=GL11.glGenTextures();
        int sampler=GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);GL33.glBindSampler(7,0);
        int skipRows=GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        try {
            GL11.glBindTexture(GL12.GL_TEXTURE_3D,texture);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D,GL12.GL_TEXTURE_MAX_LEVEL,0);
            var data=BufferUtils.createByteBuffer(4);data.putInt(-1).flip();
            // Reproduce state left by a preceding atlas sub-image upload.
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS,42);
            io.github.astelev.around_planet.compat.DhSeamCoverage.tightlyPacked(()->
                    GL12.glTexImage3D(GL12.GL_TEXTURE_3D,0,GL11.GL_RGBA8,1,1,1,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data));
            require(GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS)==42,"Upload did not restore caller pixel layout");
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"apChunkMask"),7);
            GL20.glUniform3f(GL20.glGetUniformLocation(program,"apChunkOffset"),4,4,4);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"apChunkMaskEnabled"),1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);
            require(red(8)<5,"LOD remained over a ready real chunk");
            data.clear();data.putInt(0).flip();
            io.github.astelev.around_planet.compat.DhSeamCoverage.tightlyPacked(()->
                    GL12.glTexSubImage3D(GL12.GL_TEXTURE_3D,0,0,0,0,1,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data));
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);
            require(red(8)>240,"LOD disappeared over an unbuilt chunk");
            data.clear();data.putInt(-1).flip();
            io.github.astelev.around_planet.compat.DhSeamCoverage.tightlyPacked(()->
                    GL12.glTexSubImage3D(GL12.GL_TEXTURE_3D,0,0,0,0,1,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data));
            GL20.glUniform3f(GL20.glGetUniformLocation(program,"apChunkOffset"),-32,-32,-32);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);
            require(red(8)>240,"LOD outside the chunk mask was removed");
            AroundPlanet.LOGGER.info("DH_SEAM_MASK_GPU PASS: ready chunks exclude LODs, unbuilt/outside chunks retain fallback; atlas unpack state restored");
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS,skipRows);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"apChunkMaskEnabled"),0);
            GL11.glBindTexture(GL12.GL_TEXTURE_3D,previous);GlStateManager._deleteTexture(texture);
            GL33.glBindSampler(7,sampler);
            GlStateManager._activeTexture(active);GL13.glActiveTexture(active);
        }
    }
    private static void nativeMaskEdge() {
        var mc=net.minecraft.client.Minecraft.getInstance();
        int radius=Math.min(32,mc.options.getEffectiveRenderDistance()+1);
        var params=new com.seibel.distanthorizons.core.render.RenderParams();
        params.exactCameraPosition=new com.seibel.distanthorizons.core.util.math.DhVec3d((-16-radius)*16+0.5,82.5,8.5);
        var shader=com.seibel.distanthorizons.common.render.openGl.GlDhTerrainRenderer_neoforge.INSTANCE.getTerrainShaderProgram();
        shader.bind();
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        io.github.astelev.around_planet.compat.DhSeamCoverage.bind(shader,params,32);
        try {
            GlStateManager._activeTexture(GL13.GL_TEXTURE7);GL13.glActiveTexture(GL13.GL_TEXTURE7);
            int texture=GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
            require(GL11.glGetTexLevelParameteri(GL12.GL_TEXTURE_3D,0,GL11.GL_TEXTURE_WIDTH)==radius*2+1,
                    "LOD coverage ends before the retained mesh range");
            var pixel=BufferUtils.createByteBuffer(4);
            GL45.glGetTextureSubImage(texture,0,radius*2,5-mc.level.getMinSection(),radius,1,1,1,
                    GL11.GL_RED,GL11.GL_UNSIGNED_BYTE,pixel);
            require((pixel.get(0)&255)==255,"Ready outer-ring geometry was not excluded from translated LODs");
            AroundPlanet.LOGGER.info("DH_SEAM_MASK_EDGE_GPU PASS: actual coverage includes ready geometry at the native view plus one ring");
        } finally {
            GlStateManager._activeTexture(active);GL13.glActiveTexture(active);
            io.github.astelev.around_planet.compat.DhSeamCoverage.unbind(shader);
        }
    }
    private static int compile(int type,String source) {
        int shader=GL20.glCreateShader(type);GL20.glShaderSource(shader,source);GL20.glCompileShader(shader);
        if(GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)==0)throw new AssertionError(GL20.glGetShaderInfoLog(shader));
        return shader;
    }
    private static int red(int x) {
        var pixel=BufferUtils.createByteBuffer(4);GL11.glReadPixels(x,8,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
        return pixel.get(0)&255;
    }
    private static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
    private DhSeamGpuRegression() {}
}
