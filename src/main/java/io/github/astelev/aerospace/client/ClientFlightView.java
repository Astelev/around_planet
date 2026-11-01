package io.github.astelev.aerospace.client;

import io.github.astelev.aerospace.AeroSpace;
import io.github.astelev.aerospace.flight.TemporaryChunkCache;
import io.github.astelev.aerospace.flight.TemporaryView;
import io.github.astelev.aerospace.network.TemporaryViewPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import io.github.astelev.aerospace.compat.dh.DhFlightState;

@EventBusSubscriber(modid = AeroSpace.MODID, value = Dist.CLIENT)
public final class ClientFlightView {
    private static volatile State state;

    private ClientFlightView() {
    }

    public static void accept(TemporaryViewPayload payload) {
        var current = Minecraft.getInstance().level;
        if (current == null || !current.dimension().location().equals(payload.dimension())) {
            return;
        }
        if (!payload.active()) {
            reset();
            return;
        }
        var previous = state;
        var cache = previous != null && previous.level == current
                ? previous.cache : new TemporaryChunkCache(current);
        state = new State(current, new TemporaryView(payload.x(), payload.z(), payload.radius()), cache);
    }

    public static LevelChunk get(ClientLevel requestedLevel, int x, int z) {
        var snapshot = state;
        return snapshot != null && snapshot.level == requestedLevel && snapshot.view.contains(x, z)
                ? snapshot.cache.get(x, z) : null;
    }

    public static boolean isActive(ClientLevel requestedLevel) {
        var snapshot = state;
        return snapshot != null && snapshot.level == requestedLevel;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        var snapshot = state;
        if (snapshot != null && event.getLevel() == snapshot.level) {
            reset();
        }
    }

    private static void reset() {
        state = null;
        DhFlightState.publish(false, 0);
    }

    private record State(ClientLevel level, TemporaryView view, TemporaryChunkCache cache) {
    }
}
