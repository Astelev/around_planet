package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.transfer.TransferService;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerMovementMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleMovePlayer",at=@At("RETURN"))
    private void around_planet$afterMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        // Run after vanilla's movement/collision/anti-cheat checks, not on an untrusted requested coordinate.
        if (player.serverLevel().getServer().isSameThread()) TransferService.check(player);
    }
}
