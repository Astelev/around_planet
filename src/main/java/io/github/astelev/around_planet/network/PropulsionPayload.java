package io.github.astelev.around_planet.network;
import io.github.astelev.around_planet.propulsion.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
/** Frequency packets name a real inventory slot, never arbitrary client-supplied item NBT. */
public record PropulsionPayload(BlockPos pos,int action,int index,int value) implements CustomPacketPayload {
    public static final Type<PropulsionPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","propulsion_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PropulsionPayload> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeVarInt(p.action);b.writeVarInt(p.index);b.writeVarInt(p.value);},b->new PropulsionPayload(b.readBlockPos(),b.readVarInt(),b.readVarInt(),b.readVarInt()));
    @Override public Type<PropulsionPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").optional().playToServer(TYPE,CODEC,(p,c)->c.enqueueWork(()->{
        var player=(net.minecraft.server.level.ServerPlayer)c.player();var level=player.serverLevel();if(player.isSpectator()||!player.mayBuild()||!level.hasChunkAt(p.pos)||!(level.getBlockEntity(p.pos) instanceof PropulsionBlockEntity be))return;
        org.joml.Vector3d target,viewer;if(net.neoforged.fml.ModList.get().isLoaded("sable")){target=io.github.astelev.around_planet.compat.SpaceSensorNavigation.worldPoint(level,p.pos);viewer=io.github.astelev.around_planet.compat.SpaceSensorNavigation.worldPoint(level,player.blockPosition());}else{target=new org.joml.Vector3d(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5);viewer=new org.joml.Vector3d(player.getX(),player.getY(),player.getZ());}
        if(target.distanceSquared(viewer)>64||!level.mayInteract(player,BlockPos.containing(target.x,target.y,target.z)))return;
        if(p.action==0){if(p.value<0||p.value>10_000_000)return;be.configure(be.kind()==PropulsionRegistry.Kind.DAMPER?p.value/100.0:p.value);}
        else if(p.action==1&&be.kind()==PropulsionRegistry.Kind.RSU&&p.index>=0&&p.index<8&&p.value>=-1&&p.value<36)be.frequency(p.index,p.value<0?net.minecraft.world.item.ItemStack.EMPTY:player.getInventory().getItem(p.value));
    }));}
}
