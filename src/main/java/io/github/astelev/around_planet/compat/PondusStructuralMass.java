package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** One native rebuild after multiblock topology edits, never a periodic fleet scan. */
public final class PondusStructuralMass {
    private static final Map<ServerLevel, Set<ServerSubLevel>> DIRTY = new IdentityHashMap<>();
    public static void init() {
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(PondusStructuralMass::tick);
        bus.addListener(PondusStructuralMass::stopping);
    }
    private static boolean multiblock(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals("create") &&
                (id.getPath().equals("fluid_tank") || id.getPath().equals("item_vault"));
    }
    public static void changed(ServerSubLevel ship, BlockState oldState, BlockState next) {
        if (oldState.getBlock() == next.getBlock() || (!multiblock(oldState) && !multiblock(next))) return;
        DIRTY.computeIfAbsent(ship.getLevel(), unused -> Collections.newSetFromMap(new IdentityHashMap<>())).add(ship);
    }
    private static void tick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var ships = DIRTY.remove(level);
        if (ships == null) return;
        for (var ship : ships) if (!ship.isRemoved()) {
            ship.buildMassTracker();
            ship.updateMergedMassData(0);
        }
    }
    private static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) { DIRTY.clear(); }
    private PondusStructuralMass() {}
}
