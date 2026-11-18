package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Synchronize restored engine fuel only; never rebuild other containers or the hull. */
@Pseudo @Mixin(targets="dev.ryanhcode.sable.api.SubLevelAssemblyHelper",remap=false)
public abstract class RocketAssemblyMassMixin {
 @Inject(method="assembleBlocks",at=@At("RETURN"))
 private static void around_planet$restoreFueledMass(CallbackInfoReturnable<dev.ryanhcode.sable.sublevel.ServerSubLevel> cir){var ship=cir.getReturnValue();if(ship==null)return;for(var holder:ship.getPlot().getLoadedChunks())for(var be:holder.getChunk().getBlockEntities().values())if(be instanceof io.github.astelev.around_planet.propulsion.PropulsionBlockEntity engine&&engine.kind()==io.github.astelev.around_planet.propulsion.PropulsionRegistry.Kind.LIQUID){io.github.astelev.around_planet.compat.RocketMassChange.assembled(ship,engine);}else if(be instanceof io.github.astelev.around_planet.solid.SolidColumnBlockEntity column)io.github.astelev.around_planet.compat.SolidNativeMass.assembled(ship,column);}
}
