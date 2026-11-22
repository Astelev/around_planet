package io.github.astelev.around_planet.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;

/** Fuel/other block-entity sync must not invalidate the unchanged block-derived envelope. */
@Mixin(LevelChunk.class)
public abstract class PlasmaHullBlocksMixin {
    @Shadow @Final private Level level;
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void around_planet$changed(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> ci) {
        if (!level.isClientSide || ci.getReturnValue() == null || ci.getReturnValue() == state || !net.neoforged.fml.ModList.get().isLoaded("sable")) return;
        var ship = dev.ryanhcode.sable.Sable.HELPER.getContaining(level, pos);
        if (ship instanceof dev.ryanhcode.sable.sublevel.ClientSubLevel client) io.github.astelev.around_planet.client.PlasmaHullCache.invalidate(client);
    }
}
