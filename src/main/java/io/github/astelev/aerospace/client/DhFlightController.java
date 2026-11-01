package io.github.astelev.aerospace.client;

import io.github.astelev.aerospace.AeroSpace;
import io.github.astelev.aerospace.compat.dh.DhFlightConfig;
import io.github.astelev.aerospace.compat.dh.DhFlightState;
import io.github.astelev.aerospace.compat.dh.FlightLodPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Arrays;
import java.util.Locale;

@EventBusSubscriber(modid = AeroSpace.MODID, value = Dist.CLIENT)
public final class DhFlightController {
    private static Settings settings;
    private static FlightLodPolicy policy;
    private static Vec3 lastPosition;
    private static long lastSampleMillis;
    private static long lastLogMillis;
    private static volatile boolean active;
    private static double speed;

    private DhFlightController() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        active = mc.level != null && mc.player != null && ModList.get().isLoaded("distanthorizons")
                && DhFlightConfig.ENABLED.getAsBoolean() && ClientFlightView.isActive(mc.level);
        if (!active) {
            DhFlightState.publish(false, 0);
            lastPosition = null;
            policy = null;
            return;
        }
        var currentSettings = new Settings(DhFlightConfig.COARSE_DETAIL.getAsInt(), DhFlightConfig.FAST_SPEED.get(),
                DhFlightConfig.QUIET_SECONDS.getAsInt(), DhFlightConfig.REFINE_SECONDS.getAsInt());
        if (policy == null || !currentSettings.equals(settings)) {
            settings = currentSettings;
            policy = new FlightLodPolicy(settings.detail, settings.speed, settings.quiet * 1000L, settings.refine * 1000L);
        }
        long now = System.nanoTime() / 1_000_000L;
        var position = mc.gameRenderer.getMainCamera().getPosition();
        speed = lastPosition == null || now <= lastSampleMillis ? 0 : position.distanceTo(lastPosition) * 1000.0 / (now - lastSampleMillis);
        lastPosition = position;
        lastSampleMillis = now;
        DhFlightState.publish(true, policy.sample(true, speed, now));
        if (DhFlightConfig.DIAGNOSTICS.getAsBoolean() && now - lastLogMillis >= 30_000) {
            lastLogMillis = now;
            AeroSpace.LOGGER.info("Flight LOD: minimum cell={} blocks, cameraSpeed={} blocks/s, DH generation gate={}, {}",
                    1 << DhFlightState.minimumDetail(), Math.round(speed), DhFlightState.recentGate(), serverTiming());
        }
    }

    @SubscribeEvent
    public static void debugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (active && DhFlightConfig.DIAGNOSTICS.getAsBoolean()) {
            event.getLeft().add("AeroSpace LOD: min cell " + (1 << DhFlightState.minimumDetail())
                    + " blocks; DH gate " + DhFlightState.recentGate());
            event.getLeft().add("AeroSpace " + serverTiming());
        }
    }

    private static String serverTiming() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return "server timing unavailable (multiplayer)";
        }
        long[] ticks = server.getTickTimesNanos().clone();
        Arrays.sort(ticks);
        double average = Arrays.stream(ticks).average().orElse(0) / 1_000_000.0;
        double p99 = ticks[Math.max(0, (int) Math.ceil(ticks.length * 0.99) - 1)] / 1_000_000.0;
        return String.format(Locale.ROOT, "server avg %.1f ms / p99 %.1f ms (DH limits: 25 / 50)", average, p99);
    }

    private record Settings(int detail, double speed, int quiet, int refine) {}
}
