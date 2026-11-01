package io.github.astelev.aerospace.gametest;

import io.github.astelev.aerospace.AeroSpace;
import io.github.astelev.aerospace.compat.dh.DhFlightState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.concurrent.atomic.AtomicReference;

/** Opt-in development source set: verifies the actual transformed DH client classes. */
@EventBusSubscriber(modid = AeroSpace.MODID, value = Dist.CLIENT)
public final class DhClientSmoke {
    private static int ticks;
    private static boolean finished;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (finished) return;
        var mc = Minecraft.getInstance();
        ticks++;
        if (ticks > 2400) throw new AssertionError("AEROSPACE_DH_SMOKE: timeout waiting for the test world");
        if (mc.level == null || mc.player == null || ticks < 100) return;
        try {
            var wrapperType = Class.forName("com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper_neoforge");
            var wrapper = wrapperType.getMethod("getWrapper", ClientLevel.class).invoke(null, mc.level);
            var level = wrapper.getClass().getMethod("getDhLevel").invoke(wrapper);
            if (level == null) return;
            var module = level.getClass().getField("clientside").get(level);
            var ref = (AtomicReference<?>) module.getClass().getField("ClientRenderStateRef").get(module);
            var renderState = ref.get();
            if (renderState == null) return;
            var tree = renderState.getClass().getField("quadtree").get(renderState);
            var detail = tree.getClass().getDeclaredMethod("calcDetailLevelFromDistance", double.class, byte.class);
            detail.setAccessible(true);
            DhFlightState.publish(false, 0);
            byte original = (Byte) detail.invoke(tree, 1.0, (byte) 0);
            if (original >= 4) return; // Wait for the first quadtree tick.
            DhFlightState.publish(true, 4);
            require((Byte) detail.invoke(tree, 1.0, (byte) 0) == 4, "Coarse floor did not apply to real DH quadtree");
            DhFlightState.publish(false, 0);
            require((Byte) detail.invoke(tree, 1.0, (byte) 0) == original, "Original detail did not return on exit");

            var apiType = Class.forName("com.seibel.distanthorizons.core.api.internal.ClientApi");
            var api = apiType.getField("INSTANCE").get(null);
            var averageField = apiType.getDeclaredField("cameraSpeedRollingAverage");
            averageField.setAccessible(true);
            var average = averageField.get(api);
            var add = average.getClass().getMethod("add", double.class);
            for (int i = 0; i < 40; i++) add.invoke(average, 100.0);
            var configType = Class.forName("com.seibel.distanthorizons.core.config.Config$Common$WorldGenerator");
            var pause = configType.getField("pauseIfMovingQuickly").get(null);
            var get = pause.getClass().getMethod("get");
            var set = pause.getClass().getMethod("setWithoutSaving", Object.class);
            var saved = get.invoke(pause);
            var pools = Class.forName("com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil");
            var moving = pools.getDeclaredMethod("cameraMovingFast");
            moving.setAccessible(true);
            try {
                set.invoke(pause, true);
                DhFlightState.publish(false, 0);
                require((Boolean) moving.invoke(null), "Native DH motion pause was not exercised");
                DhFlightState.publish(true, 4);
                require(!(Boolean) moving.invoke(null), "Flight motion-pause bypass did not apply");
                // This must still run the native server/queue checks, not force true.
                pools.getMethod("worldGenThreadsCanRun").invoke(null);
                require(DhFlightState.recentGate() != DhFlightState.Reason.UNKNOWN, "DH gate diagnostics did not run");
                DhFlightState.publish(false, 0);
                require((Boolean) moving.invoke(null), "Native DH motion pause did not return on exit");
            } finally {
                set.invoke(pause, saved);
                DhFlightState.publish(false, 0);
            }
            finished = true;
            AeroSpace.LOGGER.info("AEROSPACE_DH_SMOKE PASS: real client quadtree floor, exit restoration, motion pause scope and native gate diagnostics");
            mc.stop();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("AEROSPACE_DH_SMOKE failed", e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("AEROSPACE_DH_SMOKE: " + message);
    }
}
