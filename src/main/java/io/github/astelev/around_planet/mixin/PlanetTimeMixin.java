package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.space.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.world.level.LevelTimeAccess.class)
public interface PlanetTimeMixin {
    @Inject(method="getTimeOfDay",at=@At("HEAD"),cancellable=true)
    private void planet$clock(float partial,CallbackInfoReturnable<Float> ci){if((Object)this instanceof net.minecraft.world.level.Level level && PlanetDaylight.enabled(level)){var x=PlanetDaylight.scopedX(level);ci.setReturnValue((float)PlanetDaylight.time(level,x==null?0:x,partial));}}
}
