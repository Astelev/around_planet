package io.github.astelev.aerospace.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import io.github.astelev.aerospace.compat.dh.FallbackSurfaceMesh;
import io.github.astelev.aerospace.AeroSpace;
import io.github.astelev.aerospace.compat.dh.DhFallbackBridge;
import io.github.astelev.aerospace.compat.dh.DhFlightConfig;
import io.github.astelev.aerospace.compat.dh.FallbackBiomeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.io.IOException;

@EventBusSubscriber(modid = AeroSpace.MODID, value = Dist.CLIENT)
public final class FallbackPlaneRenderer {
    private static ShaderInstance shader;
    private static DynamicTexture palette;
    private static VertexBuffer surfaceMesh;
    private static boolean meshHeights;
    private static float meshExtent;
    private static long lastPaletteMillis;
    private static long paletteGeneration = -1;
    private static int originCellX, originCellZ;
    private static boolean failed;
    private static long frames;
    private static int depthCopy;
    private static int depthWidth, depthHeight;
    private static String lastSkip = "not called";

    private FallbackPlaneRenderer() {}

    @SubscribeEvent
    public static void registerShader(RegisterShadersEvent event) throws IOException {
        if (ModList.get().isLoaded("distanthorizons")) {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(AeroSpace.MODID, "fallback_plane"), DefaultVertexFormat.POSITION), value -> {
                shader = value;
                failed = false;
            });
        }
    }

    public static void render(Object renderParams) {
        var mc = Minecraft.getInstance();
        if (failed || shader == null || !FallbackBiomeSampler.surfaceEnabled(mc.level)) { lastSkip = "inactive"; return; }
        try {
            var frame = DhFallbackBridge.frame(renderParams);
            if (frame == null || frame.depthTexture() == mc.getMainRenderTarget().getDepthTextureId()) { lastSkip = "missing/shared DH depth"; return; }
            int attachment = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
            if (attachment != frame.depthTexture() || frame.lightmap() <= 0) {
                lastSkip = "unsupported DH target/lightmap";
                return;
            }
            var camera = frame.cameraPosition();
            if (camera.y <= DhFlightConfig.FALLBACK_HEIGHT.getAsInt()) { lastSkip = "camera " + camera.y; return; }
            updatePalette(camera, frame.farPlane());
            int[] viewport = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int copiedDepth = copyDepth(viewport[0], viewport[1], viewport[2], viewport[3]);
            drawPass(frame.viewProjection(), camera, DhFlightConfig.FALLBACK_HEIGHT.getAsInt(),
                    frame.farPlane(), copiedDepth, palette.getId(),
                    originCellX * (float) FallbackBiomeColors.CELL_SIZE,
                    originCellZ * (float) FallbackBiomeColors.CELL_SIZE,
                    FallbackBiomeColors.WIDTH * (float) FallbackBiomeColors.CELL_SIZE,
                    DhFlightConfig.FALLBACK_COLOR.getAsInt(), frame.dhClearDepth(), frame.lightmap(), mc.getMainRenderTarget().getDepthTextureId(), frame.vanillaClearDepth(), DhFlightConfig.FALLBACK_BRIGHTNESS.get().floatValue(), surfaceMesh);
            frames++;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            AeroSpace.LOGGER.error("Fallback plane disabled after render failure; normal DH rendering is unchanged", e);
        }
    }

    public static long drawnFrames() { return frames; }
    public static String lastSkip() { return lastSkip; }

    private static void updatePalette(Vec3 camera, float extent) {
        var mc = Minecraft.getInstance();
        int centerX = Math.floorDiv((int) Math.floor(camera.x), FallbackBiomeColors.CELL_SIZE);
        int centerZ = Math.floorDiv((int) Math.floor(camera.z), FallbackBiomeColors.CELL_SIZE);
        int newX = centerX - FallbackBiomeColors.HALF_SIZE;
        int newZ = centerZ - FallbackBiomeColors.HALF_SIZE;
        if (DhFlightConfig.FALLBACK_BIOMES.getAsBoolean()) {
            FallbackBiomeColors.setView(mc.level.dimension().location().toString(), (int) Math.floor(camera.x), (int) Math.floor(camera.z));
        } else {
            FallbackBiomeColors.clear();
        }
        long now = System.nanoTime() / 1_000_000L;
        if (palette != null && paletteGeneration == FallbackBiomeColors.generation()
                && meshHeights == DhFlightConfig.FALLBACK_HEIGHTS.getAsBoolean() && meshExtent == extent
                && now - lastPaletteMillis < 200 && newX == originCellX && newZ == originCellZ) return;
        int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE));
        if (palette == null) palette = new DynamicTexture(FallbackBiomeColors.WIDTH, FallbackBiomeColors.WIDTH, false);
        originCellX = newX;
        originCellZ = newZ;
        int fallback = DhFlightConfig.FALLBACK_COLOR.getAsInt();
        var pixels = palette.getPixels();
        int[][] heights = new int[FallbackBiomeColors.WIDTH][FallbackBiomeColors.WIDTH];
        for (int x = 0; x < FallbackBiomeColors.WIDTH; x++) {
            for (int z = 0; z < FallbackBiomeColors.WIDTH; z++) {
                int rgb = FallbackBiomeColors.color(originCellX + x, originCellZ + z, fallback);
                int abgr = 0xff000000 | (rgb & 255) << 16 | (rgb & 0xff00) | rgb >>> 16 & 255;
                pixels.setPixelRGBA(x, z, abgr);
                if (DhFlightConfig.FALLBACK_HEIGHTS.getAsBoolean())
                    heights[x][z] = FallbackBiomeColors.heightOffset(originCellX + x, originCellZ + z);
            }
        }
        // Preserve bindings: DH/vanilla cache texture state between render passes.
        RenderSystem.bindTexture(palette.getId());
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, palette.getId());
        palette.upload();
        RenderSystem.bindTexture(binding);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, binding);
        int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int array = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            BufferUploader.reset();
            if (surfaceMesh == null) surfaceMesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
            uploadSurface(surfaceMesh, heights, extent);
        } finally {
            BufferUploader.reset();
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, array);
        }
        meshHeights = DhFlightConfig.FALLBACK_HEIGHTS.getAsBoolean();
        meshExtent = extent;
        lastPaletteMillis = now;
        paletteGeneration = FallbackBiomeColors.generation();
    }

    /** Cached GPU geometry refreshes with the palette, never with every frame. */
    public static void uploadSurface(VertexBuffer target, int[][] heights, float extent) {
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        FallbackSurfaceMesh.build(heights, FallbackBiomeColors.CELL_SIZE, extent,
                (x0,y0,z0, x1,y1,z1, x2,y2,z2, x3,y3,z3) -> {
                    buffer.addVertex(x0,y0,z0); buffer.addVertex(x1,y1,z1);
                    buffer.addVertex(x2,y2,z2); buffer.addVertex(x3,y3,z3);
                });
        target.bind();
        target.upload(buffer.buildOrThrow());
    }

    /** Copy on GPU: sampling the depth attached to the draw target would be feedback. */
    private static int copyDepth(int x, int y, int width, int height) {
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        try {
            if (depthCopy == 0) depthCopy = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthCopy);
            if (width != depthWidth || height != depthHeight) {
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, width, height, 0,
                        GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (java.nio.ByteBuffer) null);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                depthWidth = width;
                depthHeight = height;
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, x, y, width, height);
            return depthCopy;
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }

    /** Also exercised against small offscreen depth buffers by the opt-in client smoke test. */
    public static void drawPass(Matrix4f viewProjection, Vec3 camera, float height, float maxDistance,
                                int dhDepth, int colorTexture, float originX, float originZ, float span,
                                int fallback, float dhClearDepth, int lightmap, int vanillaDepth, float vanillaClearDepth, float brightness) {
        drawPass(viewProjection, camera, height, maxDistance, dhDepth, colorTexture, originX, originZ, span,
                fallback, dhClearDepth, lightmap, vanillaDepth, vanillaClearDepth, brightness, null);
    }

    public static void drawPass(Matrix4f viewProjection, Vec3 camera, float height, float maxDistance,
                                int dhDepth, int colorTexture, float originX, float originZ, float span,
                                int fallback, float dhClearDepth, int lightmap, int vanillaDepth, float vanillaClearDepth, float brightness, VertexBuffer mesh) {
        if (shader == null) throw new IllegalStateException("Fallback shader not loaded");
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int sampler0 = GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, 0);
        int sampler1 = GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, 1);
        int sampler2 = GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, 2);
        int sampler3 = GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, 3);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int texture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int texture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        int texture2 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        int texture3 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(activeTexture);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        var colorMask = org.lwjgl.BufferUtils.createByteBuffer(4);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        var previousShader = RenderSystem.getShader();
        try {
            // DH uses raw GL, so invalidate vanilla's cached immediate VAO and
            // synchronize sampler bindings before ShaderInstance applies them.
            BufferUploader.reset();
            GL33.glBindSampler(0, 0);
            GL33.glBindSampler(1, 0);
            GL33.glBindSampler(2, 0);
            GL33.glBindSampler(3, 0);
            for (int unit = 0; unit < 4; unit++) {
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
                RenderSystem.bindTexture(0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            }
            shader.getUniform("SurfaceOffset").set(mesh == null ? 0 : (float) (originX - camera.x),
                    mesh == null ? 0 : (float) (height - camera.y), mesh == null ? 0 : (float) (originZ - camera.z));
            shader.getUniform("CameraPosition").set((float) camera.x, (float) camera.y, (float) camera.z);
            shader.getUniform("MaxDistance").set(Math.max(128, maxDistance));
            shader.getUniform("ColorBrightness").set(brightness);
            shader.getUniform("DhViewProjection").set(viewProjection);
            shader.getUniform("PaletteOrigin").set(originX, originZ);
            shader.getUniform("PaletteSpan").set(span);
            shader.getUniform("FallbackColor").set((fallback >> 16 & 255) / 255f, (fallback >> 8 & 255) / 255f, (fallback & 255) / 255f);
            shader.getUniform("DhClearDepth").set(dhClearDepth);
            shader.getUniform("VanillaClearDepth").set(vanillaClearDepth);
            shader.setSampler("DhDepth", dhDepth);
            shader.setSampler("BiomePalette", colorTexture);
            shader.setSampler("SurfaceLightmap", lightmap);
            shader.setSampler("VanillaDepth", vanillaDepth);
            // Only clear DH pixels survive the copied-depth mask. Real LODs,
            // including terrain below Y=60, remain authoritative.
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(dhClearDepth == 0 ? GL11.GL_GREATER : GL11.GL_LESS);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glColorMask(true, true, true, true);
            RenderSystem.setShader(() -> shader);
            if (mesh != null) {
                shader.apply();
                mesh.bind();
                mesh.draw();
                shader.clear();
            } else {
                // Same camera-relative coordinates as DH's terrain VBOs. A true
                // horizontal quad avoids assumptions about inverse screen rays.
                float y = (float) (height - camera.y);
                float radius = Math.max(128, maxDistance);
                if (y >= 0) return;
                var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                buffer.addVertex(-radius, y, -radius);
                buffer.addVertex(-radius, y, radius);
                buffer.addVertex(radius, y, radius);
                buffer.addVertex(radius, y, -radius);
                BufferUploader.drawWithShader(buffer.buildOrThrow());
            }
        } finally {
            BufferUploader.reset();
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture0);
            RenderSystem.bindTexture(texture0);
            RenderSystem.activeTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture1);
            RenderSystem.bindTexture(texture1);
            RenderSystem.activeTexture(GL13.GL_TEXTURE2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2);
            RenderSystem.bindTexture(texture2);
            RenderSystem.activeTexture(GL13.GL_TEXTURE3);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture3);
            RenderSystem.bindTexture(texture3);
            RenderSystem.activeTexture(activeTexture);
            GL33.glBindSampler(0, sampler0);
            GL33.glBindSampler(1, sampler1);
            GL33.glBindSampler(2, sampler2);
            GL33.glBindSampler(3, sampler3);
            GL11.glDepthFunc(depthFunc);
            GL11.glDepthMask(depthMask);
            set(GL11.GL_DEPTH_TEST, depth);
            set(GL11.GL_BLEND, blend);
            set(GL11.GL_CULL_FACE, cull);
            set(GL11.GL_SCISSOR_TEST, scissor);
            set(GL11.GL_STENCIL_TEST, stencil);
            GL11.glColorMask(colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
            RenderSystem.setShader(() -> previousShader);
            GL20.glUseProgram(program);
        }
    }

    private static void set(int cap, boolean enabled) {
        if (enabled) GL11.glEnable(cap); else GL11.glDisable(cap);
    }
}
