package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.compat.DhSeamBounds;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.render.QuadTree.LodRenderSection",remap=false)
public abstract class SeamDhRenderMixin {
    @Shadow @Final public long pos;
    @Inject(method="getRenderingEnabled",at=@At("RETURN"),cancellable=true)
    private void around_planet$clipLods(CallbackInfoReturnable<Boolean> cir) {
        var level=Minecraft.getInstance().level;if(level==null)return;
        var topology=PlanetBorders.get(level.getWorldBorder());
        if(topology!=null && DhSeamBounds.outside(topology,pos)) cir.setReturnValue(false);
    }
}
