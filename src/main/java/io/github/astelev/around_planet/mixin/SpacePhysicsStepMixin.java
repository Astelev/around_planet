package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import io.github.astelev.around_planet.compat.SpaceLocalPhysics;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem",remap=false)
public abstract class SpacePhysicsStepMixin {
    @Inject(method="tickPipelinePhysics",at=@At("HEAD"))
    private void space$prepare(ServerSubLevelContainer c,CallbackInfo ci){io.github.astelev.around_planet.compat.SpaceDormant.prepare(c);}
    @Inject(method="tickPipelinePhysics",at=@At(value="FIELD",target="Ldev/ryanhcode/sable/sublevel/system/SubLevelPhysicsSystem;IN_PHYSICS_STEP:Z",opcode=org.objectweb.asm.Opcodes.PUTSTATIC,ordinal=0))
    private void space$frame(ServerSubLevelContainer c,CallbackInfo ci,@Local(ordinal=0) double dt){io.github.astelev.around_planet.compat.PropulsionPhysics.step((SubLevelPhysicsSystem)(Object)this,dt);SpaceLocalPhysics.before((SubLevelPhysicsSystem)(Object)this,dt);}
    @Inject(method="updateAllPoses",at=@At("RETURN"))
    private void space$hold(ServerSubLevelContainer c,CallbackInfo ci){SpaceLocalPhysics.after((SubLevelPhysicsSystem)(Object)this);}
}
