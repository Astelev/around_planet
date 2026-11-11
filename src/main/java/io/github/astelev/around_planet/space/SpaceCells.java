package io.github.astelev.around_planet.space;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.*;

/** Persistent same-world physical addresses. Independent of the future virtual orbital controller. */
@EventBusSubscriber(modid="around_planet")
public final class SpaceCells extends SavedData {
    public record Cell(UUID id,int slot,double x,double y,double z,double originX,double originY,double originZ,List<UUID> ships) {
        public Cell {
            Objects.requireNonNull(id);ships=List.copyOf(ships);
            if(slot<0 || slot>=81 || ships.isEmpty() || ships.size()>256 || new HashSet<>(ships).size()!=ships.size())throw new IllegalArgumentException("Cell slot/members");
            for(double n:new double[]{x,y,z,originX,originY,originZ})if(!Double.isFinite(n))throw new IllegalArgumentException("Nonfinite cell");
            if(y<1400 || y>1700 || Math.abs(x)>29_500_000 || Math.abs(z)>29_500_000)throw new IllegalArgumentException("Cell bounds");
        }
        public boolean contains(double px,double py,double pz){return Math.abs(px-x)<=256 && Math.abs(pz-z)<=256 && Math.abs(py-y)<=256;}
    }
    private final Map<UUID,Cell> cells=new LinkedHashMap<>();
    private static final Map<Level,List<Cell>> VIEWS=new WeakHashMap<>();
    private static final Map<Level,Set<UUID>> MEMBERS=new WeakHashMap<>();
    public static synchronized SpaceCells get(ServerLevel level) {
        var storage=level.getServer().overworld().getDataStorage();
        var factory=new Factory<>(SpaceCells::new,SpaceCells::load);
        var data=storage.get(factory,"around_planet_space_cells");
        if(data==null) {
            var path=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/around_planet_space_cells.dat");
            if(java.nio.file.Files.exists(path))throw new IllegalStateException("Unreadable space-cell save: "+path);
            data=storage.computeIfAbsent(factory,"around_planet_space_cells");
        }
        view(level,data.all());return data;
    }
    public static synchronized boolean contains(Level level,double x,double y,double z) {
        if(!level.dimension().equals(Level.OVERWORLD))return false;
        var view=VIEWS.get(level);return view!=null && view.stream().anyMatch(c->c.contains(x,y,z));
    }
    public static boolean contains(net.minecraft.world.entity.Entity e){
        return contains(e.level(),e.getX(),e.getY(),e.getZ()) || net.neoforged.fml.ModList.get().isLoaded("sable")
                && io.github.astelev.around_planet.compat.SpaceMembership.attached(e);
    }
    public static synchronized boolean containsShip(Level level,UUID ship){
        var ids=MEMBERS.get(level);return level.dimension().equals(Level.OVERWORLD) && ids!=null && ids.contains(ship);
    }
    public static synchronized void view(Level level,List<Cell> cells){
        if(cells.equals(VIEWS.get(level)))return;
        VIEWS.put(level,List.copyOf(cells));var ids=new HashSet<UUID>();for(var c:cells)ids.addAll(c.ships());MEMBERS.put(level,Set.copyOf(ids));
    }
    @SubscribeEvent public static synchronized void unload(LevelEvent.Unload event){VIEWS.remove(event.getLevel());MEMBERS.remove(event.getLevel());if(event.getLevel() instanceof ServerLevel level && net.neoforged.fml.ModList.get().isLoaded("sable"))io.github.astelev.around_planet.compat.SpaceShipTransfers.clear(level);}
    public List<Cell> all(){return List.copyOf(cells.values());}
    public Cell forShip(UUID id){return cells.values().stream().filter(c->c.ships.contains(id)).findFirst().orElse(null);}
    public void add(ServerLevel level,Cell cell){
        for(var old:cells.values())if(!old.id.equals(cell.id) && (old.slot==cell.slot || old.ships.stream().anyMatch(cell.ships::contains)))throw new IllegalArgumentException("Space cell already occupied");
        cells.put(cell.id,cell);changed(level);
    }
    public void remove(ServerLevel level,Cell cell){cells.remove(cell.id);changed(level);}
    private void changed(ServerLevel level){setDirty();view(level,all());io.github.astelev.around_planet.network.SpaceTransferPayload.sync(level);}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        tag.putInt("schema",1);var list=new ListTag();for(var c:cells.values()) {
            var t=new CompoundTag();t.putUUID("id",c.id);t.putInt("slot",c.slot);
            t.putDouble("x",c.x);t.putDouble("y",c.y);t.putDouble("z",c.z);t.putDouble("ox",c.originX);t.putDouble("oy",c.originY);t.putDouble("oz",c.originZ);
            var ids=new ListTag();for(var id:c.ships)ids.add(NbtUtils.createUUID(id));t.put("ships",ids);list.add(t);
        }tag.put("cells",list);return tag;
    }
    public static SpaceCells load(CompoundTag tag,HolderLookup.Provider lookup) {
        if(tag.getInt("schema")!=1 || !(tag.get("cells") instanceof ListTag raw) || !raw.isEmpty() && raw.getElementType()!=Tag.TAG_COMPOUND || raw.size()>128)throw new IllegalArgumentException("Space-cell save version");
        var data=new SpaceCells();for(var item:tag.getList("cells",Tag.TAG_COMPOUND)) {
            var t=(CompoundTag)item;var ids=new ArrayList<UUID>();for(var id:t.getList("ships",Tag.TAG_INT_ARRAY))ids.add(NbtUtils.loadUUID(id));
            if(ids.isEmpty() || ids.size()>256 || !t.hasUUID("id"))throw new IllegalArgumentException("Invalid space-cell membership");
            var c=new Cell(t.getUUID("id"),t.getInt("slot"),t.getDouble("x"),t.getDouble("y"),t.getDouble("z"),t.getDouble("ox"),t.getDouble("oy"),t.getDouble("oz"),ids);
            for(double n:new double[]{c.x,c.y,c.z,c.originX,c.originY,c.originZ})if(!Double.isFinite(n))throw new IllegalArgumentException("Nonfinite cell");
            if(data.cells.putIfAbsent(c.id,c)!=null)throw new IllegalArgumentException("Duplicate cell");
        }return data;
    }
}
