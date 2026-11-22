package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.entity.Entity;
/** Same dimension rule on both sides; preserve no-gravity, flight and existing attribute modifiers. */
@Mixin(Entity.class)
public abstract class MoonEntityGravityMixin {
 @ModifyReturnValue(method="getGravity",at=@At("RETURN"))
 private double around_planet$lunarGravity(double gravity){return ((Entity)(Object)this).level().dimension().equals(io.github.astelev.around_planet.space.MoonSurface.dimension())?gravity*(.0123/(.2724*.2724)):gravity;}
}
