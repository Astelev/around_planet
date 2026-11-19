package io.github.astelev.around_planet.compat;

import io.github.astelev.around_planet.loader.ShipLoaderBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.*;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import java.util.*;

/** Persistent native tickets. Only this block's keys are reconciled; other loaders remain untouched. */
public final class ShipLoaderBridge {
    public static final SubLevelLoadingTicketType<String> TICKET = SubLevelLoadingTicketType.create(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("around_planet", "ship_loader"),
            com.mojang.serialization.Codec.STRING);

    private static final class State {
        final Set<ShipLoaderBlockEntity> blocks = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<ServerSubLevel> indexed = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<ServerSubLevel, Set<String>> owned = new IdentityHashMap<>();
        boolean updating;
    }
    private static final Map<ServerLevel, State> STATES = new WeakHashMap<>();
    private static final Set<MinecraftServer> STOPPING = Collections.newSetFromMap(new WeakHashMap<>());

    /** Also initializes TICKET before Sable decodes its saved force-loading tickets. */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(ShipLoaderBridge::unload);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> STOPPING.add(e.getServer()));
    }
    private static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) STATES.remove(level);
    }
    public static void observe(ShipLoaderBlockEntity be) {
        if (be.getLevel() instanceof ServerLevel level && !be.isRemoved())
            STATES.computeIfAbsent(level, k -> new State()).blocks.add(be);
    }
    public static void forget(ShipLoaderBlockEntity be) {
        if (be.getLevel() instanceof ServerLevel level) {
            var state = STATES.get(level);
            if (state != null) state.blocks.remove(be);
        }
    }
    public static void removed(ShipLoaderBlockEntity be) {
        forget(be);
        if (be.getLevel() instanceof ServerLevel level) before(SubLevelContainer.getContainer(level));
    }
    public static void refresh(ShipLoaderBlockEntity be) {
        observe(be);
        if (be.getLevel() instanceof ServerLevel level) before(SubLevelContainer.getContainer(level));
    }
    private static boolean current(ServerSubLevel ship, ShipLoaderBlockEntity be) {
        // Plot holders take relative chunk coordinates, but their block entities use global plot positions.
        var holder = ship.getPlot().getChunkHolder(ship.getPlot().toLocal(new ChunkPos(be.getBlockPos())));
        return holder != null && holder.getChunk() != null
                && holder.getChunk().getBlockEntity(be.getBlockPos(), LevelChunk.EntityCreationType.CHECK) == be;
    }

    /** Runs before native physics decides which bodies/chunks to unload. No terrain chunk lookups. */
    public static void before(ServerSubLevelContainer container) {
        if (container == null || container.getLevel().getServer().isStopped()
                || STOPPING.contains(container.getLevel().getServer())) return;
        var level = container.getLevel();
        var state = STATES.computeIfAbsent(level, k -> new State());
        if (state.updating) return;
        state.updating = true;
        try {
            var ships = Set.copyOf(container.getAllSubLevels());
            state.indexed.removeIf(s -> s.isRemoved() || !ships.contains(s));
            // Restored native plots may not have run ordinary block tickers yet. Index once per body.
            for (var ship : ships) if (!ship.isRemoved() && state.indexed.add(ship))
                for (var actor : ship.getPlot().getBlockEntityActors())
                    if ((Object) actor instanceof ShipLoaderBlockEntity be) observe(be);

            // Adopt tickets restored by Sable, so stale keys can be retired after restart/transfer.
            for (var entry : container.collectForceLoadTickets().entrySet())
                for (var ticket : entry.getValue())
                    if (ticket.type().equals(TICKET) && ticket.key() instanceof String key)
                        state.owned.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(key);

            var roots = new IdentityHashMap<ServerSubLevel, Set<String>>();
            var blocks = state.blocks.iterator();
            while (blocks.hasNext()) {
                var be = blocks.next();
                if (be.isRemoved() || be.getLevel() != level) { blocks.remove(); continue; }
                if (!be.enabled()) continue;
                var carrier = Sable.HELPER.getContaining(level, be.getBlockPos());
                if (carrier instanceof ServerSubLevel ship && !ship.isRemoved() && current(ship, be))
                    roots.computeIfAbsent(ship, k -> new HashSet<>()).add(be.loaderId());
            }

            var desired = new IdentityHashMap<ServerSubLevel, Set<String>>();
            if (!roots.isEmpty()) for (var group : ShipAssemblies.groups(level, ships)) {
                var keys = new HashSet<String>();
                for (var ship : group.ships()) keys.addAll(roots.getOrDefault(ship, Set.of()));
                if (!keys.isEmpty()) for (var ship : group.ships())
                    if (!ship.isRemoved()) desired.put(ship, new HashSet<>(keys));
            }
            for (var entry : desired.entrySet()) for (var key : entry.getValue())
                if (!state.owned.getOrDefault(entry.getKey(), Set.of()).contains(key)) {
                    container.addForceLoadTicket(entry.getKey(), TICKET, key);
                    state.owned.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(key);
                }

            // Keep removed body references until here: dimensional departure must clear source UUIDs too.
            var owners = state.owned.entrySet().iterator();
            while (owners.hasNext()) {
                var entry = owners.next();
                var keys = entry.getValue().iterator();
                while (keys.hasNext()) {
                    var key = keys.next();
                    if (!desired.getOrDefault(entry.getKey(), Set.of()).contains(key)) {
                        container.removeForceLoadTicket(entry.getKey(), TICKET, key);
                        keys.remove();
                    }
                }
                if (entry.getValue().isEmpty()) owners.remove();
            }
        } finally { state.updating = false; }
    }
    private ShipLoaderBridge() {}
}
