package io.github.astelev.cruising.mixin;

import io.github.astelev.cruising.flight.FlightHooks;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Inject(method = "skipPlayer", at = @At("RETURN"), cancellable = true)
    private void cruising$deferSkyPlayerTickets(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && FlightHooks.isHighShipEntity(player)) {
            cir.setReturnValue(true);
        }
    }
}
