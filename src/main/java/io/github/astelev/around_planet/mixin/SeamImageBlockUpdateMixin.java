package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.client.SeamImages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
public abstract class SeamImageBlockUpdateMixin {
    @Inject(method="handleBlockUpdate",at=@At("RETURN"))
    private void around_planet$authoritativeImageUpdate(ClientboundBlockUpdatePacket packet,CallbackInfo ci) {
        if(Minecraft.getInstance().isSameThread())SeamImages.authoritativeBlock(packet.getPos(),packet.getBlockState());
    }
}
