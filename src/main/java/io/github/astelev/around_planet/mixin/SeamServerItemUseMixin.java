package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.astelev.around_planet.transfer.SeamItemUseScope;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ServerPlayerGameMode.class)
public abstract class SeamServerItemUseMixin {
    @WrapMethod(method="useItemOn")
    private InteractionResult around_planet$nativeBlockItem(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit,Operation<InteractionResult> original) {
        var previous=SeamItemUseScope.enter(player);
        try{return original.call(player,level,stack,hand,hit);}finally{SeamItemUseScope.restore(previous);}
    }
    @WrapMethod(method="useItem")
    private InteractionResult around_planet$nativeItemRay(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,Operation<InteractionResult> original) {
        var previous=SeamItemUseScope.enter(player);
        try{var result=original.call(player,level,stack,hand);io.github.astelev.around_planet.transfer.SeamInteraction.feedback(player,SeamItemUseScope.hit());return result;}finally{SeamItemUseScope.restore(previous);}
    }
}
