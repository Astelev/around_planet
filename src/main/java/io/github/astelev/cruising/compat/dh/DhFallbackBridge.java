package io.github.astelev.cruising.compat.dh;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Lazily linked to the optional DH 3.3.3 classes; never loads them without DH. */
public final class DhFallbackBridge {
    private static Access access;

    private DhFallbackBridge() {}

    private static synchronized Access access() throws ReflectiveOperationException {
        if (access == null) access = new Access();
        return access;
    }

    public static Frame frame(Object renderParams) throws ReflectiveOperationException {
        var a = access();
        Object result = a.depth.invoke(a.renderProxy.get(null));
        if (!a.success.getBoolean(result) || !(a.payload.get(result) instanceof Integer id) || id <= 0) return null;
        var viewProjection = (Matrix4f) a.toJoml.invoke(null, a.combined.get(renderParams));
        var dhProjection = (Matrix4f) a.toJoml.invoke(null, a.dhProjection.get(renderParams));
        var mcProjection = (Matrix4f) a.toJoml.invoke(null, a.mcProjection.get(renderParams));
        Object camera = a.camera.get(renderParams);
        var position = new net.minecraft.world.phys.Vec3(a.cameraX.getDouble(camera), a.cameraY.getDouble(camera), a.cameraZ.getDouble(camera));
        return new Frame(id, viewProjection, a.farPlane.getFloat(renderParams), dhProjection.m32() > 0 ? 0 : 1,
                (Integer) a.lightmapId.invoke(a.lightmap.get(renderParams)), mcProjection.m32() > 0 ? 0 : 1, position);
    }

    public static void observeBiome(Object levelWrapper, Object biomeWrapper, int x, int z) {
        if (!FallbackBiomeColors.wantsSample(x, z)) return;
        try {
            var a = access();
            Object world = a.serverLevel.invoke(levelWrapper);
            if (!(world instanceof ServerLevel level)) return;
            Object value = a.biome.get(biomeWrapper);
            if (!(value instanceof Holder<?> holder) || !(holder.value() instanceof Biome)) return;
            @SuppressWarnings("unchecked") var typed = (Holder<Biome>) holder;
            int color = biomeColor(typed, x, z);
            FallbackBiomeColors.observe(level.dimension().location().toString(), x, z, color, biomeHeightOffset(typed, level.getSeaLevel(), DhFlightConfig.FALLBACK_HEIGHT.getAsInt()),waterSurface(typed));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // An unavailable color must never fail generation; neutral color remains.
        }
    }

    public static int biomeColor(Holder<Biome> holder, int x, int z) {
        var biome = holder.value();
        if (holder.is(Biomes.DESERT) || holder.is(BiomeTags.IS_BEACH))
            return holder.is(Biomes.STONY_SHORE) ? 0x858585 : 0xd9c58c;
        if (holder.is(BiomeTags.IS_BADLANDS)) return 0xb66b44;
        return holder.is(BiomeTags.IS_OCEAN) || holder.is(BiomeTags.IS_RIVER)
                ? biome.getWaterColor() : biome.getBaseTemperature() < 0.15f ? 0xdce4e5 : biome.getGrassColor(x, z);
    }

    public static int biomeHeightOffset(Holder<Biome> biome, int seaLevel, int baseHeight) {
        if (waterSurface(biome))
            return BiomeSurfaceHeights.seaOffset(seaLevel,baseHeight);
        return BiomeSurfaceHeights.offset(biome.unwrapKey().map(key -> key.location().toString()).orElse(null),
                biome.is(BiomeTags.IS_MOUNTAIN), biome.is(BiomeTags.IS_HILL));
    }

    public static boolean waterSurface(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH);
    }

    public record Frame(int depthTexture, Matrix4f viewProjection, float farPlane, float dhClearDepth, int lightmap, float vanillaClearDepth, net.minecraft.world.phys.Vec3 cameraPosition) {}

    private static final class Access {
        private final Field renderProxy, success, payload, combined, farPlane, biome, dhProjection, lightmap, mcProjection, camera, cameraX, cameraY, cameraZ;
        private final Method depth, toJoml, serverLevel, lightmapId;

        private Access() throws ReflectiveOperationException {
            var apiMatrix = Class.forName("com.seibel.distanthorizons.api.objects.math.DhApiMat4f");
            renderProxy = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed").getField("renderProxy");
            var proxy = renderProxy.get(null);
            depth = proxy.getClass().getMethod("getDhDepthTextureGlId");
            var result = Class.forName("com.seibel.distanthorizons.api.objects.DhApiResult");
            success = result.getField("success");
            payload = result.getField("payload");
            var params = Class.forName("com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam");
            combined = params.getField("dhMvmProjMatrix");
            farPlane = params.getField("farClipPlane");
            dhProjection = params.getField("dhProjectionMatrix");
            mcProjection = params.getField("mcProjectionMatrix");
            var internalParams = Class.forName("com.seibel.distanthorizons.core.render.RenderParams");
            lightmap = internalParams.getField("lightmap");
            camera = internalParams.getField("exactCameraPosition");
            cameraX = camera.getType().getField("x");
            cameraY = camera.getType().getField("y");
            cameraZ = camera.getType().getField("z");
            lightmapId = Class.forName("com.seibel.distanthorizons.common.wrappers.misc.LightMapWrapper_neoforge").getMethod("getOpenGlId");
            toJoml = Class.forName("com.seibel.distanthorizons.core.util.math.DhMat4f").getMethod("createJomlMatrix", apiMatrix);
            serverLevel = Class.forName("com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper_neoforge").getMethod("getWrappedMcObject");
            biome = Class.forName("com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper_neoforge").getField("biome");
        }
    }
}
