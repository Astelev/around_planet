package io.github.astelev.around_planet.network;

import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

@EventBusSubscriber(modid="around_planet")
public record SpaceTransferPayload(ResourceLocation dimension,List<SpaceCells.Cell> cells,List<Move> moves) implements CustomPacketPayload {
    public record Move(UUID ship,double x,double y,double z,double dx,double dy,double dz,double qx,double qy,double qz,double qw){
        public Move(UUID ship,double x,double y,double z,double dx,double dy,double dz){this(ship,x,y,z,dx,dy,dz,0,0,0,1);}
    }
    public static final Type<SpaceTransferPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","space_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SpaceTransferPayload> CODEC=StreamCodec.of((b,p)->{
        b.writeResourceLocation(p.dimension);b.writeVarInt(p.cells.size());for(var c:p.cells){
            b.writeUUID(c.id());b.writeInt(c.slot());for(double n:new double[]{c.x(),c.y(),c.z(),c.originX(),c.originY(),c.originZ()})b.writeDouble(n);
            b.writeVarInt(c.ships().size());for(var id:c.ships())b.writeUUID(id);
        }
        b.writeVarInt(p.moves.size());for(var m:p.moves){b.writeUUID(m.ship);for(double n:new double[]{m.x,m.y,m.z,m.dx,m.dy,m.dz,m.qx,m.qy,m.qz,m.qw})b.writeDouble(n);}
    },b->{
        var dim=b.readResourceLocation();var cells=new ArrayList<SpaceCells.Cell>();int count=count(b,128);
        for(int i=0;i<count;i++){var id=b.readUUID();int slot=b.readInt();double x=b.readDouble(),y=b.readDouble(),z=b.readDouble(),ox=b.readDouble(),oy=b.readDouble(),oz=b.readDouble();var ids=new ArrayList<UUID>();int n=count(b,256);for(int j=0;j<n;j++)ids.add(b.readUUID());cells.add(new SpaceCells.Cell(id,slot,x,y,z,ox,oy,oz,ids));}
        var moves=new ArrayList<Move>();count=count(b,256);for(int i=0;i<count;i++)moves.add(new Move(b.readUUID(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble()));
        return new SpaceTransferPayload(dim,List.copyOf(cells),List.copyOf(moves));
    });
    private static int count(RegistryFriendlyByteBuf b,int max){int n=b.readVarInt();if(n<0 || n>max)throw new IllegalArgumentException("Space payload count");return n;}
    @Override public Type<SpaceTransferPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("2").optional().playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->io.github.astelev.around_planet.client.SpaceTransferClient.accept(p)));}
    public static void sync(ServerLevel level){var p=new SpaceTransferPayload(level.dimension().location(),SpaceCells.get(level).all(),List.of());for(var player:level.players())if(player.connection.hasChannel(TYPE))PacketDistributor.sendToPlayer(player,p);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p && p.connection.hasChannel(TYPE))PacketDistributor.sendToPlayer(p,new SpaceTransferPayload(p.level().dimension().location(),SpaceCells.get(p.serverLevel()).all(),List.of()));}
}
