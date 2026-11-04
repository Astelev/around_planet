package io.github.astelev.around_planet.mixin;

import com.seibel.distanthorizons.core.level.ClientLevelModule;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import io.github.astelev.around_planet.compat.DhPortalViews;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.level.ClientLevelModule",remap=false)
public abstract class SeamDhModuleMixin {
    @Shadow @Final private IDhClientLevel clientLevel;
    @Unique private DhPortalViews around_planet$views;
    @Inject(method="clientTick",at=@At("HEAD"))
    private void around_planet$before(CallbackInfo ci) {
        if(around_planet$views==null)around_planet$views=new DhPortalViews((ClientLevelModule)(Object)this,clientLevel);
        around_planet$views.beforeTick();
    }
    @Inject(method="clientTick",at=@At("RETURN"))
    private void around_planet$after(CallbackInfo ci) {around_planet$views.afterTick();}
    @Inject(method={"stopRenderer","clearRenderCache","onGeneratorPlanChanged"},at=@At("HEAD"))
    private void around_planet$reset(CallbackInfo ci) {if(around_planet$views!=null)around_planet$views.reset();}
    @Inject(method="close",at=@At("HEAD"))
    private void around_planet$close(CallbackInfo ci) {
        if(around_planet$views!=null)around_planet$views.close();
        io.github.astelev.around_planet.compat.DhPortalDepth.clear();
    }
    @Inject(method="OnDataSourceUpdated",at=@At("RETURN"))
    private void around_planet$data(FullDataSourceV2 data,CallbackInfo ci) {if(around_planet$views!=null)around_planet$views.reload(data.getPos());}
    @Inject(method="reloadPos",at=@At("RETURN"))
    private void around_planet$reload(long pos,CallbackInfo ci) {if(around_planet$views!=null)around_planet$views.reload(pos);}
}
