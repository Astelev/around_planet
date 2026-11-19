package io.github.astelev.around_planet.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
@EventBusSubscriber(modid="around_planet")
public record VacuumPayload(Status status,int air,int maximum) implements CustomPacketPayload {
    public record Status(boolean outside,boolean room,boolean breathable){}
    public static final Type<VacuumPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","vacuum_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf,VacuumPayload> CODEC=StreamCodec.of((b,p)->{b.writeBoolean(p.status.outside);b.writeBoolean(p.status.room);b.writeBoolean(p.status.breathable);b.writeVarInt(p.air);b.writeVarInt(p.maximum);},b->new VacuumPayload(new Status(b.readBoolean(),b.readBoolean(),b.readBoolean()),b.readVarInt(),b.readVarInt()));
    @Override public Type<VacuumPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").optional().playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->io.github.astelev.around_planet.client.VacuumHud.accept(p)));}
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level&&level.getGameTime()%5==0)for(var player:level.players())if(player.connection.hasChannel(TYPE))PacketDistributor.sendToPlayer(player,new VacuumPayload(io.github.astelev.around_planet.lifesupport.VacuumBreathing.status(player),player.getAirSupply(),player.getMaxAirSupply()));}
}
