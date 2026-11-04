package io.github.astelev.around_planet.gametest;

import com.mojang.blaze3d.platform.GlStateManager;
import com.seibel.distanthorizons.common.render.openGl.glObject.GLState;
import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.compat.DhPortalDepth;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

final class DhDepthRegression {
    static void verify() {
        int vertex=compile(GL20.GL_VERTEX_SHADER,"""
                #version 330 core
                uniform float z;
                void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2)*2.0-1.0;gl_Position=vec4(p,z,1.0);}
                """);
        int fragment=compile(GL20.GL_FRAGMENT_SHADER,"""
                #version 330 core
                uniform vec4 color;out vec4 fragColor;
                void main(){fragColor=color;}
                """);
        int program=GL20.glCreateProgram(),vao=GL30.glGenVertexArrays(),fbo=GL30.glGenFramebuffers();
        int color=GL11.glGenTextures(),depth=GL11.glGenTextures();
        boolean clip=GL11.glIsEnabled(GL30.GL_CLIP_DISTANCE0);
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        try(var state=new GLState()) {
            GlStateManager._glBindVertexArray(vao);GL30.glBindVertexArray(vao);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,fbo);
            GlStateManager._bindTexture(color);GL11.glBindTexture(GL11.GL_TEXTURE_2D,color);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,8,8,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,(java.nio.ByteBuffer)null);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,color,0);
            GlStateManager._bindTexture(depth);GL11.glBindTexture(GL11.GL_TEXTURE_2D,depth);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL14.GL_DEPTH_COMPONENT24,8,8,0,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,(java.nio.ByteBuffer)null);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_DEPTH_ATTACHMENT,GL11.GL_TEXTURE_2D,depth,0);
            require(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)==GL30.GL_FRAMEBUFFER_COMPLETE,"Depth fixture framebuffer incomplete");
            GL20.glAttachShader(program,vertex);GL20.glAttachShader(program,fragment);GL20.glLinkProgram(program);
            require(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)!=0,GL20.glGetProgramInfoLog(program));
            GlStateManager._glUseProgram(program);GL20.glUseProgram(program);
            GL11.glViewport(0,0,8,8);GL11.glDisable(GL11.GL_STENCIL_TEST);GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_BLEND);GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
            GL11.glClearColor(0,0,0,1);GL11.glClearDepth(1);
            GlStateManager._depthMask(true);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager._disableDepthTest();draw(program,0,1,0,0); // DH color copy, no depth.
            GlStateManager._enableDepthTest();GlStateManager._depthFunc(GL11.GL_LEQUAL);
            draw(program,0.5F,0,0,1);require(channel(2)>240,"Color-only control did not reproduce portal overdraw");
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager._disableDepthTest();draw(program,0,1,0,0);
            DhPortalDepth.depthOnly(()->draw(program,0,1,0,0));
            GlStateManager._enableDepthTest();GlStateManager._depthMask(true);GlStateManager._depthFunc(GL11.GL_LEQUAL);
            draw(program,0.5F,0,0,1);require(channel(0)>240,"Far portal covered foreground LOD after depth priming");
            draw(program,-0.5F,0,0,1);require(channel(2)>240,"Near portal incorrectly hidden by farther LOD");
            AroundPlanet.LOGGER.info("DH_DEPTH_GPU PASS: color-only portal overdraw reproduced; foreground preserved and nearer portal visible after depth priming");
        } finally {
            if(clip)GL11.glEnable(GL30.GL_CLIP_DISTANCE0);else GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
            GL11.glClearColor(clear[0],clear[1],clear[2],clear[3]);
            GL20.glDeleteProgram(program);GL20.glDeleteShader(vertex);GL20.glDeleteShader(fragment);
            GL30.glDeleteVertexArrays(vao);GL30.glDeleteFramebuffers(fbo);GL11.glDeleteTextures(color);GL11.glDeleteTextures(depth);
        }
    }
    private static void draw(int program,float z,float r,float g,float b) {
        GL20.glUniform1f(GL20.glGetUniformLocation(program,"z"),z);
        GL20.glUniform4f(GL20.glGetUniformLocation(program,"color"),r,g,b,1);GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);
    }
    private static int channel(int index) {
        var pixel=BufferUtils.createByteBuffer(4);GL11.glReadPixels(4,4,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
        return pixel.get(index)&255;
    }
    private static int compile(int type,String source) {
        int shader=GL20.glCreateShader(type);GL20.glShaderSource(shader,source);GL20.glCompileShader(shader);
        require(GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)!=0,GL20.glGetShaderInfoLog(shader));return shader;
    }
    private static void require(boolean value,String message) {if(!value)throw new AssertionError(message);}
    private DhDepthRegression() {}
}
