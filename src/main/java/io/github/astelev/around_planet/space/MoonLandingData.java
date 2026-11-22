package io.github.astelev.around_planet.space;
import net.minecraft.nbt.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
/** Captured entry state survives a save while destination chunks are still preparing. */
public final class MoonLandingData extends SavedData {
 private final Map<UUID,SpaceState> states=new HashMap<>();
 public static MoonLandingData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(MoonLandingData::new,MoonLandingData::load),"around_planet_moon_landings");}
 public boolean containsKey(UUID id){return states.containsKey(id);}public SpaceState get(UUID id){return states.get(id);}public void put(UUID id,SpaceState state){states.put(id,state);setDirty();}public void remove(UUID id){if(states.remove(id)!=null)setDirty();}public void retain(Set<UUID> live){if(states.keySet().retainAll(live))setDirty();}
 public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){var list=new ListTag();states.forEach((id,s)->{var t=new CompoundTag();t.putUUID("id",id);t.putDouble("x",s.position().x());t.putDouble("y",s.position().y());t.putDouble("z",s.position().z());t.putDouble("vx",s.velocity().x());t.putDouble("vy",s.velocity().y());t.putDouble("vz",s.velocity().z());list.add(t);});tag.put("entries",list);return tag;}
 public static MoonLandingData load(CompoundTag tag,HolderLookup.Provider provider){var data=new MoonLandingData();for(var entry:tag.getList("entries",Tag.TAG_COMPOUND)){var t=(CompoundTag)entry;data.states.put(t.getUUID("id"),new SpaceState(new SpaceVector(t.getDouble("x"),t.getDouble("y"),t.getDouble("z")),new SpaceVector(t.getDouble("vx"),t.getDouble("vy"),t.getDouble("vz"))));}return data;}
}
