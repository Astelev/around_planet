package io.github.astelev.around_planet.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.LevelPlot", remap = false)
public abstract class PlasmaHullBlocksMixin {
    @Inject(method = "onBlockChange", at = @At("TAIL"))
    private void around_planet$changed(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (((dev.ryanhcode.sable.sublevel.plot.LevelPlot)(Object)this).getSubLevel() instanceof dev.ryanhcode.sable.sublevel.ClientSubLevel ship)
            io.github.astelev.around_planet.client.PlasmaHullCache.invalidate(ship);
    }
}
