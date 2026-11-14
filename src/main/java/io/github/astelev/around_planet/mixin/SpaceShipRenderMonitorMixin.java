package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.client.SpaceRenderView;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets={"dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher","dev.ryanhcode.sable.sublevel.render.dispatcher.FancySubLevelRenderDispatcher"},remap=false)
public abstract class SpaceShipRenderMonitorMixin {
    @Inject(method="renderSectionLayer",at=@At("HEAD"))
    private void space$shipLayer(CallbackInfo ci){if(SpaceRenderView.active())SpaceRenderView.shipPasses++;}
}
