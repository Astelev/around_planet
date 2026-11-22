package io.github.astelev.around_planet.network;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlockEntity;
import io.github.astelev.around_planet.space.OrbitalAttitude;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record OrbitalSensorPayload(BlockPos pos,int mode,int scale) implements CustomPacketPayload {
    public static final Type<OrbitalSensorPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","orbital_sensor_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OrbitalSensorPayload> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeVarInt(p.mode);b.writeVarInt(p.scale);},b->new OrbitalSensorPayload(b.readBlockPos(),b.readVarInt(),b.readVarInt()));
    @Override public Type<OrbitalSensorPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").optional().playToServer(TYPE,CODEC,(p,c)->c.enqueueWork(()->{var player=(net.minecraft.server.level.ServerPlayer)c.player();var level=player.serverLevel();
        if(player.isSpectator() || !player.mayBuild() || p.mode<0 || p.mode>=OrbitalAttitude.Mode.values().length || p.scale<5 || p.scale>180 || !level.hasChunkAt(p.pos) || !(level.getBlockEntity(p.pos) instanceof OrbitalSensorBlockEntity sensor))return;
        org.joml.Vector3d target,viewer;if(net.neoforged.fml.ModList.get().isLoaded("sable")){target=io.github.astelev.around_planet.compat.SpaceSensorNavigation.worldPoint(level,p.pos);viewer=io.github.astelev.around_planet.compat.SpaceSensorNavigation.worldPoint(level,player.blockPosition());}else{target=new org.joml.Vector3d(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5);viewer=new org.joml.Vector3d(player.getX(),player.getY(),player.getZ());}
        if(target.distanceSquared(viewer)>64 || !level.mayInteract(player,BlockPos.containing(target.x,target.y,target.z)))return;sensor.configure(OrbitalAttitude.Mode.values()[p.mode],p.scale);
    }));}
}
