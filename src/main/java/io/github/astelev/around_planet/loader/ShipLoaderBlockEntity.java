package io.github.astelev.around_planet.loader;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/** Identity is stored as a string so dimensional coordinate transforms cannot reinterpret it. */
public final class ShipLoaderBlockEntity extends BlockEntity {
 private String loaderId=java.util.UUID.randomUUID().toString();
 public ShipLoaderBlockEntity(BlockPos p,BlockState s){super(ShipLoaderRegistry.TYPE.get(),p,s);}
 public String loaderId(){return loaderId;}
 public boolean enabled(){return !isRemoved()&&getBlockState().getValue(ShipLoaderBlock.ENABLED);}
 private boolean nativeServer(){return level!=null&&!level.isClientSide&&net.neoforged.fml.ModList.get().isLoaded("sable");}
 public void changed(){setChanged();if(nativeServer())io.github.astelev.around_planet.compat.ShipLoaderBridge.refresh(this);}
 public static void tick(Level l,BlockPos p,BlockState s,ShipLoaderBlockEntity be){if(l.getGameTime()%20==0&&be.nativeServer())io.github.astelev.around_planet.compat.ShipLoaderBridge.observe(be);}
 @Override public void onLoad(){super.onLoad();if(nativeServer())io.github.astelev.around_planet.compat.ShipLoaderBridge.observe(this);}
 @Override public void setRemoved(){super.setRemoved();if(nativeServer())io.github.astelev.around_planet.compat.ShipLoaderBridge.removed(this);}
 @Override public void onChunkUnloaded(){if(nativeServer())io.github.astelev.around_planet.compat.ShipLoaderBridge.forget(this);super.onChunkUnloaded();}
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){super.saveAdditional(tag,provider);tag.putString("LoaderId",loaderId);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){super.loadAdditional(tag,provider);if(tag.contains("LoaderId",net.minecraft.nbt.Tag.TAG_STRING)){try{loaderId=java.util.UUID.fromString(tag.getString("LoaderId")).toString();}catch(IllegalArgumentException ignored){loaderId=java.util.UUID.randomUUID().toString();}}}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider){return saveWithoutMetadata(provider);}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
