package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.client.SeamInteractionScope;
import io.github.astelev.around_planet.transfer.SeamInteraction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class SeamClientBlockReadsMixin {
    @Inject(method="getBlockState",at=@At("HEAD"),cancellable=true)
    private void around_planet$state(BlockPos pos,CallbackInfoReturnable<BlockState> cir) {
        if(SeamInteractionScope.active() && (Object)this instanceof ClientLevel level && SeamInteraction.image(level,pos)) {
            var image=SeamImages.get(level,pos.getX()>>4,pos.getZ()>>4);
            cir.setReturnValue(image==null?net.minecraft.world.level.block.Blocks.AIR.defaultBlockState():image.getBlockState(pos));
        }
    }
    @Inject(method="getBlockEntity",at=@At("HEAD"),cancellable=true)
    private void around_planet$blockEntity(BlockPos pos,CallbackInfoReturnable<net.minecraft.world.level.block.entity.BlockEntity> cir) {
        if(SeamInteractionScope.active() && (Object)this instanceof ClientLevel level && SeamInteraction.image(level,pos)) {
            var image=SeamImages.get(level,pos.getX()>>4,pos.getZ()>>4);cir.setReturnValue(image==null?null:image.getBlockEntity(pos));
        }
    }
    @Inject(method="getFluidState",at=@At("HEAD"),cancellable=true)
    private void around_planet$fluid(BlockPos pos,CallbackInfoReturnable<FluidState> cir) {
        if(SeamInteractionScope.active() && (Object)this instanceof ClientLevel level && SeamInteraction.image(level,pos)) {
            var image=SeamImages.get(level,pos.getX()>>4,pos.getZ()>>4);
            cir.setReturnValue(image==null?net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState():image.getFluidState(pos));
        }
    }
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
    private void around_planet$noPredictedWorldWrites(BlockPos pos,BlockState state,int flags,int depth,CallbackInfoReturnable<Boolean> cir) {
        if(SeamInteractionScope.active() && (Object)this instanceof ClientLevel level && SeamInteraction.image(level,pos))cir.setReturnValue(false);
    }
}
