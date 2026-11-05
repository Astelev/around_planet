package io.github.astelev.around_planet.mixin;

import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.core.util.objects.SortedArraySet;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import io.github.astelev.around_planet.compat.DhSeamRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.common.render.openGl.GlDhTerrainRenderer_neoforge",remap=false)
public abstract class SeamDhTerrainMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void around_planet$farTransparent(RenderParams params,boolean opaque,
            SortedArraySet<LodBufferContainer> buffers,IProfilerWrapper profiler,CallbackInfo ci) {
        // Every translated point is behind the seam: blend it before the nearer canonical water/glass.
        if(!opaque)DhSeamRenderer.append(params,false,profiler);
    }
    @Inject(method="render",at=@At("TAIL"))
    private void around_planet$farOpaque(RenderParams params,boolean opaque,
            SortedArraySet<LodBufferContainer> buffers,IProfilerWrapper profiler,CallbackInfo ci) {
        if(opaque)DhSeamRenderer.append(params,true,profiler);
    }
}
