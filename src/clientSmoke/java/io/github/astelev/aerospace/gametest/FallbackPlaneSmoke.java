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
    private static int lightmap, mcDepth;
    private static com.mojang.blaze3d.vertex.VertexBuffer raisedMesh;
    private static int light = 255;

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
        lightmap = GL11.glGenTextures();
        mcDepth = GL11.glGenTextures();
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
            setLight(255);
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
            raisedMesh = new com.mojang.blaze3d.vertex.VertexBuffer(com.mojang.blaze3d.vertex.VertexBuffer.Usage.STATIC);
            FallbackPlaneRenderer.uploadSurface(raisedMesh, new int[][]{{32,32},{32,32}}, 100000);
            probe(target, depth, palette, down, 500, 1, 1, true, "raised biome surface");
            probe(target, depth, palette, reversedDown, 500, 1, 0, true, "reversed raised biome", 0, 1);
            probe(target, depth, palette, down, 500, 1, 0.5f, false, "real LOD masks raised surface");
            setLight(32);
            probe(target, depth, palette, down, 500, 1, 1, true, "night lightmap");
        } finally {
            if (raisedMesh != null) { raisedMesh.close(); raisedMesh = null; }
            target.destroyBuffers();
            GL11.glDeleteTextures(depth);
            GL11.glDeleteTextures(palette);
            GL11.glDeleteTextures(lightmap);
            GL11.glDeleteTextures(mcDepth);
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

    private static void setLight(int value) {
        light = value;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lightmap);
        // An asymmetric 16x16 vanilla-layout map catches transposed sky/block
        // axes: emissive blocks stay bright even when the exposed sky is dark.
        var pixel = BufferUtils.createByteBuffer(16 * 16 * 4);
        for (int sky = 0; sky < 16; sky++) {
            for (int block = 0; block < 16; block++) {
                int brightness = block == 0 ? value : 255;
                pixel.put((byte) brightness).put((byte) brightness).put((byte) brightness).put((byte) 255);
            }
        }
        pixel.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 16, 16, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
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
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mcDepth);
        depths.clear();
        for (int i = 0; i < 256; i++) depths.put(vanillaDepth);
        depths.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, 16, 16, 0,
                GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depths);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        target.setClearColor(0x20 / 255f, 0x30 / 255f, 0x40 / 255f, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.frameBufferId);
        GL11.glClearDepth(dhDepth);
        GL11.glDepthMask(true);
        GL11.glClearColor(0x20 / 255f, 0x30 / 255f, 0x40 / 255f, 1);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glClearDepth(1);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        FallbackPlaneRenderer.drawPass(new Matrix4f(inverse).invert(), new Vec3(0, cameraY, 0), 60, 100000,
                depthTexture, palette, raisedMesh == null ? -4096 : -io.github.astelev.aerospace.compat.dh.FallbackBiomeColors.CELL_SIZE, raisedMesh == null ? -4096 : -io.github.astelev.aerospace.compat.dh.FallbackBiomeColors.CELL_SIZE,
                raisedMesh == null ? 8192 : 2 * io.github.astelev.aerospace.compat.dh.FallbackBiomeColors.CELL_SIZE, 0x718a54, dhClearDepth, lightmap, mcDepth, vanillaClearDepth, 0.55f, raisedMesh);
        if (GL11.glGetInteger(GL11.GL_DEPTH_FUNC) != previousDepthFunc) throw new AssertionError("Fallback changed depth-test state");
        var pixel = BufferUtils.createByteBuffer(4);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
        GL11.glReadPixels(8, 8, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        int r = Byte.toUnsignedInt(pixel.get(0)), g = Byte.toUnsignedInt(pixel.get(1)), b = Byte.toUnsignedInt(pixel.get(2));
        float luminance = 0x71 * 0.2126f + 0x8a * 0.7152f + 0x54 * 0.0722f;
        int er = Math.round((luminance * 0.2f + 0x71 * 0.8f) * 0.55f * light / 255);
        int eg = Math.round((luminance * 0.2f + 0x8a * 0.8f) * 0.55f * light / 255);
        int eb = Math.round((luminance * 0.2f + 0x54 * 0.8f) * 0.55f * light / 255);
        int expected = shouldDraw ? er << 16 | eg << 8 | eb : 0x203040;
        if (Math.abs(r - (expected >> 16 & 255)) > 1 || Math.abs(g - (expected >> 8 & 255)) > 1 || Math.abs(b - (expected & 255)) > 1) {
            throw new AssertionError("Fallback probe failed for " + description + ": " + r + "," + g + "," + b);
        }
        var preservedDepth = BufferUtils.createFloatBuffer(1);
        GL11.glReadPixels(8, 8, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, preservedDepth);
        if (shouldDraw) {
            // Unproject the written DH depth: it must hit Y=60, not far depth.
            var hit = new org.joml.Vector4f(0.0625f, 0.0625f, preservedDepth.get(0) * 2 - 1, 1);
            inverse.transform(hit);
            double worldY = hit.y / hit.w + cameraY;
            if (Math.abs(worldY - (raisedMesh == null ? 60 : 92)) > 0.5) throw new AssertionError("Surface height mismatch: " + worldY);
        } else if (Math.abs(preservedDepth.get(0) - dhDepth) > 0.00001)
            throw new AssertionError("Fallback overwrote an existing depth");
    }
}
