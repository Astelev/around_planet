package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class SeamBorderRenderMixin {
    @WrapOperation(method="renderWorldBorder",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/border/WorldBorder;getMinX()D"))
    private double around_planet$west(WorldBorder border,Operation<Double> original) {
        return PlanetBorders.get(border)==null?original.call(border):Math.max(-border.getAbsoluteMaxSize(),border.getCenterX()-border.getSize()/2);
    }
    @WrapOperation(method="renderWorldBorder",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/border/WorldBorder;getMaxX()D"))
    private double around_planet$east(WorldBorder border,Operation<Double> original) {
        return PlanetBorders.get(border)==null?original.call(border):Math.min(border.getAbsoluteMaxSize(),border.getCenterX()+border.getSize()/2);
    }
}
