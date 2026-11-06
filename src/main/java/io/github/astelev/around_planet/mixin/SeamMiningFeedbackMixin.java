package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.transfer.SeamInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayerGameMode.class)
public abstract class SeamMiningFeedbackMixin {
    @Shadow @Final protected ServerPlayer player;
    @Inject(method="destroyAndAck",at=@At("RETURN"))
    private void around_planet$miningResult(BlockPos pos,int sequence,String reason,CallbackInfo ci) {
        if(SeamInteraction.periodicReach(player,pos,1))player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.serverLevel(),pos));
    }
}
