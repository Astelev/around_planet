package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.PlanetConfig;
import dev.ryanhcode.sable.api.sublevel.*;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import java.util.*;
/** Snapshot before native unload; stage a coherent group on native observation before assigning an active cell. */
public final class SpaceDormant {
    private static final Map<ServerLevel,Set<UUID>> STARTED=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<UUID,SpaceVector>> LAST=new WeakHashMap<>();
    public static void clear(ServerLevel level){STARTED.remove(level);LAST.remove(level);}
    public static boolean bound(ServerSubLevel ship){return SpaceDormantData.get(ship.getLevel().getServer()).forShip(ship.getUniqueId())!=null;}
    public static void capture(ServerSubLevel ship,List<UUID> dependencies){
        var level=ship.getLevel();if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD) || !PlanetConfig.SPEC.isLoaded() || !SpaceCells.hasCells(level))return;
        var c=SubLevelContainer.getContainer(level);var registry=SpaceDormantData.get(level.getServer());if(registry.forShip(ship.getUniqueId())!=null || SpaceCells.containsShip(level,ship.getUniqueId()))return;
        var group=ShipAssemblies.groups(level,c.getAllSubLevels()).stream().filter(g->g.ships().contains(ship)).findFirst().orElse(null);
        if(group==null || group.anchored() || group.ships().size()>256 || group.ships().stream().anyMatch(s->registry.forShip(s.getUniqueId())!=null || SpaceCells.containsShip(level,s.getUniqueId()) || !c.physicsSystem().getPhysicsHandle(s).isValid()) || dependencies.stream().anyMatch(id->SpaceCells.containsShip(level,id)))return;
        var center=SpaceDetachment.center(group.ships());if(center==null)return;var cell=SpaceCells.get(level).all().stream().filter(x->x.contains(center.x,center.y,center.z)).findFirst().orElse(null);if(cell==null)return;
        var velocity=SpaceDetachment.velocity(c,group.ships());if(velocity.length()<=PlanetConfig.SPACE_UNLOAD_SPEED.get())return;
        var orbits=SpaceOrbitData.get(level.getServer());if(orbits.get(cell.id()).isEmpty() || orbits.reentry(cell.id()).isPresent())return;
        var nav=SpaceLocalPhysics.navigation(level,cell);var id=UUID.randomUUID();var local=new SpaceState(new SpaceVector(center.x-cell.x(),center.y-cell.y(),center.z-cell.z()),vector(velocity));
        var members=group.ships().stream().map(s->new SpaceDormantData.Member(s.getUniqueId(),vector(new Vector3d(s.logicalPose().position())),vector(c.physicsSystem().getPipeline().getLinearVelocity(s,new Vector3d()).sub(velocity)),vector(c.physicsSystem().getPipeline().getAngularVelocity(s,new Vector3d())))).toList();
        orbits.detach(cell.id(),id,level.getGameTime(),nav.frame(),local);orbits.navigate(id,nav);
        registry.put(new SpaceDormantData.Binding(id,ship.getName()==null?"Object "+id.toString().substring(0,8):ship.getName(),vector(center),cell.originY(),members));
        LAST.computeIfAbsent(level,k->new HashMap<>()).put(id,orbits.sample(id,level.getGameTime()).position());STARTED.computeIfAbsent(level,k->new HashSet<>()).remove(id);
        io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_UNLOAD_DETACH orbit={} bodies={} relativeSpeed={}",id,members.size(),velocity.length());
    }
    private static List<ServerSubLevel> loaded(ServerSubLevelContainer c,SpaceDormantData.Binding b){var result=new ArrayList<ServerSubLevel>();for(var m:b.members()){var s=c.getSubLevel(m.ship());if(s instanceof ServerSubLevel ship && !ship.isRemoved() && c.physicsSystem().getPhysicsHandle(ship).isValid())result.add(ship);}return result;}
    private static List<SpaceDormantData.Binding> active(ServerSubLevelContainer c){var registry=SpaceDormantData.get(c.getLevel().getServer());var result=new LinkedHashSet<SpaceDormantData.Binding>();for(var ship:c.getAllSubLevels())if(!ship.isRemoved()){var b=registry.forShip(ship.getUniqueId());if(b!=null)result.add(b);}return List.copyOf(result);}
    /** Before queued engine forces: restore each saved internal velocity exactly once on coherent activation. */
    public static void prepare(ServerSubLevelContainer c){var level=c.getLevel();if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return;
        var started=STARTED.computeIfAbsent(level,k->new HashSet<>());
        for(var b:active(c)){var ships=loaded(c,b);if(ships.size()!=b.members().size()){started.remove(b.orbit());continue;}
            if(started.add(b.orbit()))for(var s:ships){var m=b.members().stream().filter(n->n.ship().equals(s.getUniqueId())).findFirst().orElseThrow();var desired=new Vector3d(m.relativeVelocity().x(),m.relativeVelocity().y(),m.relativeVelocity().z());c.physicsSystem().getPipeline().addLinearAndAngularVelocity(s,desired.sub(c.physicsSystem().getPipeline().getLinearVelocity(s,new Vector3d())),new Vector3d(m.angularVelocity().x(),m.angularVelocity().y(),m.angularVelocity().z()).sub(c.physicsSystem().getPipeline().getAngularVelocity(s,new Vector3d())));}
        }
    }
    public static void before(SubLevelPhysicsSystem system,double dt){var level=system.getLevel();if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return;var c=SubLevelContainer.getContainer(level);var pipeline=system.getPipeline();var anti=new Vector3d(DimensionPhysicsData.getGravity(level)).mul(-dt);var data=SpaceOrbitData.get(level.getServer());
        for(var b:active(c)){var ships=loaded(c,b);boolean complete=ships.size()==b.members().size();var common=complete?SpaceDetachment.velocity(c,ships):new Vector3d();
            if(complete && common.lengthSquared()>1e-10 && data.get(b.orbit()).isPresent())data.impulse(b.orbit(),level.getGameTime(),data.navigation(b.orbit()).orElseThrow().frame().toCosmic(vector(common)));
            for(var s:ships)pipeline.addLinearAndAngularVelocity(s,new Vector3d(anti).sub(complete?common:pipeline.getLinearVelocity(s,new Vector3d())),new Vector3d());
        }
    }
    public static void after(SubLevelPhysicsSystem system){var level=system.getLevel();if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return;var c=SubLevelContainer.getContainer(level);
        for(var b:active(c)){var ships=loaded(c,b);if(ships.isEmpty())continue;boolean complete=ships.size()==b.members().size();var center=complete?SpaceDetachment.center(ships):null;
            for(var ship:ships){var target=complete?new Vector3d(b.anchor().x(),b.anchor().y(),b.anchor().z()).sub(center).add(ship.logicalPose().position()):new Vector3d(b.members().stream().filter(m->m.ship().equals(ship.getUniqueId())).findFirst().orElseThrow().position().x(),b.members().stream().filter(m->m.ship().equals(ship.getUniqueId())).findFirst().orElseThrow().position().y(),b.members().stream().filter(m->m.ship().equals(ship.getUniqueId())).findFirst().orElseThrow().position().z());var delta=new Vector3d(target).sub(ship.logicalPose().position());if(delta.lengthSquared()<1e-12)continue;system.getPipeline().teleport(ship,target,ship.logicalPose().orientation());((ShipPoseHistory)ship).around_planet$shiftHistory(delta.x,delta.y,delta.z);ship.updateBoundingBox();}
        }
    }
    public static void check(ServerLevel level,ServerSubLevelContainer c){prepare(c);var registry=SpaceDormantData.get(level.getServer());var data=SpaceOrbitData.get(level.getServer());var cells=SpaceCells.get(level);var last=LAST.computeIfAbsent(level,k->new HashMap<>());
        for(var b:level.getGameTime()%20==0?registry.all():active(c)){
            if(data.get(b.orbit()).isEmpty())continue;var nav=data.navigation(b.orbit()).orElseThrow();SpaceAtmosphere.advance(level,b.orbit(),null);var state=data.sample(b.orbit(),level.getGameTime());double shell=SpaceAtmosphere.returnAltitude();
            if(data.reentry(b.orbit()).isEmpty()){var prev=last.put(b.orbit(),state.position());var contact=nav.planet().inwardCrossing(prev==null?data.get(b.orbit()).orElseThrow().initial().position():prev,state.position(),shell).orElse(null);if(contact==null && state.position().length()<nav.planet().radius()+shell && state.position().dot(state.velocity())<0)contact=state.position().scale((nav.planet().radius()+shell)/state.position().length());if(contact!=null){SpaceAtmosphere.handoff(level,b.orbit(),null);data.reenter(b.orbit(),contact,level.getGameTime());}}
            var ships=loaded(c,b);if(ships.size()!=b.members().size())continue;
            var ids=b.members().stream().map(SpaceDormantData.Member::ship).collect(java.util.stream.Collectors.toSet());
            var group=ShipAssemblies.groups(level,c.getAllSubLevels()).stream().filter(g->g.ships().contains(ships.getFirst())).findFirst().orElse(null);
            if(group==null || group.anchored() || !ids.equals(group.ships().stream().map(ServerSubLevel::getUniqueId).collect(java.util.stream.Collectors.toSet())))continue;
            var candidate=SpaceShipTransfers.allocateDetached(level,ships,SpaceDetachment.center(ships));if(candidate==null)continue;
            var cell=new SpaceCells.Cell(b.orbit(),candidate.slot(),candidate.x(),candidate.y(),candidate.z(),candidate.originX(),b.returnAltitude(),candidate.originZ(),b.members().stream().map(SpaceDormantData.Member::ship).toList());var center=SpaceDetachment.center(ships);var delta=new Vector3d(cell.x(),cell.y(),cell.z()).sub(center);
            if(!SpaceShipTransfers.fits(level,ships,delta,cell) || !SpaceShipTransfers.ready(level,ships,delta))continue;
            var roots=SpaceShipTransfers.roots(level,ships);if(roots.stream().flatMap(net.minecraft.world.entity.Entity::getSelfAndPassengers).filter(net.minecraft.server.level.ServerPlayer.class::isInstance).map(net.minecraft.server.level.ServerPlayer.class::cast).anyMatch(p->!(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && !p.connection.hasChannel(io.github.astelev.around_planet.network.SpaceTransferPayload.TYPE)))continue;
            var moving=Collections.newSetFromMap(new IdentityHashMap<net.minecraft.world.entity.Entity,Boolean>());for(var root:roots)root.getSelfAndPassengers().forEach(moving::add);boolean leash=false;for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.Mob mob && mob.isLeashed() && moving.contains(mob)!=moving.contains(mob.getLeashHolder())){leash=true;break;}if(leash)continue;
            cells.add(level,cell);SpaceShipTransfers.move(level,c,ships,roots,delta,cells.all());registry.remove(b.orbit());STARTED.computeIfAbsent(level,k->new HashSet<>()).remove(b.orbit());last.remove(b.orbit());SpaceReentry.entered(level,cell.id(),data.sample(cell.id(),level.getGameTime()).position());
            io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_DORMANT_ACTIVATE orbit={} bodies={}",cell.id(),ships.size());
        }
    }
    private static SpaceVector vector(Vector3d v){return new SpaceVector(v.x,v.y,v.z);}
    private SpaceDormant(){}
}
