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
    public SpaceState sample(UUID id,long gameTick){return require(id).at(gameTick);}
    public void put(UUID id,SpaceTrajectory trajectory) {
        Objects.requireNonNull(id);Objects.requireNonNull(trajectory);
        if(!trajectory.equals(objects.put(id,trajectory)))setDirty();
    }
    public void remove(UUID id){if(objects.remove(id)!=null)setDirty();}
    public void impulse(UUID id,long gameTick,SpaceVector deltaVelocity){put(id,require(id).impulse(gameTick,deltaVelocity));}
    /** New object inherits origin velocity plus local relative velocity, once, in one inertial chart. */
    public SpaceTrajectory detach(UUID source,UUID detached,long gameTick,SpaceFrame frame,SpaceState local) {
        if(objects.containsKey(detached))throw new IllegalArgumentException("Detached object ID already exists");
        var parent=require(source);
        var trajectory=new SpaceTrajectory(parent.primary(),gameTick,frame.detach(parent.at(gameTick),local),parent.mu());
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
            if(data.objects.putIfAbsent(entry.getUUID("id"),trajectory)!=null)throw new IllegalArgumentException("Duplicate orbit object ID");
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup) {
        tag.putInt("schema",1);var list=new ListTag();
        objects.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(item->{
            var e=new CompoundTag();var trajectory=item.getValue();var p=trajectory.initial().position();var v=trajectory.initial().velocity();
            e.putUUID("id",item.getKey());e.putString("primary",trajectory.primary());e.putLong("epoch",trajectory.epochTick());e.putDouble("mu",trajectory.mu());
            e.putDouble("px",p.x());e.putDouble("py",p.y());e.putDouble("pz",p.z());e.putDouble("vx",v.x());e.putDouble("vy",v.y());e.putDouble("vz",v.z());list.add(e);
        });tag.put("objects",list);return tag;
    }
}
