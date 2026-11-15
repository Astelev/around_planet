package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import java.util.*;
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap",remap=false)
public abstract class SpaceUnloadCaptureMixin implements io.github.astelev.around_planet.compat.SpaceHoldingAccess {
    @WrapOperation(method={"processUnload","moveToUnloaded"},at=@At(value="INVOKE",target="Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelSerializer;toData(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ljava/util/List;)Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelData;"))
    private SubLevelData space$beforeSerialize(ServerSubLevel ship,List<UUID> dependencies,Operation<SubLevelData> original){io.github.astelev.around_planet.compat.SpaceDormant.capture(ship,dependencies);io.github.astelev.around_planet.compat.SpaceCargo.capture(ship);return original.call(ship,dependencies);}
    @Shadow @Final private net.minecraft.server.level.ServerLevel level;
    @org.spongepowered.asm.mixin.injection.Inject(method={"processUnload","moveToUnloaded"},at=@At("RETURN"))
    private void space$holdingSaved(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci,@com.llamalad7.mixinextras.sugar.Local(argsOnly=true) net.minecraft.world.level.ChunkPos chunk){io.github.astelev.around_planet.compat.SpaceCargo.saved(level,chunk);}
    @Shadow @Final private it.unimi.dsi.fastutil.objects.Object2ObjectMap<UUID,dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel> allHoldingSubLevels;
    @Shadow @Final private it.unimi.dsi.fastutil.longs.Long2ObjectMap<dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk> loadedHoldingChunks;
    @Shadow @Final private dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer container;
    @Shadow public abstract void snatchAndLoad(dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer pointer,UUID id);
    @Shadow private dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk getOrLoadHoldingChunk(net.minecraft.world.level.ChunkPos p,boolean create){throw new AssertionError();}
    @Shadow private void setDirty(net.minecraft.world.level.ChunkPos p){throw new AssertionError();}
    @Shadow @Final private it.unimi.dsi.fastutil.objects.ObjectSet<dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer> queuedDeletion;
    @Override public boolean around_planet$discard(UUID id,net.minecraft.world.level.ChunkPos fallback){
        if(container.getSubLevel(id)!=null)return false;
        var known=allHoldingSubLevels.get(id);
        if(known!=null&&known.pointer()!=null)getOrLoadHoldingChunk(known.pointer().chunkPos(),false);
        else if(fallback!=null)getOrLoadHoldingChunk(fallback,false);
        for(var chunk:loadedHoldingChunks.values()){
            var held=((io.github.astelev.around_planet.compat.SpaceHeldChunkAccess)chunk).around_planet$discard(id);
            if(held==null)continue;
            allHoldingSubLevels.remove(id);setDirty(chunk.getChunkPos());
            if(held.pointer()!=null)queuedDeletion.add(held.pointer());
            return true;
        }
        // Already absent (e.g. a previous member of the same cleanup transaction).
        return !allHoldingSubLevels.containsKey(id);
    }
    @Override public boolean around_planet$queued(UUID id){for(var chunk:loadedHoldingChunks.values())for(var held:chunk.getLoadedHoldingSubLevels())if(held.data().uuid().equals(id))return true;return false;}
    @Override public boolean around_planet$wake(UUID id,net.minecraft.world.level.ChunkPos fallback){
        if(container.getSubLevel(id)!=null)return true;
        var chunk=fallback;for(var heldChunk:loadedHoldingChunks.values()){boolean found=false;for(var held:heldChunk.getLoadedHoldingSubLevels())if(held.data().uuid().equals(id)){found=true;break;}if(found){chunk=heldChunk.getChunkPos();break;}}
        if(chunk==null)return false;snatchAndLoad(new dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer(chunk,(short)0,(short)0),id);return container.getSubLevel(id)!=null;
    }
    @org.spongepowered.asm.mixin.injection.Inject(method="loadHoldingSubLevel",at=@At("HEAD"),cancellable=true)
    private void space$alreadyActive(dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel held,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
        var active=container.getSubLevel(held.data().uuid());if(active!=null&&!active.isRemoved()){allHoldingSubLevels.remove(held.data().uuid(),held);ci.cancel();}
    }
}
