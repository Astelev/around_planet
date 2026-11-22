package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import io.github.astelev.around_planet.compat.PondusMassCompatibility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Pondus needs a Level to read inventories/seed its ledger; Sable supplies a BlockGetter wrapper. */
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.api.physics.mass.MassTracker", remap=false)
public abstract class PondusMassBuildMixin {
    @WrapMethod(method="build")
    private static MassTracker around_planet$readInventories(BlockGetter level, BoundingBox3ic bounds,
                                                             Operation<MassTracker> original) {
        return original.call(level instanceof MassLevelAccessor access ? access.around_planet$massLevel() : level, bounds);
    }

    // Sable 2.0.6 stores each cell's mass in slot 13 in both the COM and inertia passes.
    // Require both sites: changing only one would leave mass and inertia inconsistent.
    @ModifyVariable(method="build", at=@At("STORE"), index=13, require=2, allow=2)
    private static double around_planet$allMultiblockCells(double mass,
            @Local(argsOnly=true) BlockGetter level, @Local BlockPos.MutableBlockPos pos) {
        return PondusMassCompatibility.buildCellMass(level, pos, mass);
    }

    // Contents are distributed across cells already. Pondus's build-only COM patch supplies
    // absolute controller coordinates here, where Sable adds the current cell position.
    @ModifyArg(method="build", at=@At(value="INVOKE",
            target="Lorg/joml/Vector3d;add(Lorg/joml/Vector3dc;)Lorg/joml/Vector3d;"), index=0, require=2, allow=2)
    private static Vector3dc around_planet$blockLocalCenter(Vector3dc offset,
            @Local(argsOnly=true) BlockGetter level, @Local BlockPos.MutableBlockPos pos) {
        return PondusMassCompatibility.multiblock(level, pos) ? new Vector3d(.5, .5, .5) : offset;
    }
}
