package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.network.SeamVehiclePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.LinkedHashMap;
import java.util.Map;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamVehicleCorrection {
    private record Pending(SeamVehiclePayload pose,long expires) {}
    private static final Map<Integer,Pending> PENDING=new LinkedHashMap<>();
    private static long ticks;
    private static net.minecraft.client.multiplayer.ClientLevel world;
    public static void accept(SeamVehiclePayload pose) {
        var level=Minecraft.getInstance().level;
        if(world!=level){PENDING.clear();world=level;}
        if(PENDING.size()>=64)PENDING.remove(PENDING.keySet().iterator().next());
        PENDING.put(pose.id(),new Pending(pose,ticks+200));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        ticks++;var level=Minecraft.getInstance().level;
        if(world!=level){PENDING.clear();world=level;return;}
        PENDING.values().removeIf(p->{
            var s=p.pose;
            if(level==null || !level.dimension().location().equals(s.dimension()) || ticks>p.expires)return true;
            var entity=level.getEntity(s.id());
            if(entity==null || !entity.getUUID().equals(s.uuid()))return false;
            // Pairing/spawn and destination chunk packets may follow the player's teleport.
            // Apply after those, not while Sable/vanilla are still initializing a controlled vehicle at its old pose.
            var player=Minecraft.getInstance().player;
            if(player==null || player.getRootVehicle()!=entity || level.getChunkSource().getChunk(
                    (int)Math.floor(s.x()/16),(int)Math.floor(s.z()/16),net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)==null)return false;
            entity.lerpTo(s.x(),s.y(),s.z(),s.yaw(),s.pitch(),0);
            entity.absMoveTo(s.x(),s.y(),s.z(),s.yaw(),s.pitch());
            entity.setOldPosAndRot();entity.syncPacketPositionCodec(s.x(),s.y(),s.z());
            return true;
        });
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if(event.getLevel()==world){PENDING.clear();world=null;}
    }
    private SeamVehicleCorrection() {}
}
