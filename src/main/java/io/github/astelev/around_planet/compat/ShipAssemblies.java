package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Per-scene rigid joint connectivity; grounded/unknown endpoints prevent an incomplete transfer. */
public final class ShipAssemblies {
    private record Link(PhysicsPipelineBody a,PhysicsPipelineBody b,PhysicsConstraintHandle handle){}
    public record Group(List<ServerSubLevel> ships,boolean anchored){}
    private static final Map<ServerLevel,Map<PhysicsConstraintHandle,Link>> LINKS=new IdentityHashMap<>();
    private static final Map<Object,Map<Object,ServerSubLevel>> ROPES=new IdentityHashMap<>();
    public static synchronized void rope(Object handle,Object point,ServerSubLevel body) {
        var endpoints=ROPES.computeIfAbsent(handle,k->new HashMap<>());
        if(body==null)endpoints.remove(point);else endpoints.put(point,body);
        if(endpoints.isEmpty())ROPES.remove(handle);
    }
    public static synchronized void added(ServerLevel level,PhysicsPipelineBody a,PhysicsPipelineBody b,PhysicsConstraintHandle handle) {
        if(handle!=null)LINKS.computeIfAbsent(level,l->new IdentityHashMap<>()).put(handle,new Link(a,b,handle));
    }
    public static synchronized void removed(Object handle){for(var links:LINKS.values())links.remove(handle);ROPES.remove(handle);}
    public static synchronized void bodyRemoved(ServerLevel level,PhysicsPipelineBody body) {
        var links=LINKS.get(level);if(links!=null)links.values().removeIf(l->l.a==body || l.b==body);
        for(var e:ROPES.values())e.values().removeIf(s->s==body);ROPES.values().removeIf(Map::isEmpty);
    }
    public static synchronized void clear(ServerLevel level){LINKS.remove(level);ROPES.values().removeIf(e->e.values().stream().anyMatch(s->s.getLevel()==level));}
    public static synchronized List<Group> groups(ServerLevel level,Iterable<ServerSubLevel> ships) {
        var links=LINKS.get(level);
        var adjacent=new IdentityHashMap<PhysicsPipelineBody,List<PhysicsPipelineBody>>();
        var anchors=Collections.newSetFromMap(new IdentityHashMap<PhysicsPipelineBody,Boolean>());
        for(var endpoints:ROPES.values())anchors.addAll(endpoints.values());
        if(links!=null) {
            links.values().removeIf(l->l.a!=null && l.a.isRemoved() || l.b!=null && l.b.isRemoved() || !l.handle.isValid());
            for(var l:links.values()) {
                if(l.a==null){if(l.b!=null)anchors.add(l.b);continue;}
                if(l.b==null){anchors.add(l.a);continue;}
                adjacent.computeIfAbsent(l.a,k->new ArrayList<>()).add(l.b);adjacent.computeIfAbsent(l.b,k->new ArrayList<>()).add(l.a);
            }
        }
        var seen=Collections.newSetFromMap(new IdentityHashMap<PhysicsPipelineBody,Boolean>());var result=new ArrayList<Group>();
        for(var ship:ships)if(!ship.isRemoved() && seen.add(ship)) {
            var queue=new ArrayDeque<PhysicsPipelineBody>();queue.add(ship);var members=new ArrayList<ServerSubLevel>();boolean anchored=false;
            while(!queue.isEmpty()) {
                var body=queue.removeFirst();anchored|=anchors.contains(body);
                if(body instanceof ServerSubLevel member)members.add(member);else anchored=true;
                for(var next:adjacent.getOrDefault(body,List.of()))if(seen.add(next))queue.add(next);
            }
            result.add(new Group(List.copyOf(members),anchored));
        }
        return result;
    }
    private ShipAssemblies(){}
}
