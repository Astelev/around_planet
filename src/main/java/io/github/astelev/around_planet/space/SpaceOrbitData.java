package io.github.astelev.around_planet.space;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Persistent virtual trajectories, independent of cells, chunk tickets and Sable body lifetimes.
 * No automatic ticking: reading a dormant object's state never rewrites its epoch or dirties storage.
 * Future Sable activation/transfer adapters must explicitly bind stable object IDs to ship groups.
 */
public final class SpaceOrbitData extends SavedData {
    public static final String STORAGE_ID="around_planet_space_orbits";
    private final Map<UUID,Integer> forecastTurns=new HashMap<>();
    public int forecastTurns(UUID id){return forecastTurns.getOrDefault(id,1);}
    public void forecastTurns(UUID id,int turns){require(id);if(turns<1||turns>ForecastWindow.MAX_TURNS)throw new IllegalArgumentException("Forecast turns");forecastTurns.put(id,turns);setDirty();}
    private final Map<UUID,Integer> previewDepths=new HashMap<>();
    public int previewDepth(UUID id){return previewDepths.getOrDefault(id,6);}
    public void previewDepth(UUID id,int value){require(id);if(value<1||value>16)throw new IllegalArgumentException("Preview depth");previewDepths.put(id,value);setDirty();}
    private final Set<UUID> solarImpacts=new HashSet<>();
    public Set<UUID> solarImpacts(){return Set.copyOf(solarImpacts);}
    public void solarImpact(UUID id){require(id);if(solarImpacts.add(id))setDirty();}
    private CelestialSystem system;
    public CelestialSystem system(){return system;}
    public void system(CelestialSystem value){if(!Objects.equals(system,value)){system=value;setDirty();}}
    private final Map<UUID,SpaceTrajectory> objects=new HashMap<>();
    private final Map<UUID,UUID> targets=new HashMap<>();
    public SpaceState targetState(UUID focus,UUID target,long tick){var own=require(focus);if(CelestialSystem.body(target)){if(system==null)throw new IllegalArgumentException("Missing celestial system");return system.target(target,own.primary(),tick);}var state=sample(target,tick);var other=require(target);return system!=null&&system.managed(own.primary())&&system.managed(other.primary())?system.relative(own.primary(),system.global(other.primary(),state,tick),tick):state;}
    public Optional<UUID> target(UUID id){return Optional.ofNullable(targets.get(id)).filter(key->objects.containsKey(key)||CelestialSystem.body(key));}
    public void target(UUID id,UUID target){require(id);if(target==null)targets.remove(id);else{if(!CelestialSystem.body(target))require(target);if(id.equals(target))throw new IllegalArgumentException("Self target");targets.put(id,target);}setDirty();}
    private final Map<UUID,Long> warningFallback=new HashMap<>();
    public void warningFallback(UUID id,long ticks){warningFallback.put(id,ticks);}
    private final Map<UUID,List<ManeuverNode>> maneuvers=new HashMap<>();
    public List<ManeuverNode> maneuvers(UUID id){return maneuvers.getOrDefault(id,List.of());}
    public void maneuver(UUID orbit,ManeuverNode node){require(orbit);var nodes=new ArrayList<>(maneuvers(orbit));nodes.removeIf(n->n.id().equals(node.id()));if(nodes.size()>=ManeuverNode.MAX_NODES)throw new IllegalArgumentException("Too many maneuvers");nodes.add(node);nodes.sort(Comparator.comparingLong(ManeuverNode::tick).thenComparing(ManeuverNode::id));maneuvers.put(orbit,List.copyOf(nodes));setDirty();}
    public void removeManeuver(UUID orbit,UUID node){var nodes=new ArrayList<>(maneuvers(orbit));if(nodes.removeIf(n->n.id().equals(node))){maneuvers.put(orbit,List.copyOf(nodes));setDirty();}}
    private final Map<UUID,SpaceNavigation> navigation=new HashMap<>();
    private final Map<UUID,SpaceVector> reentries=new HashMap<>();
    private record SurfaceContact(SpaceVector world,SpaceVector relativeVelocity){}
    private final Map<UUID,SurfaceContact> surfaceContacts=new HashMap<>();
    public void reenterRotating(UUID id,SpaceVector contact,long tick,PlanetSpin spin){reenter(id,contact,tick);var planet=navigation.get(id).planet();surfaceContacts.put(id,new SurfaceContact(spin.surface(planet,contact),spin.landingVelocity(planet,contact,sample(id,tick).velocity())));setDirty();}
    /** Only parked handoffs follow the ground while asynchronous chunks become ready. */
    public Map<UUID,SpaceVector> followContacts(PlanetSpin spin,long tick){var impulses=new HashMap<UUID,SpaceVector>();
        for(var id:List.copyOf(reentries.keySet())){var nav=navigation.get(id);if(nav==null)continue;
            var old=sample(id,tick);var contact=surfaceContacts.computeIfAbsent(id,key->new SurfaceContact(spin.surface(nav.planet(),old.position()),spin.landingVelocity(nav.planet(),old.position(),old.velocity())));
            var next=spin.launch(nav.planet(),contact.world(),contact.relativeVelocity());reentries.put(id,next.position());objects.put(id,new SpaceTrajectory(require(id).primary(),tick,next,0));impulses.put(id,next.velocity().subtract(old.velocity()));setDirty();
        }return impulses;
    }
    public Optional<SpaceVector> reentry(UUID id){return Optional.ofNullable(reentries.get(id));}
    public void reenter(UUID id,SpaceVector contact,long tick){surfaceContacts.remove(id);var state=sample(id,tick);reentries.put(id,contact);put(id,new SpaceTrajectory(require(id).primary(),tick,new SpaceState(contact,state.velocity()),0));setDirty();}
    public Optional<SpaceNavigation> navigation(UUID id){return Optional.ofNullable(navigation.get(id));}
    public void navigate(UUID id,SpaceNavigation value){if(!value.equals(navigation.put(id,value)))setDirty();}
    public static SpaceOrbitData get(MinecraftServer server) {
        var storage=server.overworld().getDataStorage();
        var factory=new SavedData.Factory<>(SpaceOrbitData::new,SpaceOrbitData::load);
        var existing=storage.get(factory,STORAGE_ID);
        if(existing!=null)return existing;
        // Vanilla logs deserialization failures and returns null. Never replace an unreadable
        // existing catalog with a fresh empty one and later overwrite the only saved trajectories.
        var path=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data").resolve(STORAGE_ID+".dat");
        if(java.nio.file.Files.exists(path))throw new IllegalStateException("Existing orbit catalog could not be loaded: "+path);
        return storage.computeIfAbsent(factory,STORAGE_ID);
    }
    public Optional<SpaceTrajectory> get(UUID id){return Optional.ofNullable(objects.get(id));}
    public Set<UUID> ids(){return Set.copyOf(objects.keySet());}
    public SpaceState sample(UUID id,long gameTick){var state=require(id).at(gameTick);return reentries.containsKey(id)?new SpaceState(reentries.get(id),state.velocity()):state;}
    public void put(UUID id,SpaceTrajectory trajectory) {
        Objects.requireNonNull(id);Objects.requireNonNull(trajectory);if(CelestialSystem.body(id))throw new IllegalArgumentException("Celestial bodies have prescribed ephemerides");
        if(!trajectory.equals(objects.put(id,trajectory)))setDirty();
    }
    public void remove(UUID id){solarImpacts.remove(id);warningFallback.remove(id);forecastTurns.remove(id);previewDepths.remove(id);targets.remove(id);targets.values().removeIf(id::equals);if(maneuvers.remove(id)!=null | objects.remove(id)!=null | navigation.remove(id)!=null | reentries.remove(id)!=null | surfaceContacts.remove(id)!=null)setDirty();}
    public void impulse(UUID id,long gameTick,SpaceVector deltaVelocity){var before=require(id);var state=sample(id,gameTick);var parked=surfaceContacts.get(id);if(parked!=null){var p=navigation.get(id).planet();double phase=Math.atan2(state.position().x(),state.position().y())-parked.world().x()/p.radius();var frame=new PlanetSpin(phase,0).frame(p,parked.world().x(),parked.world().z());surfaceContacts.put(id,new SurfaceContact(parked.world(),parked.relativeVelocity().add(frame.toLocal(deltaVelocity))));}put(id,new SpaceTrajectory(require(id).primary(),gameTick,new SpaceState(state.position(),state.velocity().add(deltaVelocity)),require(id).mu()));
        var next=ManeuverGuidance.next(maneuvers(id),gameTick);
        if(next.isPresent()&&next.get().warningTicks(warningFallback.getOrDefault(id,600L))>0&&next.get().tick()-gameTick<=next.get().warningTicks(warningFallback.getOrDefault(id,600L))&&next.get().magnitude()>0){
            var node=next.get();var oldRef=PatchedConics.trace(before,gameTick,node.tick(),system).finalPath().at(node.tick());
            var newRef=PatchedConics.trace(require(id),gameTick,node.tick(),system).finalPath().at(node.tick());
            maneuver(id,node.afterImpulse(oldRef,newRef,deltaVelocity));
        }
    }
    /** New object inherits origin velocity plus local relative velocity, once, in one inertial chart. */
    public SpaceTrajectory detach(UUID source,UUID detached,long gameTick,SpaceFrame frame,SpaceState local) {
        if(objects.containsKey(detached))throw new IllegalArgumentException("Detached object ID already exists");
        var parent=require(source);
        var trajectory=new SpaceTrajectory(parent.primary(),gameTick,frame.detach(sample(source,gameTick),local),parent.mu());
        put(detached,trajectory);return trajectory;
    }
    /** Atomic virtual-time jump; Minecraft gameTime and physical cells remain untouched. */
    public void warp(long now,long ticks){if(ticks<0)throw new IllegalArgumentException("Negative orbital warp");if(ticks==0)return;long future=Math.addExact(now,ticks);var hitSun=new HashSet<UUID>();var advanced=new HashMap<UUID,SpaceTrajectory>();var plans=new HashMap<UUID,List<ManeuverNode>>();for(var e:objects.entrySet()){var path=e.getValue();var flight=reentries.containsKey(e.getKey())?null:PatchedConics.trace(path,now,future,system);var coast=flight==null?path:flight.finalPath();if(flight!=null&&flight.solarImpact()){hitSun.add(e.getKey());coast=new SpaceTrajectory(coast.primary(),future,coast.at(flight.legs().getLast().until()),0);}advanced.put(e.getKey(),reentries.containsKey(e.getKey())?path:new SpaceTrajectory(coast.primary(),now,coast.at(future),coast.mu()));}for(var e:maneuvers.entrySet()){var nodes=new ArrayList<ManeuverNode>();for(var n:e.getValue())nodes.add(new ManeuverNode(n.id(),Math.max(0,n.tick()-ticks),n.prograde(),n.normal(),n.radial(),n.warningSeconds()));plans.put(e.getKey(),List.copyOf(nodes));}solarImpacts.addAll(hitSun);if(system!=null)system=system.warp(ticks);objects.clear();objects.putAll(advanced);maneuvers.clear();maneuvers.putAll(plans);setDirty();}
    private SpaceTrajectory require(UUID id){var value=objects.get(id);if(value==null)throw new IllegalArgumentException("Unknown space object: "+id);return value;}
    public static SpaceOrbitData load(CompoundTag tag,HolderLookup.Provider lookup) {
        if(tag.getInt("schema")!=1 || !tag.contains("objects",Tag.TAG_LIST))throw new IllegalArgumentException("Unsupported orbit save format");
        var data=new SpaceOrbitData();if(tag.contains("celestialSystem",Tag.TAG_COMPOUND)){var s=tag.getCompound("celestialSystem");data.system=new CelestialSystem(s.getDouble("radius"),s.getDouble("mu"),s.getLong("clock"),s.contains("entry")?s.getDouble("entry"):2000);}var entries=(ListTag)tag.get("objects");
        if(!entries.isEmpty() && entries.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid orbit record list");
        for(var raw:entries) {
            var entry=(CompoundTag)raw;
            if(!entry.hasUUID("id") || !entry.contains("epoch",Tag.TAG_LONG) || !entry.contains("primary",Tag.TAG_STRING))throw new IllegalArgumentException("Incomplete orbit record");
            for(String key:new String[]{"mu","px","py","pz","vx","vy","vz"})
                if(!entry.contains(key,Tag.TAG_ANY_NUMERIC))throw new IllegalArgumentException("Missing orbit component: "+key);
            var state=new SpaceState(new SpaceVector(entry.getDouble("px"),entry.getDouble("py"),entry.getDouble("pz")),new SpaceVector(entry.getDouble("vx"),entry.getDouble("vy"),entry.getDouble("vz")));
            var trajectory=new SpaceTrajectory(entry.getString("primary"),entry.getLong("epoch"),state,entry.getDouble("mu"));
            if(entry.contains("reentry",Tag.TAG_COMPOUND))data.reentries.put(entry.getUUID("id"),readVector(entry.getCompound("reentry"),"p"));
            if(entry.contains("navigation",Tag.TAG_COMPOUND)) {
                var n=entry.getCompound("navigation");var frame=new SpaceFrame(readVector(n,"east"),readVector(n,"up"),readVector(n,"south"));
                data.navigation.put(entry.getUUID("id"),new SpaceNavigation(new PlanetGeometry(n.getDouble("halfX"),n.getDouble("halfZ")),frame));
            }
            if(entry.contains("surfaceContact",Tag.TAG_COMPOUND)){var n=entry.getCompound("surfaceContact");data.surfaceContacts.put(entry.getUUID("id"),new SurfaceContact(readVector(n,"world"),readVector(n,"velocity")));}
            if(entry.contains("forecastTurns"))data.forecastTurns.put(entry.getUUID("id"),Math.clamp(entry.getInt("forecastTurns"),1,ForecastWindow.MAX_TURNS));if(entry.contains("previewPatches"))data.previewDepths.put(entry.getUUID("id"),Math.clamp(entry.getInt("previewPatches"),1,16));if(entry.hasUUID("target"))data.targets.put(entry.getUUID("id"),entry.getUUID("target"));
            if(entry.contains("maneuvers",Tag.TAG_LIST)){var nodes=entry.getList("maneuvers",Tag.TAG_COMPOUND);var saved=new ArrayList<ManeuverNode>();var ids=new HashSet<UUID>();for(var rawNode:nodes){try{var n=(CompoundTag)rawNode;if(saved.size()>=ManeuverNode.MAX_NODES||!n.hasUUID("id")||!ids.add(n.getUUID("id")))continue;saved.add(new ManeuverNode(n.getUUID("id"),n.getLong("tick"),n.getDouble("prograde"),n.getDouble("normal"),n.getDouble("radial"),n.contains("warningTicks")?WarningDuration.seconds(n.getLong("warningTicks")):n.contains("warningSeconds")?n.getDouble("warningSeconds"):-1));}catch(IllegalArgumentException invalid){io.github.astelev.around_planet.AroundPlanet.LOGGER.warn("Skipping invalid saved maneuver for {}",entry.getUUID("id"));}}data.maneuvers.put(entry.getUUID("id"),List.copyOf(saved));}
            if(data.objects.putIfAbsent(entry.getUUID("id"),trajectory)!=null)throw new IllegalArgumentException("Duplicate orbit object ID");
        }
        for(var raw:tag.getList("solarImpacts",Tag.TAG_STRING)){var id=UUID.fromString(raw.getAsString());if(data.objects.containsKey(id))data.solarImpacts.add(id);}
        return data;
    }
    private static SpaceVector readVector(CompoundTag t,String key){for(String axis:new String[]{"x","y","z"})if(!t.contains(key+axis,Tag.TAG_ANY_NUMERIC))throw new IllegalArgumentException("Missing frame axis");return new SpaceVector(t.getDouble(key+"x"),t.getDouble(key+"y"),t.getDouble(key+"z"));}
    private static void writeVector(CompoundTag t,String key,SpaceVector v){t.putDouble(key+"x",v.x());t.putDouble(key+"y",v.y());t.putDouble(key+"z",v.z());}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup) {
        var solar=new ListTag();for(var id:solarImpacts)solar.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));tag.put("solarImpacts",solar);
        tag.putInt("schema",1);if(system!=null){var s=new CompoundTag();s.putDouble("radius",system.earthRadius());s.putDouble("mu",system.earthMu());s.putLong("clock",system.clockTicks());s.putDouble("entry",system.earthEntry());tag.put("celestialSystem",s);}var list=new ListTag();
        objects.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(item->{
            var e=new CompoundTag();var trajectory=item.getValue();var p=trajectory.initial().position();var v=trajectory.initial().velocity();
            e.putUUID("id",item.getKey());e.putString("primary",trajectory.primary());e.putLong("epoch",trajectory.epochTick());e.putDouble("mu",trajectory.mu());
            e.putDouble("px",p.x());e.putDouble("py",p.y());e.putDouble("pz",p.z());e.putDouble("vx",v.x());e.putDouble("vy",v.y());e.putDouble("vz",v.z());
            if(forecastTurns.containsKey(item.getKey()))e.putInt("forecastTurns",forecastTurns(item.getKey()));if(previewDepths.containsKey(item.getKey()))e.putInt("previewPatches",previewDepth(item.getKey()));if(targets.containsKey(item.getKey()))e.putUUID("target",targets.get(item.getKey()));
            var planned=new ListTag();for(var node:maneuvers(item.getKey())){var n=new CompoundTag();n.putUUID("id",node.id());n.putLong("tick",node.tick());n.putDouble("prograde",node.prograde());n.putDouble("normal",node.normal());n.putDouble("radial",node.radial());n.putLong("warningTicks",node.warningTicks(-1));planned.add(n);}if(!planned.isEmpty())e.put("maneuvers",planned);
            var parked=surfaceContacts.get(item.getKey());if(parked!=null){var n=new CompoundTag();writeVector(n,"world",parked.world());writeVector(n,"velocity",parked.relativeVelocity());e.put("surfaceContact",n);}
            var contact=reentries.get(item.getKey());if(contact!=null){var n=new CompoundTag();writeVector(n,"p",contact);e.put("reentry",n);}
            var nav=navigation.get(item.getKey());if(nav!=null){var n=new CompoundTag();n.putDouble("halfX",nav.planet().halfX());n.putDouble("halfZ",nav.planet().halfZ());writeVector(n,"east",nav.frame().east());writeVector(n,"up",nav.frame().up());writeVector(n,"south",nav.frame().south());e.put("navigation",n);}list.add(e);
        });tag.put("objects",list);return tag;
    }
}
