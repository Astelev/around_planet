package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import io.github.astelev.around_planet.fuel.FuelMassBridge;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Repairs API/ledger boundaries while retaining Pondus's configured mass calculations. */
public final class PondusMassCompatibility {
    // Lazy initialization keeps Pondus optional, without resolving its classes on standalone installs.
    private static final class Api {
        static final Method IS_MULTIBLOCK, CONTROLLER, UPDATE_TOTAL, ENTRY, BASE, SET_CACHED;
        static {
            try {
                String p = "org.fractalinteractive.ci.pondus_inventory_fi.";
                var helper = Class.forName(p + "mass.CreateMultiblockHelper");
                var ledger = Class.forName(p + "mass.InventoryMassCache");
                IS_MULTIBLOCK = helper.getMethod("isCreateMultiblock", BlockEntity.class);
                CONTROLLER = helper.getMethod("getController", BlockEntity.class);
                UPDATE_TOTAL = ledger.getMethod("updateTotalMultiblockMass", Level.class, BlockPos.class, BlockEntity.class);
                ENTRY = Class.forName(p + "mass.MassRegistry").getMethod("getBlockMass", Block.class, BlockState.class);
                BASE = Class.forName(p + "config.BlockMassEntry").getMethod("getBaseMass");
                SET_CACHED = ledger.getMethod("setCached", Level.class, BlockPos.class, double.class);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unsupported Pondus mass ledger API", e);
            }
        }
    }

    public static boolean multiblock(BlockGetter level, BlockPos pos) {
        if (!FuelMassBridge.pondus()) return false;
        var be = level.getBlockEntity(pos);
        return be != null && multiblock(be);
    }

    private static boolean multiblock(BlockEntity be) {
        try { return (boolean) Api.IS_MULTIBLOCK.invoke(null, be); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    public static void refresh(BlockEntity be) {
        if (be == null || be.getLevel() == null || be.getLevel().isClientSide || !multiblock(be)) return;
        try {
            var root = (BlockEntity) Api.CONTROLLER.invoke(null, be);
            if (root != null) Api.UPDATE_TOTAL.invoke(null, root.getLevel(), root.getBlockPos(), root);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot refresh Pondus multiblock ledger", e);
        }
    }

    public static double buildCellMass(BlockGetter level, BlockPos pos, double previous) {
        if (!multiblock(level, pos)) return previous;
        refresh(level.getBlockEntity(pos));
        // Use the same native Pondus calculation as incremental block changes. Its build redirect
        // otherwise counts only inventory-owning controllers, losing the other cells' shares.
        double mass = PhysicsBlockPropertyHelper.getMass(level, pos, level.getBlockState(pos));
        if (level instanceof Level world) {
            try {
                var state = level.getBlockState(pos);
                var definition = Api.ENTRY.invoke(null, state.getBlock(), state);
                if (definition != null)
                    Api.SET_CACHED.invoke(null, world, pos, Math.max(0, mass - (double) Api.BASE.invoke(definition)));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot seed multiblock cell ledger", e);
            }
        }
        return mass;
    }

    private PondusMassCompatibility() {}
}
