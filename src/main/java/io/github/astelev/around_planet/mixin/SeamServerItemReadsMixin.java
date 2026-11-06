package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.transfer.SeamInteraction;
import io.github.astelev.around_planet.transfer.SeamItemUseScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Only a validated actor's native item ray reads periodic blocks. Writes remain normal canonical level operations. */
@Mixin(Level.class)
public abstract class SeamServerItemReadsMixin {
    @Inject(method="getBlockState",at=@At("HEAD"),cancellable=true)
    private void around_planet$state(BlockPos pos,CallbackInfoReturnable<BlockState> cir) {
        var p=SeamItemUseScope.actor();
        if(p!=null && (Object)this instanceof ServerLevel level && p.level()==level && SeamInteraction.image(level,pos) ) {
            var source=SeamInteraction.canonical(level,pos);var chunk=SeamInteraction.sourceReady(p,pos)?level.getChunkSource().getChunkNow(source.getX()>>4,source.getZ()>>4):null;
            cir.setReturnValue(chunk==null?net.minecraft.world.level.block.Blocks.AIR.defaultBlockState():chunk.getBlockState(source));
        }
    }
    @Inject(method="getFluidState",at=@At("HEAD"),cancellable=true)
    private void around_planet$fluid(BlockPos pos,CallbackInfoReturnable<FluidState> cir) {
        var p=SeamItemUseScope.actor();
        if(p!=null && (Object)this instanceof ServerLevel level && p.level()==level && SeamInteraction.image(level,pos) ) {
            var source=SeamInteraction.canonical(level,pos);var chunk=SeamInteraction.sourceReady(p,pos)?level.getChunkSource().getChunkNow(source.getX()>>4,source.getZ()>>4):null;
            cir.setReturnValue(chunk==null?net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState():chunk.getFluidState(source));
        }
    }
}
