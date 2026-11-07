package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.client.SeamVehicleCorrection;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class SeamVehiclePassengersMixin {
    @Inject(method="handleSetEntityPassengersPacket",at=@At("RETURN"))
    private void around_planet$cancelOldMount(ClientboundSetPassengersPacket packet,CallbackInfo ci) {
        SeamVehicleCorrection.passengersChanged(packet);
    }
}
