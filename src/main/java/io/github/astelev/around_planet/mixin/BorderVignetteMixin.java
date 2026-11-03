package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public abstract class BorderVignetteMixin {
    @WrapOperation(method="renderVignette",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/border/WorldBorder;getDistanceToBorder(Lnet/minecraft/world/entity/Entity;)D"))
    private double around_planet$noSeamWarning(WorldBorder border, Entity entity, Operation<Double> original) {
        return PlanetBorders.get(border)==null ? original.call(border,entity) : PlanetBorders.warningDistance(border,entity);
    }
}
