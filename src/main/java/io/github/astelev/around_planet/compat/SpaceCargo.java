package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.server.level.ServerLevel;
import java.util.*;
/** Capture the holding address after native serialization has completed, including free subobjects. */
public final class SpaceCargo {
    private record Pending(UUID cell,SpaceVector velocity,SpaceVector angular){}
    private static final Map<ServerLevel,Set<UUID>> RESTORED=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<UUID,Pending>> PENDING=new WeakHashMap<>();
    public static void capture(ServerSubLevel ship){var level=ship.getLevel();if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||!SpaceCells.hasCells(level))return;var binding=SpaceDormantData.get(level.getServer()).forShip(ship.getUniqueId());if(binding!=null){PENDING.computeIfAbsent(level,k->new HashMap<>()).put(ship.getUniqueId(),snapshot(ship,binding.orbit()));return;}var cell=SpaceCells.get(level).all().stream().filter(c->SpaceLocalPhysics.inCell(ship,c)).findFirst().orElse(null);if(cell!=null)PENDING.computeIfAbsent(level,k->new HashMap<>()).put(ship.getUniqueId(),snapshot(ship,cell.id()));}
    public static void saved(ServerLevel level,net.minecraft.world.level.ChunkPos chunk){var pending=PENDING.remove(level);if(pending==null)return;var c=SubLevelContainer.getContainer(level);var data=SpaceCargoData.get(level.getServer());for(var e:pending.entrySet()){var held=c.getHoldingChunkMap().getHoldingSubLevel(e.getKey());if(held==null)continue;RESTORED.computeIfAbsent(level,k->new HashSet<>()).remove(e.getKey());data.put(new SpaceCargoData.Cargo(e.getKey(),e.getValue().cell(),chunk.toLong(),(short)0,(short)0,e.getValue().velocity(),e.getValue().angular()));}}
    public static void clear(ServerLevel level){PENDING.remove(level);RESTORED.remove(level);}
    private static Pending snapshot(ServerSubLevel ship,UUID cell){var pipe=SubLevelContainer.getContainer(ship.getLevel()).physicsSystem().getPipeline();return new Pending(cell,vector(pipe.getLinearVelocity(ship,new org.joml.Vector3d())),vector(pipe.getAngularVelocity(ship,new org.joml.Vector3d())));}
    /** Native holding does not preserve pipeline motion. Restore the saved local motion once. */
    public static void prepare(dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer c){var level=c.getLevel();if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||!SpaceCells.hasCells(level))return;var restored=RESTORED.computeIfAbsent(level,k->new HashSet<>());var data=SpaceCargoData.get(level.getServer());var ids=data.all().stream().map(SpaceCargoData.Cargo::ship).collect(java.util.stream.Collectors.toSet());restored.retainAll(ids);for(var record:data.all()){if(SpaceOrbitData.get(level.getServer()).get(record.cell()).isEmpty()){data.remove(record.ship());continue;}var ship=(ServerSubLevel)c.getSubLevel(record.ship());if(ship==null||ship.isRemoved()||!c.physicsSystem().getPhysicsHandle(ship).isValid()||!restored.add(record.ship()))continue;if(SpaceDormant.bound(ship))continue;var pipe=c.physicsSystem().getPipeline();pipe.addLinearAndAngularVelocity(ship,vec(record.velocity()).sub(pipe.getLinearVelocity(ship,new org.joml.Vector3d())),vec(record.angular()).sub(pipe.getAngularVelocity(ship,new org.joml.Vector3d())));}}
    private static SpaceVector vector(org.joml.Vector3d v){return new SpaceVector(v.x,v.y,v.z);}
    private static org.joml.Vector3d vec(SpaceVector v){return new org.joml.Vector3d(v.x(),v.y(),v.z());}
    private SpaceCargo(){}
}
