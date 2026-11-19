package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.space.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.world.level.LevelReader.class)
public interface PlanetBrightnessMixin {
    @Inject(method="getMaxLocalRawBrightness(Lnet/minecraft/core/BlockPos;)I",at=@At("HEAD"),cancellable=true)
    private void planet$localLight(net.minecraft.core.BlockPos pos,CallbackInfoReturnable<Integer> ci){if((Object)this instanceof net.minecraft.world.level.Level level && PlanetDaylight.enabled(level) && !SpaceCells.contains(level,pos.getX(),pos.getY(),pos.getZ()))ci.setReturnValue(level.getMaxLocalRawBrightness(pos,PlanetDaylight.darken(level,PlanetDaylight.worldX(level,pos))));}
}
