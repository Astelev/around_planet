package io.github.astelev.around_planet.compat;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.distanthorizons.common.render.openGl.glObject.shader.GlShaderProgram;
import com.seibel.distanthorizons.core.render.RenderParams;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Exclude LOD sections only after the destination's real terrain meshes are ready. */
public final class DhSeamCoverage {
    private static final int UNIT=7;
    private static int texture,textureSize,textureHeight;
    private static java.nio.ByteBuffer pixels;

    public static void bind(GlShaderProgram shader,RenderParams params,int shiftChunks) {
        var mc=Minecraft.getInstance();
        int radius=Math.min(32,Math.max(1,mc.options.getEffectiveRenderDistance()+1));
        int size=radius*2+1;
        int cx=Math.floorDiv((int)Math.floor(params.exactCameraPosition.x),16);
        int cz=Math.floorDiv((int)Math.floor(params.exactCameraPosition.z),16);
        int height=mc.level.getSectionsCount();
        if(pixels==null || pixels.capacity()!=size*size*height)pixels=BufferUtils.createByteBuffer(size*size*height);
        pixels.clear();
        // Texture layout: X contiguous, then Y, then Z; coverage is bounded by the native view.
        for(int z=cz-radius;z<=cz+radius;z++)for(int x=cx-radius;x<=cx+radius;x++) {
            for(int section=0;section<height;section++) {
                int y=mc.level.getMinSection()+section;
                boolean ready=(io.github.astelev.around_planet.client.SeamSectionCoverage.ready(mc.level,x+shiftChunks,y,z,null)
                        || io.github.astelev.around_planet.client.SeamMeshCache.hasMesh(x,y,z));
                int index=((z-cz+radius)*height+section)*size+x-cx+radius;
                pixels.put(index,(byte)(ready?255:0));
            }
        }
        pixels.position(pixels.capacity());pixels.flip();
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        active(GL13.GL_TEXTURE0+UNIT);
        int bound=GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
        previousSampler=GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        GL33.glBindSampler(UNIT,0);
        try {
            // Allocate a target-neutral name for this 3D texture.
            if(texture==0)texture=GL11.glGenTextures();
            GL11.glBindTexture(GL12.GL_TEXTURE_3D,texture);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D,GL12.GL_TEXTURE_MAX_LEVEL,0);
            tightlyPacked(()->{
                if(textureSize!=size || textureHeight!=height) {
                    GL12.glTexImage3D(GL12.GL_TEXTURE_3D,0,GL30.GL_R8,size,height,size,0,GL11.GL_RED,GL11.GL_UNSIGNED_BYTE,pixels);
                    textureSize=size;textureHeight=height;
                } else GL12.glTexSubImage3D(GL12.GL_TEXTURE_3D,0,0,0,0,size,height,size,GL11.GL_RED,GL11.GL_UNSIGNED_BYTE,pixels);
            });
            shader.setUniform(shader.getUniformLocation("apChunkMask"),UNIT);
            GL20.glUniform3f(shader.getUniformLocation("apChunkOffset"),
                    (float)(params.exactCameraPosition.x-(cx-radius)*16.0),
                    (float)(params.exactCameraPosition.y-mc.level.getMinBuildHeight()),
                    (float)(params.exactCameraPosition.z-(cz-radius)*16.0));
            shader.setUniform(shader.getUniformLocation("apChunkMaskEnabled"),true);
        } finally {
            active(active);
        }
        // Caller restores binding after both opaque and transparent terrain draws.
        previousBinding=bound;previousActive=active;
    }
    private static int previousBinding,previousActive,previousSampler;
    private static void active(int unit) {GlStateManager._activeTexture(unit);GL13.glActiveTexture(unit);}
    public static void unbind(GlShaderProgram shader) {
        shader.setUniform(shader.getUniformLocation("apChunkMaskEnabled"),false);
        active(GL13.GL_TEXTURE0+UNIT);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D,previousBinding);
        GL33.glBindSampler(UNIT,previousSampler);
        active(previousActive);
    }
    /** Other atlas uploads can leave row/image skips set; never inherit their CPU buffer layout. */
    public static void tightlyPacked(Runnable upload) {
        int[] names={GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_UNPACK_SKIP_ROWS,
                GL11.GL_UNPACK_SKIP_PIXELS,GL12.GL_UNPACK_IMAGE_HEIGHT,GL12.GL_UNPACK_SKIP_IMAGES};
        int[] old=new int[names.length];
        int pbo=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        for(int i=0;i<names.length;i++) {old[i]=GL11.glGetInteger(names[i]);GL11.glPixelStorei(names[i],i==0?1:0);}
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
        try {upload.run();} finally {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,pbo);
            for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],old[i]);
        }
    }
    public static void clear() {
        Runnable release=()->{if(texture!=0)GlStateManager._deleteTexture(texture);texture=0;textureSize=0;textureHeight=0;pixels=null;};
        if(RenderSystem.isOnRenderThread())release.run();else RenderSystem.recordRenderCall(release::run);
    }
    private DhSeamCoverage() {}
}
