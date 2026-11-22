package io.github.astelev.around_planet.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.ChunkPos;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.ClientLevelPlot", remap = false)
public abstract class PlasmaHullChunksMixin {
    @Inject(method = "addChunkHolder", at = @At("TAIL"))
    private void around_planet$chunk(ChunkPos pos, dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder holder, boolean load, CallbackInfo ci) {
        io.github.astelev.around_planet.client.PlasmaHullCache.invalidate(((dev.ryanhcode.sable.sublevel.plot.ClientLevelPlot)(Object)this).getSubLevel());
    }
}
