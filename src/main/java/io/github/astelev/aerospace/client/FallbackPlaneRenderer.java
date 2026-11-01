package io.github.astelev.aerospace.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
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
    private static long lastPaletteMillis;
    private static long paletteGeneration = -1;
    private static int originCellX, originCellZ;
    private static boolean failed;
    private static long frames;
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
        if (failed || shader == null || mc.level == null || !ClientFlightView.isActive(mc.level)
                || !DhFlightConfig.ENABLED.getAsBoolean() || !DhFlightConfig.FALLBACK_PLANE.getAsBoolean()) { lastSkip = "inactive"; return; }
        // Unknown shader-pack destinations are left untouched. MC and DH can
        // use different depth directions; infer each from its own projection.
        if (GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) != mc.getMainRenderTarget().frameBufferId) {
            lastSkip = "destination " + GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) + "/" + mc.getMainRenderTarget().frameBufferId
                    + ", clear=" + GL11.glGetFloat(GL11.GL_DEPTH_CLEAR_VALUE);
            return;
        }
        try {
            var frame = DhFallbackBridge.frame(renderParams);
            if (frame == null || frame.depthTexture() == mc.getMainRenderTarget().getDepthTextureId()) { lastSkip = "missing/shared DH depth"; return; }
            var camera = mc.gameRenderer.getMainCamera().getPosition();
            if (camera.y <= DhFlightConfig.FALLBACK_HEIGHT.getAsInt()) { lastSkip = "camera " + camera.y; return; }
            updatePalette(camera);
            float[] fog = RenderSystem.getShaderFogColor();
            drawPass(frame.inverseViewProjection(), camera, DhFlightConfig.FALLBACK_HEIGHT.getAsInt(),
                    frame.farPlane(), frame.depthTexture(), palette.getId(),
                    originCellX * (float) FallbackBiomeColors.CELL_SIZE,
                    originCellZ * (float) FallbackBiomeColors.CELL_SIZE,
                    FallbackBiomeColors.WIDTH * (float) FallbackBiomeColors.CELL_SIZE,
                    DhFlightConfig.FALLBACK_COLOR.getAsInt(), fog[0], fog[1], fog[2], frame.dhClearDepth(), frame.vanillaClearDepth());
            frames++;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            AeroSpace.LOGGER.error("Fallback plane disabled after render failure; normal DH rendering is unchanged", e);
        }
    }

    public static long drawnFrames() { return frames; }
    public static String lastSkip() { return lastSkip; }

    private static void updatePalette(Vec3 camera) {
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
                && now - lastPaletteMillis < 200 && newX == originCellX && newZ == originCellZ) return;
        int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE));
        if (palette == null) palette = new DynamicTexture(FallbackBiomeColors.WIDTH, FallbackBiomeColors.WIDTH, false);
        originCellX = newX;
        originCellZ = newZ;
        int fallback = DhFlightConfig.FALLBACK_COLOR.getAsInt();
        var pixels = palette.getPixels();
        for (int x = 0; x < FallbackBiomeColors.WIDTH; x++) {
            for (int z = 0; z < FallbackBiomeColors.WIDTH; z++) {
                int rgb = FallbackBiomeColors.color(originCellX + x, originCellZ + z, fallback);
                int abgr = 0xff000000 | (rgb & 255) << 16 | (rgb & 0xff00) | rgb >>> 16 & 255;
                pixels.setPixelRGBA(x, z, abgr);
            }
        }
        // Preserve bindings: DH/vanilla cache texture state between render passes.
        RenderSystem.bindTexture(palette.getId());
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, palette.getId());
        palette.upload();
        RenderSystem.bindTexture(binding);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, binding);
        lastPaletteMillis = now;
        paletteGeneration = FallbackBiomeColors.generation();
    }

    /** Also exercised against small offscreen depth buffers by the opt-in client smoke test. */
    public static void drawPass(Matrix4f inverse, Vec3 camera, float height, float fogDistance,
                                int dhDepth, int colorTexture, float originX, float originZ, float span,
                                int fallback, float fogR, float fogG, float fogB, float dhClearDepth, float vanillaClearDepth) {
        if (shader == null) throw new IllegalStateException("Fallback shader not loaded");
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int sampler0 = GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, 0);
        int sampler1 = GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, 1);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int texture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int texture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
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
            for (int unit = 0; unit < 2; unit++) {
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
                RenderSystem.bindTexture(0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            }
            shader.getUniform("InverseDhViewProjection").set(inverse);
            shader.getUniform("CameraPosition").set((float) camera.x, (float) camera.y, (float) camera.z);
            shader.getUniform("PlaneHeight").set(height);
            shader.getUniform("FogDistance").set(Math.max(128, fogDistance));
            shader.getUniform("PaletteOrigin").set(originX, originZ);
            shader.getUniform("PaletteSpan").set(span);
            shader.getUniform("FallbackColor").set((fallback >> 16 & 255) / 255f, (fallback >> 8 & 255) / 255f, (fallback & 255) / 255f);
            shader.getUniform("PlaneFogColor").set(fogR, fogG, fogB);
            shader.getUniform("DhClearDepth").set(dhClearDepth);
            shader.getUniform("VanillaClearDepth").set(vanillaClearDepth);
            shader.setSampler("DhDepth", dhDepth);
            shader.setSampler("BiomePalette", colorTexture);
            // The fragment shader emits far depth. Equal-to-clear admits ONLY
            // pixels without vanilla geometry; its DH texture test excludes LODs.
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_EQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glColorMask(true, true, true, true);
            RenderSystem.setShader(() -> shader);
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            buffer.addVertex(-1, -1, 0);
            buffer.addVertex(1, -1, 0);
            buffer.addVertex(1, 1, 0);
            buffer.addVertex(-1, 1, 0);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
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
            RenderSystem.activeTexture(activeTexture);
            GL33.glBindSampler(0, sampler0);
            GL33.glBindSampler(1, sampler1);
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
