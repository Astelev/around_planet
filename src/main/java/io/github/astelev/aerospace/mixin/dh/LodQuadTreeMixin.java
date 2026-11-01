package io.github.astelev.aerospace.mixin.dh;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.astelev.aerospace.compat.dh.DhFlightState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.render.QuadTree.LodQuadTree", remap = false)
public abstract class LodQuadTreeMixin {
    @ModifyReturnValue(method = "calcDetailLevelFromDistance(DB)B", at = @At("RETURN"))
    private byte aerospace$coarseFlightDetail(byte original) {
        return DhFlightState.applyDetailFloor(original);
    }
}
