package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(UseOnContext.class)
public abstract class SeamPlacementLocationMixin {
    @Inject(method="getClickLocation",at=@At("RETURN"),cancellable=true)
    private void around_planet$coherentHitLocation(CallbackInfoReturnable<Vec3> cir) {
        if(!((Object)this instanceof BlockPlaceContext context) || context.getLevel().isClientSide)return;
        var t=PlanetBorders.get(context.getLevel().getWorldBorder());if(t==null || t.outside(context.getClickedPos().getX()))return;
        var point=cir.getReturnValue();double dx=Math.floor((context.getClickedPos().getX()-point.x)/t.width()+0.5)*t.width();
        if(dx!=0)cir.setReturnValue(point.add(dx,0,0));
    }
}
