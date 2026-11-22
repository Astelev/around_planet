package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Stop the weather graph before terrain scans, flow catalog construction and visual broadcasts. */
@Pseudo @Mixin(targets="net.aste.aeroworks.wind.WindWorlds",remap=false)
public abstract class AeroLunarWindMixin {
 @Inject(method="tick",at=@At("HEAD"),cancellable=true,require=0)
 private static void moonTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post e,CallbackInfo ci){if(e.getLevel().dimension().equals(io.github.astelev.around_planet.space.MoonSurface.dimension()))ci.cancel();}
 @Inject(method="isTurbulent",at=@At("HEAD"),cancellable=true,require=0)
 private static void moonTurbulence(net.minecraft.server.level.ServerLevel l,int body,CallbackInfoReturnable<Boolean> ci){if(l.dimension().equals(io.github.astelev.around_planet.space.MoonSurface.dimension()))ci.setReturnValue(false);}
}
