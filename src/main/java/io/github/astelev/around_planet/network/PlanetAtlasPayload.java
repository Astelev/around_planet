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
    private static final Map<ServerPlayer,PlanetAtlasData> SENT=new WeakHashMap<>();
    private static final Map<ServerPlayer,Integer> SIZES=new WeakHashMap<>();
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").optional().playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->io.github.astelev.around_planet.client.PlanetAtlasTexture.accept(p)));}
    @Override public Type<PlanetAtlasPayload> type(){return TYPE;}
    @SubscribeEvent public static void tick(LevelTickEvent.Post e){
        if(!(e.getLevel() instanceof net.minecraft.server.level.ServerLevel level)||level.dimension()!=Level.OVERWORLD||!PlanetConfig.SPEC.isLoaded()||!PlanetConfig.ENABLED.getAsBoolean()||level.getServer().getPlayerList().getPlayers().isEmpty())return;
        var atlas=PlanetAtlasData.get(level);atlas.validate(level,PlanetConfig.PLANET_SIZE.get());atlas.advance(level);if(!atlas.ready())return;
        for(var player:level.getServer().getPlayerList().getPlayers())if(player.connection.hasChannel(TYPE)&&(SENT.get(player)!=atlas||!Objects.equals(SIZES.get(player),atlas.circumference()))){PacketDistributor.sendToPlayer(player,new PlanetAtlasPayload(atlas.circumference(),atlas.pixels()));SENT.put(player,atlas);SIZES.put(player,atlas.circumference());}
    }
}
