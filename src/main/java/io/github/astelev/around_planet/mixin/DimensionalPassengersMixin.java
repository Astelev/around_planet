package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Our adapter transfers the full cell's passenger trees exactly once. Other warps stay native. */
@Pseudo @Mixin(targets="dev.egg.dimensionalsable.SubLevelWarper",remap=false)
public abstract class DimensionalPassengersMixin {
 @Inject(method="teleportAllEntities",at=@At("HEAD"),cancellable=true,require=1)
 private static void around_planet$passengers(java.util.Set<net.minecraft.world.entity.Entity> entities,net.minecraft.server.level.ServerLevel source,net.minecraft.server.level.ServerLevel destination,org.joml.Quaterniond q,org.joml.Vector3d old,org.joml.Vector3d target,CallbackInfo ci){if(io.github.astelev.around_planet.compat.DimensionalTransfers.active())ci.cancel();}
}
