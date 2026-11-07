package io.github.astelev.around_planet.mixin;

import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read complete visual metadata, including reverted defaults, without consuming tracker dirtiness. */
@Mixin(SynchedEntityData.class)
public interface EntityDataAccessor {
    @Accessor("itemsById") SynchedEntityData.DataItem<?>[] around_planet$items();
}
