package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {
    @Unique private VoxelShape around_planet$shape;
    @Unique private double around_planet$minX, around_planet$maxX, around_planet$minZ, around_planet$maxZ;

    @Inject(method="getMinX", at=@At("RETURN"), cancellable=true)
    private void around_planet$west(CallbackInfoReturnable<Double> cir) {
        var topology = PlanetBorders.get((WorldBorder)(Object)this);
        if (topology != null) cir.setReturnValue(Math.max(cir.getReturnValue(), topology.west()-PlanetBorders.guardMargin()));
    }
    @Inject(method="getMaxX", at=@At("RETURN"), cancellable=true)
    private void around_planet$east(CallbackInfoReturnable<Double> cir) {
        var topology = PlanetBorders.get((WorldBorder)(Object)this);
        if (topology != null) cir.setReturnValue(Math.min(cir.getReturnValue(), topology.east()+PlanetBorders.guardMargin()));
    }
    @Inject(method="getMinZ", at=@At("RETURN"), cancellable=true)
    private void around_planet$north(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(Math.max(cir.getReturnValue(),-PlanetBorders.boundaryZ((WorldBorder)(Object)this)));
    }
    @Inject(method="getMaxZ", at=@At("RETURN"), cancellable=true)
    private void around_planet$south(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(Math.min(cir.getReturnValue(),PlanetBorders.boundaryZ((WorldBorder)(Object)this)));
    }
    @Inject(method="getCollisionShape", at=@At("HEAD"), cancellable=true)
    private void around_planet$guard(CallbackInfoReturnable<VoxelShape> cir) {
        var border = (WorldBorder)(Object)this;
        if (PlanetBorders.get(border) == null) return;
        double x0=border.getMinX(), x1=border.getMaxX(), z0=border.getMinZ(), z1=border.getMaxZ();
        if (around_planet$shape == null || x0!=around_planet$minX || x1!=around_planet$maxX || z0!=around_planet$minZ || z1!=around_planet$maxZ) {
            around_planet$shape = Shapes.join(Shapes.INFINITY, Shapes.box(Math.floor(x0), Double.NEGATIVE_INFINITY,
                    Math.floor(z0), Math.ceil(x1), Double.POSITIVE_INFINITY, Math.ceil(z1)), BooleanOp.ONLY_FIRST);
            around_planet$minX=x0; around_planet$maxX=x1; around_planet$minZ=z0; around_planet$maxZ=z1;
        }
        cir.setReturnValue(around_planet$shape);
    }
}
