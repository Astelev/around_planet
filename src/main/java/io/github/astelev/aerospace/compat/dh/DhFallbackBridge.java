package io.github.astelev.aerospace.compat.dh;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
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
        var inverse = (Matrix4f) a.toJoml.invoke(null, a.inverse.get(renderParams));
        var dhProjection = (Matrix4f) a.toJoml.invoke(null, a.dhProjection.get(renderParams));
        var mcProjection = (Matrix4f) a.toJoml.invoke(null, a.mcProjection.get(renderParams));
        return new Frame(id, inverse, a.farPlane.getFloat(renderParams), dhProjection.m32() > 0 ? 0 : 1,
                mcProjection.m32() > 0 ? 0 : 1);
    }

    public static void observeBiome(Object levelWrapper, Object biomeWrapper, int x, int z) {
        if (!FallbackBiomeColors.wantsSample(x, z)) return;
        try {
            var a = access();
            Object world = a.serverLevel.invoke(levelWrapper);
            if (!(world instanceof ServerLevel level)) return;
            Object value = a.biome.get(biomeWrapper);
            if (!(value instanceof Holder<?> holder) || !(holder.value() instanceof Biome biome)) return;
            @SuppressWarnings("unchecked") var typed = (Holder<Biome>) holder;
            int color = typed.is(BiomeTags.IS_OCEAN) || typed.is(BiomeTags.IS_RIVER) ? biome.getWaterColor()
                    : biome.getBaseTemperature() < 0.15f ? 0xdce4e5 : biome.getGrassColor(x, z);
            FallbackBiomeColors.observe(level.dimension().location().toString(), x, z, color);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // An unavailable color must never fail generation; neutral color remains.
        }
    }

    public record Frame(int depthTexture, Matrix4f inverseViewProjection, float farPlane, float dhClearDepth, float vanillaClearDepth) {}

    private static final class Access {
        private final Field renderProxy, success, payload, inverse, farPlane, biome, dhProjection, mcProjection;
        private final Method depth, toJoml, serverLevel;

        private Access() throws ReflectiveOperationException {
            var apiMatrix = Class.forName("com.seibel.distanthorizons.api.objects.math.DhApiMat4f");
            renderProxy = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed").getField("renderProxy");
            var proxy = renderProxy.get(null);
            depth = proxy.getClass().getMethod("getDhDepthTextureGlId");
            var result = Class.forName("com.seibel.distanthorizons.api.objects.DhApiResult");
            success = result.getField("success");
            payload = result.getField("payload");
            var params = Class.forName("com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam");
            inverse = params.getField("dhInverseMvmProjectionMatrix");
            farPlane = params.getField("farClipPlane");
            dhProjection = params.getField("dhProjectionMatrix");
            mcProjection = params.getField("mcProjectionMatrix");
            toJoml = Class.forName("com.seibel.distanthorizons.core.util.math.DhMat4f").getMethod("createJomlMatrix", apiMatrix);
            serverLevel = Class.forName("com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper_neoforge").getMethod("getWrappedMcObject");
            biome = Class.forName("com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper_neoforge").getField("biome");
        }
    }
}
