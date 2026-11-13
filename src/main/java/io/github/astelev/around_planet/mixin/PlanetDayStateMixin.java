package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.space.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.world.level.Level.class)
public abstract class PlanetDayStateMixin {
    @Inject(method="isDay",at=@At("HEAD"),cancellable=true)
    private void planet$localDay(CallbackInfoReturnable<Boolean> ci){var level=(net.minecraft.world.level.Level)(Object)this;var x=PlanetDaylight.scopedX(level);if(x!=null && PlanetDaylight.enabled(level))ci.setReturnValue(PlanetDaylight.darken(level,x)<4);}
    @Inject(method="getSkyDarken",at=@At("HEAD"),cancellable=true)
    private void planet$localDark(CallbackInfoReturnable<Integer> ci){var level=(net.minecraft.world.level.Level)(Object)this;var x=PlanetDaylight.scopedX(level);if(x!=null && PlanetDaylight.enabled(level))ci.setReturnValue(PlanetDaylight.darken(level,x));}
}
