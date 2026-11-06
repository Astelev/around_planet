package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.transfer.SeamInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SeamInteractionPacketMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleUseItemOn",at=@At("HEAD"),cancellable=true)
    private void around_planet$validateUse(ServerboundUseItemOnPacket packet,CallbackInfo ci) {
        if(player.serverLevel().getServer().isSameThread() && !SeamInteraction.requestReady(player,packet.getHitResult().getBlockPos())) {
            player.connection.ackBlockChangesUpTo(packet.getSequence());ci.cancel();
        }
    }
    @Inject(method="handlePlayerAction",at=@At("HEAD"),cancellable=true)
    private void around_planet$validateDig(ServerboundPlayerActionPacket packet,CallbackInfo ci) {
        var action=packet.getAction();
        if(player.serverLevel().getServer().isSameThread() && (action==ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                || action==ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK )
                && !SeamInteraction.requestReady(player,packet.getPos())) {
            player.connection.ackBlockChangesUpTo(packet.getSequence());ci.cancel();
        }
    }
    @WrapOperation(method="handleUseItemOn",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ServerboundUseItemOnPacket;getHitResult()Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult around_planet$canonicalUse(ServerboundUseItemOnPacket packet,Operation<BlockHitResult> original) {
        return SeamInteraction.canonical(player.level(),original.call(packet));
    }
    @WrapOperation(method="handlePlayerAction",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;getPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos around_planet$canonicalDig(ServerboundPlayerActionPacket packet,Operation<BlockPos> original) {
        return SeamInteraction.canonical(player.level(),original.call(packet));
    }
    @WrapOperation(method="handleUseItemOn",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos around_planet$canonicalNeighborFeedback(BlockPos pos,net.minecraft.core.Direction direction,Operation<BlockPos> original) {
        return SeamInteraction.canonical(player.level(),original.call(pos,direction));
    }

}
