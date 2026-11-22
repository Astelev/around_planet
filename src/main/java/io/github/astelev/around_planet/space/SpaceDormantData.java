package io.github.astelev.around_planet.space;
import net.minecraft.nbt.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
/** Bindings to native held ships, not active cells. No chunk tickets and no native serialization replacement. */
public final class SpaceDormantData extends SavedData {
    public record Member(UUID ship,SpaceVector position,SpaceVector relativeVelocity,SpaceVector angularVelocity){}
    public record Binding(UUID orbit,String name,SpaceVector anchor,double returnAltitude,List<Member> members){public Binding{Objects.requireNonNull(orbit);Objects.requireNonNull(name);Objects.requireNonNull(anchor);members=List.copyOf(members);if(members.isEmpty() || members.size()>256 || members.stream().map(Member::ship).distinct().count()!=members.size() || !Double.isFinite(returnAltitude))throw new IllegalArgumentException("Dormant binding");}}
    private final Map<UUID,Binding> bindings=new LinkedHashMap<>();
    private final Map<UUID,Binding> byShip=new HashMap<>();
    public List<Binding> all(){return List.copyOf(bindings.values());}
    public Binding get(UUID id){return bindings.get(id);}
    public Binding forShip(UUID id){return byShip.get(id);}
    public void put(Binding b){for(var old:bindings.values())if(!old.orbit.equals(b.orbit) && old.members.stream().anyMatch(m->b.members.stream().anyMatch(n->n.ship.equals(m.ship))))throw new IllegalArgumentException("Duplicate dormant ship");var previous=bindings.put(b.orbit,b);if(previous!=null)for(var m:previous.members)byShip.remove(m.ship);for(var m:b.members)byShip.put(m.ship,b);setDirty();}
    public void remove(UUID id){var b=bindings.remove(id);if(b!=null){for(var m:b.members)byShip.remove(m.ship);setDirty();}}
    public static SpaceDormantData get(MinecraftServer server){var storage=server.overworld().getDataStorage();var factory=new Factory<>(SpaceDormantData::new,SpaceDormantData::load);var data=storage.get(factory,"around_planet_space_dormant");if(data!=null)return data;var path=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/around_planet_space_dormant.dat");if(java.nio.file.Files.exists(path))throw new IllegalStateException("Unreadable dormant bindings: "+path);return storage.computeIfAbsent(factory,"around_planet_space_dormant");}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){tag.putInt("schema",1);var list=new ListTag();for(var b:bindings.values()){var t=new CompoundTag();t.putUUID("id",b.orbit);t.putString("name",b.name);write(t,"a",b.anchor);t.putDouble("returnY",b.returnAltitude);var members=new ListTag();for(var m:b.members){var n=new CompoundTag();n.putUUID("ship",m.ship);write(n,"p",m.position);write(n,"v",m.relativeVelocity);write(n,"w",m.angularVelocity);members.add(n);}t.put("members",members);list.add(t);}tag.put("bindings",list);return tag;}
    public static SpaceDormantData load(CompoundTag tag,HolderLookup.Provider lookup){if(tag.getInt("schema")!=1 || !tag.contains("bindings",Tag.TAG_LIST))throw new IllegalArgumentException("Dormant schema");var source=(ListTag)tag.get("bindings");if(!source.isEmpty() && source.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Dormant list type");var raw=tag.getList("bindings",Tag.TAG_COMPOUND);var data=new SpaceDormantData();for(var item:raw){var t=(CompoundTag)item;if(!t.hasUUID("id") || !t.contains("returnY",Tag.TAG_ANY_NUMERIC))throw new IllegalArgumentException("Dormant record");var members=new ArrayList<Member>();for(var child:t.getList("members",Tag.TAG_COMPOUND)){var n=(CompoundTag)child;if(!n.hasUUID("ship"))throw new IllegalArgumentException("Missing dormant ship");members.add(new Member(n.getUUID("ship"),read(n,"p"),read(n,"v"),read(n,"w")));}var b=new Binding(t.getUUID("id"),t.getString("name"),read(t,"a"),t.getDouble("returnY"),members);if(data.get(b.orbit)!=null)throw new IllegalArgumentException("Duplicate dormant orbit");data.put(b);}data.setDirty(false);return data;}
    private static void write(CompoundTag t,String p,SpaceVector v){t.putDouble(p+"x",v.x());t.putDouble(p+"y",v.y());t.putDouble(p+"z",v.z());}
    private static SpaceVector read(CompoundTag t,String p){for(var a:new String[]{"x","y","z"})if(!t.contains(p+a,Tag.TAG_ANY_NUMERIC))throw new IllegalArgumentException("Dormant vector");return new SpaceVector(t.getDouble(p+"x"),t.getDouble(p+"y"),t.getDouble(p+"z"));}
}
