package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.transfer.SeamInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class SeamInteractionReachMixin {
    @Inject(method="canInteractWithBlock",at=@At("RETURN"),cancellable=true)
    private void around_planet$periodicReach(BlockPos pos,double extra,CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue() && SeamInteraction.periodicReach((Player)(Object)this,pos,extra))cir.setReturnValue(true);
    }
}
