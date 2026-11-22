package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.astelev.around_planet.space.PlanetDaylight;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(net.minecraft.world.level.block.DaylightDetectorBlock.class)
public abstract class PlanetDetectorDayMixin {
    @WrapMethod(method="updateSignalStrength") private static void planet$detectorClock(BlockState state,Level level,BlockPos pos,Operation<Void> original){try(var scope=PlanetDaylight.scope(level,PlanetDaylight.worldX(level,pos))){original.call(state,level,pos);}}
}
