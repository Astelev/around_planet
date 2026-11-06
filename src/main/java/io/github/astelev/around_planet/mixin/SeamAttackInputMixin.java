package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.transfer.SeamInteraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The input gate reads the target before MultiPlayerGameMode gets an opportunity to act. */
@Mixin(Minecraft.class)
public abstract class SeamAttackInputMixin {
    @WrapOperation(method={"startAttack","continueAttack"},at=@At(value="INVOKE",
            target="Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState around_planet$target(ClientLevel level,BlockPos pos,Operation<BlockState> original) {
        if(!SeamInteraction.image(level,pos))return original.call(level,pos);
        var image=SeamImages.get(level,pos.getX()>>4,pos.getZ()>>4);
        return image==null?Blocks.AIR.defaultBlockState():image.getBlockState(pos);
    }
}
