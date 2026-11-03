package io.github.astelev.cruising.client;

import io.github.astelev.cruising.CruisingSpeed;
import io.github.astelev.cruising.compat.dh.DhFlightConfig;
import io.github.astelev.cruising.compat.dh.DhFallbackBridge;
import io.github.astelev.cruising.compat.dh.FallbackBiomeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.core.QuartPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Latest-view-only biome sampling. Never asks the chunk system for terrain. */
@EventBusSubscriber(modid = CruisingSpeed.MODID, value = Dist.CLIENT)
public final class FallbackBiomeSampler {
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task, "CruisingSpeed biome colors");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private static ClientLevel sampledLevel;
    private static volatile long samples;
    private static volatile long failedGeneration = -1;

    private FallbackBiomeSampler() {}
    public static long samples() { return samples; }

    public static boolean surfaceEnabled(ClientLevel level) {
        return level != null && level.dimension().equals(Level.OVERWORLD) && ModList.get().isLoaded("distanthorizons")
                && DhFlightConfig.ENABLED.getAsBoolean() && DhFlightConfig.FALLBACK_PLANE.getAsBoolean();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clearWorld(); }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() == sampledLevel) clearWorld();
    }

    private static void clearWorld() {
        sampledLevel = null;
        FallbackBiomeColors.clear();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (sampledLevel != mc.level) {
            FallbackBiomeColors.clear();
            sampledLevel = mc.level;
        }
        if (!surfaceEnabled(mc.level) || !DhFlightConfig.FALLBACK_BIOMES.getAsBoolean()) return;
        var camera = mc.gameRenderer.getMainCamera().getPosition();
        FallbackBiomeColors.setView(mc.level.dimension().location().toString(), (int) Math.floor(camera.x), (int) Math.floor(camera.z));
        // The integrated server has the seed/sampler. Remote servers do not reveal it;
        // those clients retain neutral cells and can still observe DH biome results.
        var server = mc.getSingleplayerServer();
        var level = server == null ? null : server.getLevel(mc.level.dimension());
        if (level == null || failedGeneration == FallbackBiomeColors.generation() || !BUSY.compareAndSet(false, true)) return;
        var batch = FallbackBiomeColors.missingNearest(64);
        if (batch.cells().isEmpty()) { BUSY.set(false); return; }
        var generator = level.getChunkSource().getGenerator();
        var sampler = level.getChunkSource().randomState().sampler();
        int height = DhFlightConfig.FALLBACK_HEIGHT.getAsInt();
        int seaLevel = level.getSeaLevel();
        WORKER.execute(() -> {
            try {
                long deadline = System.nanoTime() + 2_000_000L;
                for (var cell : batch.cells()) {
                    if (batch.generation() != FallbackBiomeColors.generation()) break;
                    int x = cell.x() * FallbackBiomeColors.CELL_SIZE + FallbackBiomeColors.CELL_SIZE / 2;
                    int z = cell.z() * FallbackBiomeColors.CELL_SIZE + FallbackBiomeColors.CELL_SIZE / 2;
                    var biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(height),
                            QuartPos.fromBlock(z), sampler);
                    FallbackBiomeColors.observe(batch.generation(), batch.dimension(), x, z,
                            DhFallbackBridge.biomeColor(biome, x, z), DhFallbackBridge.biomeHeightOffset(biome,seaLevel,height),DhFallbackBridge.waterSurface(biome));
                    samples++;
                    // Cooperative budget: no queued backlog, one small batch per client tick.
                    // One in-flight biome lookup cannot be interrupted; the next one is deferred.
                    if (System.nanoTime() >= deadline) break;
                }
            } catch (RuntimeException e) {
                failedGeneration = batch.generation();
                CruisingSpeed.LOGGER.warn("Biome palette sampling stopped; neutral colors remain available", e);
            } finally {
                BUSY.set(false);
            }
        });
    }
}
