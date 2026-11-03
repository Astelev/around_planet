package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.seam.SeamGeometry;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="io.github.astelev.cruising.client.SurfaceTerrainMask",remap=false)
public abstract class SeamCruisingMaskMixin {
    @Inject(method="coveredColumn",at=@At("HEAD"),cancellable=true)
    private static void around_planet$noApproximationOutside(ClientLevel level,int x,int z,CallbackInfoReturnable<Boolean> cir) {
        var topology=PlanetBorders.get(level.getWorldBorder());
        if(topology!=null && !SeamGeometry.canonical(topology,x)) cir.setReturnValue(true);
    }
}
