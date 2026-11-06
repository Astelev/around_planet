package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.astelev.around_planet.client.SeamInteractionScope;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MultiPlayerGameMode.class)
public abstract class SeamClientInteractionScopeMixin {
    @WrapMethod(method="useItem")
    private InteractionResult around_planet$item(net.minecraft.world.entity.player.Player player,InteractionHand hand,Operation<InteractionResult> original) {
        SeamInteractionScope.enter();try{return original.call(player,hand);}finally{SeamInteractionScope.exit();}
    }
    @WrapMethod(method="startDestroyBlock")
    private boolean around_planet$start(BlockPos pos,Direction face,Operation<Boolean> original) {
        SeamInteractionScope.enter();try{return original.call(pos,face);}finally{SeamInteractionScope.exit();}
    }
    @WrapMethod(method="continueDestroyBlock")
    private boolean around_planet$continue(BlockPos pos,Direction face,Operation<Boolean> original) {
        SeamInteractionScope.enter();try{return original.call(pos,face);}finally{SeamInteractionScope.exit();}
    }
    @WrapMethod(method="destroyBlock")
    private boolean around_planet$destroy(BlockPos pos,Operation<Boolean> original) {
        SeamInteractionScope.enter();try{return original.call(pos);}finally{SeamInteractionScope.exit();}
    }
    @WrapMethod(method="useItemOn")
    private InteractionResult around_planet$use(LocalPlayer player,InteractionHand hand,BlockHitResult hit,Operation<InteractionResult> original) {
        SeamInteractionScope.enter();try{return original.call(player,hand,hit);}finally{SeamInteractionScope.exit();}
    }
}
