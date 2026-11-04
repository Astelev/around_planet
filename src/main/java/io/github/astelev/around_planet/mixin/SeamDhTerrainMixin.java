package io.github.astelev.around_planet.mixin;

import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import io.github.astelev.around_planet.compat.DhPortalTerrain;
import io.github.astelev.around_planet.compat.DhPortalViews;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.render.renderer.LodRenderer",remap=false)
public abstract class SeamDhTerrainMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void around_planet$terrainOnly(RenderParams params,IProfilerWrapper profiler,CallbackInfo ci) {
        if(DhPortalViews.ready()) {DhPortalTerrain.render(params,profiler);ci.cancel();}
    }
}
