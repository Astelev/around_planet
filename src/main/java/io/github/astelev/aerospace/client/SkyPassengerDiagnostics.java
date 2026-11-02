package io.github.astelev.aerospace.client;

import io.github.astelev.aerospace.AeroSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.UUID;

/** Observe contact loss only. Never changes attachment, movement, or collision. */
@EventBusSubscriber(modid = AeroSpace.MODID, value = Dist.CLIENT)
public final class SkyPassengerDiagnostics {
    private static Snapshot previous;
    private static long lastLog;
    private SkyPassengerDiagnostics() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !ModList.get().isLoaded("sable")) { previous = null; return; }
        boolean sky = ClientFlightView.isActive(mc.level);
        if (!sky && (previous == null || !previous.sky)) { previous = null; return; }
        var current = Native.read(mc.level, sky);
        if (previous != null && previous.level == mc.level && previous.sky && previous.ship != null
                && current.ship == null && mc.player.getVehicle() == null) {
            long now = System.nanoTime();
            if (now - lastLog >= 10_000_000_000L) {
                lastLog = now;
                AeroSpace.LOGGER.info("AeroSpace sky passenger contact lost: previousShip={}, position={}, movement={}, temporaryView={}, clientChunk={}, {}",
                        previous.ship, current.position, current.position.subtract(previous.position), sky,
                        mc.level.hasChunkAt(mc.player.blockPosition()), Native.details(previous.ship));
            }
        }
        previous = current;
    }

    private record Snapshot(ClientLevel level, boolean sky, UUID ship, Vec3 position) {}

    /** Only linked when the optional Sable mod is present. */
    private static final class Native {
        static Snapshot read(ClientLevel level, boolean sky) {
            var player = Minecraft.getInstance().player;
            var ship = dev.ryanhcode.sable.Sable.HELPER.getTrackingSubLevel(player);
            return new Snapshot(level, sky, ship == null ? null : ship.getUniqueId(), player.position());
        }
        static String details(UUID previousShip) {
            var player = Minecraft.getInstance().player;
            var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(player.level());
            var ship = container == null ? null : container.getSubLevel(previousShip);
            var movement = (dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension) player;
            return "shipPresent=" + (ship != null && !ship.isRemoved()) + ", lastNativeShip=" + movement.sable$getLastTrackingSubLevelID()
                    + ", onGround=" + player.onGround() + ", velocity=" + player.getDeltaMovement();
        }
    }
}
