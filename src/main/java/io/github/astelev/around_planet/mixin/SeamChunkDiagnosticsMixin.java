package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.diagnostics.SeamDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe packets on the client executor only; no change to cache ownership or packet handling. */
@Mixin(ClientPacketListener.class)
public abstract class SeamChunkDiagnosticsMixin {
    @Inject(method="handleLevelChunkWithLight",at=@At("HEAD"))
    private void around_planet$chunk(ClientboundLevelChunkWithLightPacket packet,CallbackInfo ci) {
        if(!Minecraft.getInstance().isSameThread())return;
        SeamDiagnostics.event("client","native.chunk_packet",()->"pos="+packet.getX()+","+packet.getZ());
    }
    @Inject(method="handleForgetLevelChunk",at=@At("HEAD"))
    private void around_planet$forget(ClientboundForgetLevelChunkPacket packet,CallbackInfo ci) {
        if(!Minecraft.getInstance().isSameThread())return;
        SeamDiagnostics.event("client","native.forget_packet",()->"pos="+packet.pos());
    }
    @Inject(method="handleSetChunkCacheCenter",at=@At("HEAD"))
    private void around_planet$center(net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket packet,CallbackInfo ci) {
        if(!Minecraft.getInstance().isSameThread())return;
        io.github.astelev.around_planet.diagnostics.SeamClientDiagnostics.cacheX=packet.getX();
        io.github.astelev.around_planet.diagnostics.SeamClientDiagnostics.cacheZ=packet.getZ();
        SeamDiagnostics.event("client","native.cache_center",()->"pos="+packet.getX()+","+packet.getZ());
    }
    @Inject(method="handleSetChunkCacheRadius",at=@At("HEAD"))
    private void around_planet$radius(net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket packet,CallbackInfo ci) {
        if(!Minecraft.getInstance().isSameThread())return;
        io.github.astelev.around_planet.diagnostics.SeamClientDiagnostics.cacheRadius=packet.getRadius();
        SeamDiagnostics.event("client","native.cache_radius",()->"radius="+packet.getRadius());
    }
}
