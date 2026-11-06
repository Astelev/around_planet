package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.transfer.SeamCollisions;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Boat.class)
public abstract class SeamBoatDismountMixin {
    @WrapOperation(method="getDismountLocationForPassenger",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getBlockFloorHeight(Lnet/minecraft/core/BlockPos;)D"))
    private double around_planet$floor(Level level,BlockPos pos,Operation<Double> original) {
        var view=SeamCollisions.view(level,(Boat)(Object)this,new AABB(pos));
        return view==level?original.call(level,pos):view.getBlockFloorHeight(pos);
    }
    @WrapOperation(method="getDismountLocationForPassenger",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;isWaterAt(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean around_planet$water(Level level,BlockPos pos,Operation<Boolean> original) {
        var view=SeamCollisions.view(level,(Boat)(Object)this,new AABB(pos));
        return view==level?original.call(level,pos):view.getFluidState(pos).is(FluidTags.WATER);
    }
}
