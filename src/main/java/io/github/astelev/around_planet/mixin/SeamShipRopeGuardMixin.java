package io.github.astelev.around_planet.mixin;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.astelev.around_planet.compat.ShipAssemblies;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Rope particles do not yet have an atomic rebase API, so attached groups stay put instead of stretching a rope across the world. */
@Pseudo
@Mixin(targets={"dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject","dev.ryanhcode.sable.physics.impl.rapier.rope.RapierRopeHandle"},remap=false)
public abstract class SeamShipRopeGuardMixin {
    @Inject(method="setAttachment",at=@At("RETURN"))
    private void around_planet$attachment(RopeHandle.AttachmentPoint point,Vector3dc location,ServerSubLevel ship,CallbackInfo ci){ShipAssemblies.rope(this,point,ship);}
    @Inject(method="remove",at=@At("RETURN"))
    private void around_planet$remove(CallbackInfo ci){ShipAssemblies.removed(this);}
}
