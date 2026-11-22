package io.github.astelev.around_planet.network;
import io.github.astelev.around_planet.atlas.*;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
/** One bounded atlas per login/revision, not a recurring render snapshot. */
@EventBusSubscriber(modid="around_planet")
public record PlanetAtlasPayload(int circumference,int[] pixels) implements CustomPacketPayload {
    public static final Type<PlanetAtlasPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","planet_atlas"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PlanetAtlasPayload> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.circumference);for(int color:p.pixels)b.writeInt(color);},b->{int size=b.readVarInt();if(size<256||size>59_000_000||b.readableBytes()!=PlanetAtlas.SIZE*4)throw new IllegalArgumentException("Planet atlas size");int[] pixels=new int[PlanetAtlas.SIZE];for(int i=0;i<pixels.length;i++)pixels[i]=b.readInt();return new PlanetAtlasPayload(size,pixels);});
    public record Part(int circumference,int offset,int[] pixels) implements CustomPacketPayload {
        public static final int CHUNK=65536;
        public static final Type<Part> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","planet_atlas_part"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Part> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.circumference);b.writeVarInt(p.offset);b.writeVarInt(p.pixels.length);for(int color:p.pixels)b.writeInt(color);},b->{int c=b.readVarInt(),offset=b.readVarInt(),n=b.readVarInt();if(c<256||c>59000000||offset<0||offset%CHUNK!=0||n!=Math.min(CHUNK,PlanetAtlas.SIZE-offset)||n<=0)throw new IllegalArgumentException("Atlas chunk bounds");int[] pixels=new int[n];for(int i=0;i<n;i++)pixels[i]=b.readInt();return new Part(c,offset,pixels);});
        @Override public Type<Part> type(){return TYPE;}
    }
    private static final Map<ServerPlayer,PlanetAtlasData> SENT=new WeakHashMap<>();
    private static final Map<ServerPlayer,Integer> SIZES=new WeakHashMap<>();
    public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("2").optional();r.playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->io.github.astelev.around_planet.client.PlanetAtlasTexture.accept(p)));r.playToClient(Part.TYPE,Part.CODEC,(p,c)->c.enqueueWork(()->io.github.astelev.around_planet.client.PlanetAtlasTexture.acceptPart(p)));}
    @Override public Type<PlanetAtlasPayload> type(){return TYPE;}
    @SubscribeEvent public static void tick(LevelTickEvent.Post e){
        if(!(e.getLevel() instanceof net.minecraft.server.level.ServerLevel level)||level.dimension()!=Level.OVERWORLD||!PlanetConfig.SPEC.isLoaded()||!PlanetConfig.ENABLED.getAsBoolean()||level.getServer().getPlayerList().getPlayers().isEmpty())return;
        var atlas=PlanetAtlasData.get(level);atlas.validate(level,PlanetConfig.PLANET_SIZE.get());atlas.advance(level);if(!atlas.ready())return;
        for(var player:level.getServer().getPlayerList().getPlayers())if(player.connection.hasChannel(Part.TYPE)&&(SENT.get(player)!=atlas||!Objects.equals(SIZES.get(player),atlas.circumference()))){for(int offset=0;offset<PlanetAtlas.SIZE;offset+=Part.CHUNK)PacketDistributor.sendToPlayer(player,new Part(atlas.circumference(),offset,Arrays.copyOfRange(atlas.pixels(),offset,Math.min(PlanetAtlas.SIZE,offset+Part.CHUNK))));SENT.put(player,atlas);SIZES.put(player,atlas.circumference());}
    }
}
