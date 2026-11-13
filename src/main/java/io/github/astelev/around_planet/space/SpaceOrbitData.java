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
    private final Map<UUID,SpaceTrajectory> objects=new HashMap<>();
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
        Objects.requireNonNull(id);Objects.requireNonNull(trajectory);
        if(!trajectory.equals(objects.put(id,trajectory)))setDirty();
    }
    public void remove(UUID id){if(objects.remove(id)!=null | navigation.remove(id)!=null | reentries.remove(id)!=null | surfaceContacts.remove(id)!=null)setDirty();}
    public void impulse(UUID id,long gameTick,SpaceVector deltaVelocity){var state=sample(id,gameTick);var parked=surfaceContacts.get(id);if(parked!=null){var p=navigation.get(id).planet();double phase=Math.atan2(state.position().x(),state.position().y())-parked.world().x()/p.radius();var frame=new PlanetSpin(phase,0).frame(p,parked.world().x(),parked.world().z());surfaceContacts.put(id,new SurfaceContact(parked.world(),parked.relativeVelocity().add(frame.toLocal(deltaVelocity))));}put(id,new SpaceTrajectory(require(id).primary(),gameTick,new SpaceState(state.position(),state.velocity().add(deltaVelocity)),require(id).mu()));}
    /** New object inherits origin velocity plus local relative velocity, once, in one inertial chart. */
    public SpaceTrajectory detach(UUID source,UUID detached,long gameTick,SpaceFrame frame,SpaceState local) {
        if(objects.containsKey(detached))throw new IllegalArgumentException("Detached object ID already exists");
        var parent=require(source);
        var trajectory=new SpaceTrajectory(parent.primary(),gameTick,frame.detach(sample(source,gameTick),local),parent.mu());
        put(detached,trajectory);return trajectory;
    }
    private SpaceTrajectory require(UUID id){var value=objects.get(id);if(value==null)throw new IllegalArgumentException("Unknown space object: "+id);return value;}
    public static SpaceOrbitData load(CompoundTag tag,HolderLookup.Provider lookup) {
        if(tag.getInt("schema")!=1 || !tag.contains("objects",Tag.TAG_LIST))throw new IllegalArgumentException("Unsupported orbit save format");
        var data=new SpaceOrbitData();var entries=(ListTag)tag.get("objects");
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
            if(data.objects.putIfAbsent(entry.getUUID("id"),trajectory)!=null)throw new IllegalArgumentException("Duplicate orbit object ID");
        }
        return data;
    }
    private static SpaceVector readVector(CompoundTag t,String key){for(String axis:new String[]{"x","y","z"})if(!t.contains(key+axis,Tag.TAG_ANY_NUMERIC))throw new IllegalArgumentException("Missing frame axis");return new SpaceVector(t.getDouble(key+"x"),t.getDouble(key+"y"),t.getDouble(key+"z"));}
    private static void writeVector(CompoundTag t,String key,SpaceVector v){t.putDouble(key+"x",v.x());t.putDouble(key+"y",v.y());t.putDouble(key+"z",v.z());}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup) {
        tag.putInt("schema",1);var list=new ListTag();
        objects.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(item->{
            var e=new CompoundTag();var trajectory=item.getValue();var p=trajectory.initial().position();var v=trajectory.initial().velocity();
            e.putUUID("id",item.getKey());e.putString("primary",trajectory.primary());e.putLong("epoch",trajectory.epochTick());e.putDouble("mu",trajectory.mu());
            e.putDouble("px",p.x());e.putDouble("py",p.y());e.putDouble("pz",p.z());e.putDouble("vx",v.x());e.putDouble("vy",v.y());e.putDouble("vz",v.z());
            var parked=surfaceContacts.get(item.getKey());if(parked!=null){var n=new CompoundTag();writeVector(n,"world",parked.world());writeVector(n,"velocity",parked.relativeVelocity());e.put("surfaceContact",n);}
            var contact=reentries.get(item.getKey());if(contact!=null){var n=new CompoundTag();writeVector(n,"p",contact);e.put("reentry",n);}
            var nav=navigation.get(item.getKey());if(nav!=null){var n=new CompoundTag();n.putDouble("halfX",nav.planet().halfX());n.putDouble("halfZ",nav.planet().halfZ());writeVector(n,"east",nav.frame().east());writeVector(n,"up",nav.frame().up());writeVector(n,"south",nav.frame().south());e.put("navigation",n);}list.add(e);
        });tag.put("objects",list);return tag;
    }
}
