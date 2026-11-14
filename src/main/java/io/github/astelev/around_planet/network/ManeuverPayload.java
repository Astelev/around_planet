package io.github.astelev.around_planet.network;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.compat.ManeuverAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
/** Authoritative saved plans. Editor sessions are granted by a real empty-hand block interaction. */
public record ManeuverPayload(UUID focus,long now,SpaceTrajectory trajectory,List<ManeuverNode> nodes,UUID session,long ack,boolean open,String error,int warningSeconds) implements CustomPacketPayload {
    public static final UUID NONE=new UUID(0,0);
    public static final Type<ManeuverPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","maneuver_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ManeuverPayload> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.focus);b.writeLong(p.now);b.writeLong(p.trajectory.epochTick());for(double n:new double[]{p.trajectory.initial().position().x(),p.trajectory.initial().position().y(),p.trajectory.initial().position().z(),p.trajectory.initial().velocity().x(),p.trajectory.initial().velocity().y(),p.trajectory.initial().velocity().z(),p.trajectory.mu()})b.writeDouble(n);b.writeVarInt(p.nodes.size());for(var n:p.nodes)writeNode(b,n);b.writeUUID(p.session);b.writeLong(p.ack);b.writeBoolean(p.open);b.writeUtf(p.error,96);b.writeVarInt(p.warningSeconds);},b->{UUID id=b.readUUID();long now=b.readLong(),epoch=b.readLong();var path=new SpaceTrajectory("around_planet:preview",epoch,new SpaceState(vector(b),vector(b)),b.readDouble());int size=b.readVarInt();if(size<0||size>ManeuverNode.MAX_NODES)throw new IllegalArgumentException("Maneuver count");var nodes=new ArrayList<ManeuverNode>();for(int i=0;i<size;i++)nodes.add(readNode(b));return new ManeuverPayload(id,now,path,List.copyOf(nodes),b.readUUID(),b.readLong(),b.readBoolean(),b.readUtf(96),b.readVarInt());});
    public record Edit(UUID session,long sequence,boolean delete,ManeuverNode node) implements CustomPacketPayload {
        public static final Type<Edit> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","maneuver_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Edit> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.session);b.writeLong(p.sequence);b.writeBoolean(p.delete);writeNode(b,p.node);},b->new Edit(b.readUUID(),b.readLong(),b.readBoolean(),readNode(b)));
        @Override public Type<Edit> type(){return TYPE;}
    }
    public record Warning(UUID session,int seconds) implements CustomPacketPayload {
        public static final Type<Warning> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","maneuver_warning"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Warning> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.session);b.writeVarInt(p.seconds);},b->new Warning(b.readUUID(),b.readVarInt()));
        @Override public Type<Warning> type(){return TYPE;}
    }
    private static SpaceVector vector(RegistryFriendlyByteBuf b){return new SpaceVector(b.readDouble(),b.readDouble(),b.readDouble());}
    private static void writeNode(RegistryFriendlyByteBuf b,ManeuverNode n){b.writeUUID(n.id());b.writeLong(n.tick());b.writeDouble(n.prograde());b.writeDouble(n.normal());b.writeDouble(n.radial());}
    private static ManeuverNode readNode(RegistryFriendlyByteBuf b){return new ManeuverNode(b.readUUID(),b.readLong(),b.readDouble(),b.readDouble(),b.readDouble());}
    private record Session(UUID nonce,UUID focus,BlockPos block,ResourceLocation dimension,long expires,long sequence){}
    private static final Map<ServerPlayer,Session> EDITORS=new WeakHashMap<>();
    @Override public Type<ManeuverPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("2").optional();r.playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->io.github.astelev.around_planet.client.ManeuverClient.accept(p)));r.playToServer(Edit.TYPE,Edit.CODEC,(p,c)->c.enqueueWork(()->edit((ServerPlayer)c.player(),p)));r.playToServer(Warning.TYPE,Warning.CODEC,(p,c)->c.enqueueWork(()->warning((ServerPlayer)c.player(),p)));}
    public static void open(ServerPlayer p,BlockPos block){
        if(p instanceof net.neoforged.neoforge.common.util.FakePlayer||!net.neoforged.fml.ModList.get().isLoaded("sable")||p.isSpectator()||!p.mayBuild()||!p.connection.hasChannel(TYPE))return;
        var focus=ManeuverAccess.focus(p);if(focus==null||!ManeuverAccess.near(p,block,focus.id())){p.displayClientMessage(net.minecraft.network.chat.Component.translatable("around_planet.maneuver.space_only"),true);return;}
        p.level().getBlockEntity(block);EDITORS.put(p,new Session(UUID.randomUUID(),focus.id(),block.immutable(),p.level().dimension().location(),p.level().getGameTime()+200,0));send(p,focus.id(),true,"");
    }
    public static void close(ServerPlayer p){EDITORS.remove(p);}
    public static void refresh(ServerPlayer p,UUID focus){var s=valid(p,focus);if(s!=null)EDITORS.put(p,new Session(s.nonce,s.focus,s.block,s.dimension,p.level().getGameTime()+200,s.sequence));send(p,focus,false,"");}
    private static Session valid(ServerPlayer p,UUID focus){var s=EDITORS.get(p);if(s==null||s.expires<p.level().getGameTime()||!s.focus.equals(focus)||!s.dimension.equals(p.level().dimension().location())||!ManeuverAccess.near(p,s.block,focus)||p.isSpectator()||!p.mayBuild()){EDITORS.remove(p);return null;}return s;}
    private static void send(ServerPlayer p,UUID focus,boolean open,String error){if(!p.connection.hasChannel(TYPE))return;var data=SpaceOrbitData.get(p.getServer());var path=data.get(focus);if(path.isEmpty())return;var s=valid(p,focus);PacketDistributor.sendToPlayer(p,new ManeuverPayload(focus,p.level().getGameTime(),path.get(),data.maneuvers(focus),s==null?NONE:s.nonce,s==null?0:s.sequence,open,error,s==null?0:p.level().getBlockEntity(s.block) instanceof io.github.astelev.around_planet.maneuver.ManeuverPlannerBlockEntity be?be.warningSeconds():30));}
    private static void warning(ServerPlayer p,Warning packet){if(packet.seconds<0||!net.neoforged.fml.ModList.get().isLoaded("sable"))return;var focus=ManeuverAccess.focus(p);if(focus==null)return;var s=valid(p,focus.id());if(s==null||!s.nonce.equals(packet.session))return;if(p.level().getBlockEntity(s.block) instanceof io.github.astelev.around_planet.maneuver.ManeuverPlannerBlockEntity be)be.configure(packet.seconds);send(p,focus.id(),false,"");}
    private static void edit(ServerPlayer p,Edit packet){
        if(!net.neoforged.fml.ModList.get().isLoaded("sable"))return;var focus=ManeuverAccess.focus(p);if(focus==null)return;var s=valid(p,focus.id());
        if(s==null||!s.nonce.equals(packet.session)||packet.sequence<=s.sequence)return;
        EDITORS.put(p,new Session(s.nonce,s.focus,s.block,s.dimension,s.expires,packet.sequence));var data=SpaceOrbitData.get(p.getServer());
        String error="";try{if(packet.delete)data.removeManeuver(focus.id(),packet.node.id());else{long delta=packet.node.tick()-p.level().getGameTime();if(delta<=0)throw new IllegalArgumentException("time");if(data.reentry(focus.id()).isPresent())throw new IllegalArgumentException("reentry");data.maneuver(focus.id(),packet.node);}}catch(IllegalArgumentException ex){error="around_planet.maneuver.rejected";}
        send(p,focus.id(),false,error);
    }
}
