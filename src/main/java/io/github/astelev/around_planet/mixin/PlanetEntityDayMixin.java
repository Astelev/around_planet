package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.astelev.around_planet.space.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
/** Sun-burning and ordinary living-entity day checks use that entity's longitude. */
@Mixin(LivingEntity.class)
public abstract class PlanetEntityDayMixin {
    @WrapMethod(method="tick") private void planet$entityClock(Operation<Void> original){var e=(LivingEntity)(Object)this;if(!PlanetDaylight.enabled(e.level()) || SpaceCells.contains(e)){original.call();return;}try(var scope=PlanetDaylight.scope(e.level(),PlanetDaylight.worldX(e.level(),e.blockPosition()))){original.call();}}
}
