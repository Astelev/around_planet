package io.github.astelev.around_planet.gametest;

import com.mojang.blaze3d.platform.GlStateManager;
import com.seibel.distanthorizons.common.render.openGl.glObject.GLState;
import com.seibel.distanthorizons.common.render.openGl.glObject.shader.GlShader;
import io.github.astelev.around_planet.AroundPlanet;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** The actual patched DH fragment shader must keep the middle of a crossing tile. */
final class DhClipRegression {
    static void verify() {
        String vertex="""
                #version 330 core
                out vec4 vPos; out vec4 vertexColor; out vec3 vertexWorldPos; out vec3 vBlockPos;
                flat out uint vNormalIndex; flat out uint vTextureTileId;
                void main() {
                    vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2)*2.0-1.0;
                    gl_Position=vec4(p,0.0,1.0);
                    vPos=vec4(0.0);vertexColor=vec4(1.0,0.0,0.0,1.0);
                    vertexWorldPos=vec3(p.x*2.0,0.0,100.0);vBlockPos=vec3(0.0);
                    vNormalIndex=0u;vTextureTileId=0u;
                }
                """;
        int vs=compile(GL20.GL_VERTEX_SHADER,vertex);
        int fs=compile(GL20.GL_FRAGMENT_SHADER,GlShader.loadFile("assets/distanthorizons/shaders/terrain/gl/frag.frag",false));
        int program=GL20.glCreateProgram();
        int fbo=GL30.glGenFramebuffers(),texture=GL11.glGenTextures(),vao=GL30.glGenVertexArrays();
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        try(var saved=new GLState()) {
            GlStateManager._glBindVertexArray(vao);
            GlStateManager._bindTexture(texture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,16,16,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,(java.nio.ByteBuffer)null);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,fbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);
            GL11.glViewport(0,0,16,16);
            GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_STENCIL_TEST);GL11.glDisable(GL11.GL_BLEND);
            GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL20.glAttachShader(program,vs);GL20.glAttachShader(program,fs);GL20.glLinkProgram(program);
            if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new AssertionError(GL20.glGetProgramInfoLog(program));
            GlStateManager._glUseProgram(program);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"apSeamEnabled"),1);
            GL20.glUniform1f(GL20.glGetUniformLocation(program,"apSeamMinX"),-1);
            GL20.glUniform1f(GL20.glGetUniformLocation(program,"apSeamMaxX"),1);
            GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);
            require(red(8)>240,"Canonical part of crossing LOD tile was removed");
            require(red(2)<5 && red(14)<5,"Noncanonical part of LOD tile leaked across the seam");
            AroundPlanet.LOGGER.info("DH_CLIP_GPU PASS: crossing tile preserved inside and discarded outside both X planes");
        } finally {
            GL11.glClearColor(clear[0],clear[1],clear[2],clear[3]);
            GL20.glDeleteProgram(program);GL20.glDeleteShader(vs);GL20.glDeleteShader(fs);
            GL30.glDeleteFramebuffers(fbo);GL11.glDeleteTextures(texture);GL30.glDeleteVertexArrays(vao);
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
    private DhClipRegression() {}
}
