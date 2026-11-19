package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.astelev.around_planet.space.PlanetDaylight;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(net.minecraft.world.level.block.BedBlock.class)
public abstract class PlanetBedDayMixin {
    @WrapMethod(method="useWithoutItem") private InteractionResult planet$bedClock(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit,Operation<InteractionResult> original){try(var scope=PlanetDaylight.scope(level,PlanetDaylight.worldX(level,pos))){return original.call(state,level,pos,player,hit);}}
}
