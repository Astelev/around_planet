package io.github.astelev.aerospace.gametest;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.astelev.aerospace.client.FallbackPlaneRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class FallbackPlaneSmoke {
    private FallbackPlaneSmoke() {}

    public static void verify() {
        int previousFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        double clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        int[] stores = {GL11.GL_UNPACK_ROW_LENGTH, GL11.GL_UNPACK_SKIP_ROWS, GL11.GL_UNPACK_SKIP_PIXELS, GL11.GL_UNPACK_ALIGNMENT,
                GL11.GL_PACK_ROW_LENGTH, GL11.GL_PACK_SKIP_ROWS, GL11.GL_PACK_SKIP_PIXELS, GL11.GL_PACK_ALIGNMENT};
        int[] savedStores = java.util.Arrays.stream(stores).map(GL11::glGetInteger).toArray();
        int depth = GL11.glGenTextures(), palette = GL11.glGenTextures();
        var target = new TextureTarget(16, 16, true, Minecraft.ON_OSX);
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glColorMask(true, true, true, true);
            for (int store : stores) GL11.glPixelStorei(store, store == GL11.GL_PACK_ALIGNMENT || store == GL11.GL_UNPACK_ALIGNMENT ? 1 : 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, palette);
            var color = BufferUtils.createByteBuffer(4);
            color.put((byte) 0x71).put((byte) 0x8a).put((byte) 0x54).put((byte) 255).flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, color);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            var down = new Matrix4f().perspective((float) Math.PI / 2, 1, 0.1f, 10000)
                    .rotateX((float) Math.PI / 2).invert();
            probe(target, depth, palette, down, 500, 1, 1, true, "empty terrain");
            probe(target, depth, palette, down, 500, 0.5f, 1, false, "existing vanilla depth");
            probe(target, depth, palette, down, 500, 1, 0.5f, false, "existing LOD depth");
            probe(target, depth, palette, down, 40, 1, 1, false, "camera below plane");
            var reversedDown = new Matrix4f(down).mul(new Matrix4f().scaling(1, 1, -1));
            probe(target, depth, palette, reversedDown, 500, 1, 0, true, "reversed DH depth", 0, 1);
            probe(target, depth, palette, reversedDown, 500, 1, 0.5f, false, "reversed existing LOD", 0, 1);
            probe(target, depth, palette, reversedDown, 500, 0, 0, true, "both depths reversed", 0, 0);
            probe(target, depth, palette, reversedDown, 500, 0.5f, 0, false, "reversed existing vanilla", 0, 0);
            var up = new Matrix4f().perspective((float) Math.PI / 2, 1, 0.1f, 10000)
                    .rotateX(-(float) Math.PI / 2).invert();
            probe(target, depth, palette, up, 500, 1, 1, false, "sky above horizon");
        } finally {
            target.destroyBuffers();
            GL11.glDeleteTextures(depth);
            GL11.glDeleteTextures(palette);
            RenderSystem.bindTexture(texture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.clearDepth(clearDepth);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            if (stencil) GL11.glEnable(GL11.GL_STENCIL_TEST);
            for (int i = 0; i < stores.length; i++) GL11.glPixelStorei(stores[i], savedStores[i]);
        }
    }

    private static void probe(TextureTarget target, int depthTexture, int palette, Matrix4f inverse, double cameraY,
                              float vanillaDepth, float dhDepth, boolean shouldDraw, String description) {
        probe(target, depthTexture, palette, inverse, cameraY, vanillaDepth, dhDepth, shouldDraw, description, 1, 1);
    }

    private static void probe(TextureTarget target, int depthTexture, int palette, Matrix4f inverse, double cameraY,
                              float vanillaDepth, float dhDepth, boolean shouldDraw, String description,
                              float dhClearDepth, float vanillaClearDepth) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        var depths = BufferUtils.createFloatBuffer(256);
        for (int i = 0; i < 256; i++) depths.put(dhDepth);
        depths.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, 16, 16, 0,
                GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depths);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        target.setClearColor(0x20 / 255f, 0x30 / 255f, 0x40 / 255f, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.frameBufferId);
        GL11.glClearDepth(vanillaDepth);
        GL11.glDepthMask(true);
        GL11.glClearColor(0x20 / 255f, 0x30 / 255f, 0x40 / 255f, 1);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glClearDepth(1);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        FallbackPlaneRenderer.drawPass(inverse, new Vec3(0, cameraY, 0), 60, 100000,
                depthTexture, palette, -4096, -4096, 8192, 0x718a54, 0, 0, 0, dhClearDepth, vanillaClearDepth);
        if (GL11.glGetInteger(GL11.GL_DEPTH_FUNC) != previousDepthFunc) throw new AssertionError("Fallback changed depth-test state");
        var pixel = BufferUtils.createByteBuffer(4);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
        GL11.glReadPixels(8, 8, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        int r = Byte.toUnsignedInt(pixel.get(0)), g = Byte.toUnsignedInt(pixel.get(1)), b = Byte.toUnsignedInt(pixel.get(2));
        int expected = shouldDraw ? 0x718a54 : 0x203040;
        if (Math.abs(r - (expected >> 16 & 255)) > 1 || Math.abs(g - (expected >> 8 & 255)) > 1 || Math.abs(b - (expected & 255)) > 1) {
            throw new AssertionError("Fallback probe failed for " + description + ": " + r + "," + g + "," + b);
        }
        var preservedDepth = BufferUtils.createFloatBuffer(1);
        GL11.glReadPixels(8, 8, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, preservedDepth);
        if (Math.abs(preservedDepth.get(0) - vanillaDepth) > 0.00001) throw new AssertionError("Fallback wrote world depth");
    }
}
