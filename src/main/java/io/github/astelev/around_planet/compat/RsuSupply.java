package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.propulsion.*;
import java.util.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
/** A per-substep grouping of loaded machines, never a chunk/plot scan. Detached ships cannot share fuel. */
public final class RsuSupply {
 public static Map<PropulsionBlockEntity,Double> allocate(SubLevelPhysicsSystem system,double dt){
  var level=system.getLevel();var groups=new IdentityHashMap<ServerSubLevel,List<PropulsionBlockEntity>>();var result=new IdentityHashMap<PropulsionBlockEntity,Double>();
  for(var be:PropulsionRuntime.loaded(level)){
   if(be.kind()!=PropulsionRegistry.Kind.RSU&&be.kind()!=PropulsionRegistry.Kind.DISTRIBUTOR)continue;
   if(be.kind()==PropulsionRegistry.Kind.RSU)be.rsuPower(0);
   if(be.isRemoved()||!level.hasChunkAt(be.getBlockPos())||level.getBlockEntity(be.getBlockPos())!=be)continue;
   var body=Sable.HELPER.getContaining(level,be.getBlockPos());if(!(body instanceof ServerSubLevel ship)||ship.isRemoved()||!system.getPhysicsHandle(ship).isValid()||ship.getMassTracker().isInvalid()||!(ship.getMassTracker().getMass()>0))continue;
   groups.computeIfAbsent(ship,k->new ArrayList<>()).add(be);
  }
  for(var machines:groups.values()){
   double requested=0;for(var be:machines)if(be.kind()==PropulsionRegistry.Kind.RSU)for(int i=0;i<4;i++)requested+=PropulsionMath.impulse(be.thrust(),be.signal(i),dt);
   if(requested<=0)continue;
   machines.sort(Comparator.comparingLong(be->be.getBlockPos().asLong()));double paid=0;
   for(var be:machines)if(be.kind()==PropulsionRegistry.Kind.DISTRIBUTOR&&paid<requested)paid+=be.rsuFuel.consume(requested-paid);
   double factor=Math.clamp(paid/requested,0,1);for(var be:machines)if(be.kind()==PropulsionRegistry.Kind.RSU){be.rsuPower(factor);result.put(be,factor);}
  }
  return result;
 }
 private RsuSupply(){}
}
