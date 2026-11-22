package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import java.util.UUID;
@Pseudo @Mixin(targets="dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk",remap=false)
public abstract class SpaceHeldDiscardMixin implements io.github.astelev.around_planet.compat.SpaceHeldChunkAccess {
    @Shadow @Final private it.unimi.dsi.fastutil.objects.Object2ObjectMap<UUID,HoldingSubLevel> loadedHoldingSubLevels;
    @Shadow @Final private it.unimi.dsi.fastutil.objects.ObjectList<HoldingSubLevel> alsoLoad;
    @Shadow public abstract java.util.List<dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer> getSubLevelPointers();
    @Override public HoldingSubLevel around_planet$discard(UUID id){
        var held=loadedHoldingSubLevels.remove(id);alsoLoad.removeIf(h->h.data().uuid().equals(id));
        if(held!=null&&held.pointer()!=null)getSubLevelPointers().remove(held.pointer().local());
        return held;
    }
}
