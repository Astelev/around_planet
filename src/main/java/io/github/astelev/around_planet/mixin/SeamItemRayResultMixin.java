package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.transfer.SeamInteraction;
import io.github.astelev.around_planet.transfer.SeamItemUseScope;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Item.class)
public abstract class SeamItemRayResultMixin {
    @Inject(method="getPlayerPOVHitResult",at=@At("RETURN"),cancellable=true)
    private static void around_planet$canonicalRay(Level level,Player player,ClipContext.Fluid mode,CallbackInfoReturnable<BlockHitResult> cir) {
        var hit=cir.getReturnValue();
        if(player instanceof ServerPlayer p && SeamItemUseScope.actor()==p && SeamInteraction.image(level,hit.getBlockPos())) {
            if(SeamInteraction.requestReady(p,hit.getBlockPos())){var source=SeamInteraction.canonical(level,hit);SeamItemUseScope.hit(source);cir.setReturnValue(source);}
            else cir.setReturnValue(BlockHitResult.miss(hit.getLocation(),hit.getDirection(),hit.getBlockPos()));
        }
    }
}
