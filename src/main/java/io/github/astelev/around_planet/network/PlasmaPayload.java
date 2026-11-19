package io.github.astelev.around_planet.network;

import io.github.astelev.around_planet.space.SpaceVector;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;

/** Bounded, four-Hz visual samples; never sends blocks or subscribes a player to a remote ship. */
@EventBusSubscriber(modid = "around_planet")
public record PlasmaPayload(List<Sample> samples) implements CustomPacketPayload {
    public record Sample(UUID ship, SpaceVector velocity, double density) {}
    public PlasmaPayload { samples = List.copyOf(samples); if (samples.size() > 128) throw new IllegalArgumentException("Plasma sample limit"); }
    public static final Type<PlasmaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet", "plasma"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlasmaPayload> CODEC = StreamCodec.of((b, p) -> {
        b.writeVarInt(p.samples.size()); for (var s : p.samples) { b.writeUUID(s.ship); b.writeDouble(s.velocity.x()); b.writeDouble(s.velocity.y()); b.writeDouble(s.velocity.z()); b.writeDouble(s.density); }
    }, b -> {
        int count = b.readVarInt(); if (count < 0 || count > 128) throw new IllegalArgumentException("Plasma sample limit");
        var list = new ArrayList<Sample>(count);
        for (int i = 0; i < count; i++) list.add(new Sample(b.readUUID(), new SpaceVector(b.readDouble(), b.readDouble(), b.readDouble()), b.readDouble()));
        return new PlasmaPayload(list);
    });
    @Override public Type<PlasmaPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, CODEC, (p, c) -> c.enqueueWork(() -> io.github.astelev.around_planet.client.PlasmaRenderer.accept(p)));
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) || !level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                || level.getGameTime() % 5 != 0 || level.players().isEmpty() || !net.neoforged.fml.ModList.get().isLoaded("sable")) return;
        sync(level);
    }
    private static void sync(net.minecraft.server.level.ServerLevel level) {
        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level); if (container == null) return;
        var samples = new HashMap<UUID, Sample>();
        for (var ship : container.getAllSubLevels()) { var sample = io.github.astelev.around_planet.compat.PlasmaMotion.sample(ship); if (sample != null) samples.put(ship.getUniqueId(), sample); }
        for (var player : level.players()) if (player.connection.hasChannel(TYPE)) {
            var list = new ArrayList<Sample>();
            for (var entry : samples.entrySet()) {
                var ship = container.getSubLevel(entry.getKey()); var p = ship.logicalPose().position();
                if (player.distanceToSqr(p.x(), p.y(), p.z()) <= 2048.0 * 2048 || container.getPlayersTracking(ship.getPlot().getCenterChunk()).contains(player)) list.add(entry.getValue());
                if (list.size() == 128) break;
            }
            PacketDistributor.sendToPlayer(player, new PlasmaPayload(list));
        }
    }
}
