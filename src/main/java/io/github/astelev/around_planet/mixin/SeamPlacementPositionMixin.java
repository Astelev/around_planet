package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.transfer.SeamInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockPlaceContext.class)
public abstract class SeamPlacementPositionMixin {
    @Inject(method="getClickedPos",at=@At("RETURN"),cancellable=true)
    private void around_planet$derivedPosition(CallbackInfoReturnable<BlockPos> cir) {
        var context=(BlockPlaceContext)(Object)this;
        if(!context.getLevel().isClientSide)cir.setReturnValue(SeamInteraction.canonical(context.getLevel(),cir.getReturnValue()));
    }
}
