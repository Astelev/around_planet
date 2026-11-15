package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.Mixin;
/** Sable remains optional; the plugin excludes this interface when it is absent. */
@Mixin(io.github.astelev.around_planet.loader.ShipLoaderBlockEntity.class)
public abstract class ShipLoaderNativeActorMixin implements dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor {
 @Override public void sable$tick(dev.ryanhcode.sable.sublevel.ServerSubLevel ship){io.github.astelev.around_planet.compat.ShipLoaderBridge.observe((io.github.astelev.around_planet.loader.ShipLoaderBlockEntity)(Object)this);}
}
