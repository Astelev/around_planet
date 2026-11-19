package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.propulsion.PropulsionBlockEntity;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.*;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.*;
/** Optional Create API: four independent native receivers, each with its own item pair. */
public final class RsuLinkBridge {
    private record Receiver(PropulsionBlockEntity be,int port,Couple<RedstoneLinkNetworkHandler.Frequency> key) implements IRedstoneLinkable {
        public int getTransmittedStrength(){return 0;}
        public void setReceivedStrength(int value){be.receiveSignal(port,value);}
        public boolean isListening(){return true;}
        public boolean isAlive(){return !be.isRemoved()&&be.getLevel()!=null&&be.getLevel().hasChunkAt(be.getBlockPos())&&be.getLevel().getBlockEntity(be.getBlockPos())==be;}
        public Couple<RedstoneLinkNetworkHandler.Frequency> getNetworkKey(){return key;}
        public BlockPos getLocation(){if(net.neoforged.fml.ModList.get().isLoaded("sable")){var p=SpaceSensorNavigation.worldPoint(be.getLevel(),be.getBlockPos());return BlockPos.containing(p.x,p.y,p.z);}return be.getBlockPos();}
    }
    private record Session(Level level,List<Receiver> receivers){}
    public static Object attach(PropulsionBlockEntity be){var receivers=new ArrayList<Receiver>();for(int i=0;i<4;i++){var a=be.frequency(i*2);var b=be.frequency(i*2+1);if(a.isEmpty()&&b.isEmpty())continue;var receiver=new Receiver(be,i,Couple.create(RedstoneLinkNetworkHandler.Frequency.of(a),RedstoneLinkNetworkHandler.Frequency.of(b)));receivers.add(receiver);Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(be.getLevel(),receiver);}return new Session(be.getLevel(),receivers);}
    public static void refresh(Object handle){var s=(Session)handle;for(var r:s.receivers)Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(s.level,r);}
    public static void detach(Object handle){var s=(Session)handle;for(var r:s.receivers)Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(s.level,r);}
    private RsuLinkBridge(){}
}
