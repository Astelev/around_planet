package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Serialized item components are payload, not world-coordinate references. */
@Pseudo @Mixin(targets="dev.egg.dimensionalsable.utils.BlockEntityTransformer",remap=false)
public abstract class DimensionalItemDataMixin {
 @Inject(method="transformBlockEntityRecursive",at=@At("HEAD"),cancellable=true,require=1)
 private void around_planet$keepItems(Object value,CallbackInfo ci){if(io.github.astelev.around_planet.compat.DimensionalTransfers.active()&&value instanceof net.minecraft.nbt.CompoundTag tag&&tag.contains("id",net.minecraft.nbt.Tag.TAG_STRING)&&(tag.contains("count",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)||tag.contains("Count",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)))ci.cancel();}
}
