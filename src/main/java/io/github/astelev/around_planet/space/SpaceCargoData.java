package io.github.astelev.around_planet.space;
import net.minecraft.nbt.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.ChunkPos;
import java.util.*;
/** Addresses only. Sable remains the sole owner of native body serialization. */
public final class SpaceCargoData extends SavedData {
    public record Cargo(UUID ship,UUID cell,long chunk,short storage,short index,SpaceVector velocity,SpaceVector angular){}
    private final Map<UUID,Cargo> records=new LinkedHashMap<>();
    public List<Cargo> all(){return List.copyOf(records.values());}
    public void put(Cargo record){if(!record.equals(records.put(record.ship(),record)))setDirty();}
    public void remove(UUID ship){if(records.remove(ship)!=null)setDirty();}
    public static SpaceCargoData get(MinecraftServer server){var storage=server.overworld().getDataStorage();var factory=new Factory<>(SpaceCargoData::new,SpaceCargoData::load);var data=storage.get(factory,"around_planet_space_cargo");if(data!=null)return data;var path=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/around_planet_space_cargo.dat");if(java.nio.file.Files.exists(path))throw new IllegalStateException("Unreadable space cargo addresses: "+path);return storage.computeIfAbsent(factory,"around_planet_space_cargo");}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider r){tag.putInt("schema",1);var list=new ListTag();for(var c:records.values()){var n=new CompoundTag();n.putUUID("ship",c.ship());n.putUUID("cell",c.cell());n.putLong("chunk",c.chunk());n.putShort("storage",c.storage());n.putShort("index",c.index());write(n,"v",c.velocity());write(n,"w",c.angular());list.add(n);}tag.put("cargo",list);return tag;}
    public static SpaceCargoData load(CompoundTag tag,HolderLookup.Provider r){if(tag.getInt("schema")!=1)throw new IllegalArgumentException("Space cargo schema");var data=new SpaceCargoData();for(var raw:tag.getList("cargo",10)){var n=(CompoundTag)raw;if(!n.hasUUID("ship")||!n.hasUUID("cell"))throw new IllegalArgumentException("Space cargo ID");data.put(new Cargo(n.getUUID("ship"),n.getUUID("cell"),n.getLong("chunk"),n.getShort("storage"),n.getShort("index"),read(n,"v"),read(n,"w")));}data.setDirty(false);return data;}
    private static void write(CompoundTag t,String p,SpaceVector v){t.putDouble(p+"x",v.x());t.putDouble(p+"y",v.y());t.putDouble(p+"z",v.z());}
    private static SpaceVector read(CompoundTag t,String p){return new SpaceVector(t.getDouble(p+"x"),t.getDouble(p+"y"),t.getDouble(p+"z"));}
}
