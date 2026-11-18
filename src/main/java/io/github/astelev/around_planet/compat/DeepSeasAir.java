package io.github.astelev.around_planet.compat;
import com.maxenonyme.createsubmarine.submarine.compartment.CompartmentTracker;
import com.maxenonyme.createsubmarine.submarine.block.entity.OxygeneDiffuserBlockEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
/** Read-only room-level bridge. Deep Seas owns topology, gas storage, consumption and invalidation. */
public final class DeepSeasAir {
    private record Emitters(long tick,List<BlockPos> positions){}
    private static final Map<ServerLevel,Map<UUID,Emitters>> CACHE=new WeakHashMap<>();
    public static boolean breathable(LivingEntity entity){
        if(!(entity.level() instanceof ServerLevel level))return false;
        var container=SubLevelContainer.getContainer(level);if(container==null)return false;
        var world=VacuumSable.worldEye(entity);
        for(var ship:container.getAllSubLevels()){
            if(ship.isRemoved())continue;var bounds=ship.boundingBox();
            if(world.x<bounds.minX()||world.x>bounds.maxX()||world.y<bounds.minY()||world.y>bounds.maxY()||world.z<bounds.minZ()||world.z>bounds.maxZ())continue;
            var id=ship.getUniqueId();var local=ship.logicalPose().transformPositionInverse(new org.joml.Vector3d(world));var eye=BlockPos.containing(local.x,local.y,local.z);
            for(var room:CompartmentTracker.getCompartments(id)){
                if(!room.sealed()||!room.internal().contains(eye)||CompartmentTracker.isBreached(id,room))continue;
                for(var pos:emitters(level,ship)){
                    if(!room.internal().contains(pos)&&!room.internal().contains(pos.above()))continue;
                    // Always verify current fuel and redstone. A drained or disabled diffuser must
                    // not grant an extra cache interval of breathable air.
                    if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof OxygeneDiffuserBlockEntity diffuser&&diffuser.oxygenTank.getFluidAmount()>0&&level.hasNeighborSignal(pos))return true;
                }
            }
        }
        return false;
    }
    private static List<BlockPos> emitters(ServerLevel level,ServerSubLevel ship){
        var cache=CACHE.computeIfAbsent(level,k->new HashMap<>());var old=cache.get(ship.getUniqueId());long now=level.getGameTime();
        if(old!=null&&now-old.tick()<5)return old.positions();
        cache.keySet().removeIf(id->SubLevelContainer.getContainer(level).getSubLevel(id)==null);
        var positions=new ArrayList<BlockPos>();for(var holder:ship.getPlot().getLoadedChunks())for(var be:holder.getChunk().getBlockEntities().values())if(be instanceof OxygeneDiffuserBlockEntity&&!be.isRemoved())positions.add(be.getBlockPos().immutable());
        var sample=new Emitters(now,List.copyOf(positions));cache.put(ship.getUniqueId(),sample);return sample.positions();
    }
    public static void clear(ServerLevel level){CACHE.remove(level);}
    private DeepSeasAir(){}
}
