package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import java.util.*;
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap",remap=false)
public abstract class SpaceUnloadCaptureMixin {
    @WrapOperation(method={"processUnload","moveToUnloaded"},at=@At(value="INVOKE",target="Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelSerializer;toData(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ljava/util/List;)Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelData;"))
    private SubLevelData space$beforeSerialize(ServerSubLevel ship,List<UUID> dependencies,Operation<SubLevelData> original){io.github.astelev.around_planet.compat.SpaceDormant.capture(ship,dependencies);io.github.astelev.around_planet.compat.SpaceCargo.capture(ship);return original.call(ship,dependencies);}
    @Shadow @Final private net.minecraft.server.level.ServerLevel level;
    @org.spongepowered.asm.mixin.injection.Inject(method={"processUnload","moveToUnloaded"},at=@At("RETURN"))
    private void space$holdingSaved(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci,@com.llamalad7.mixinextras.sugar.Local(argsOnly=true) net.minecraft.world.level.ChunkPos chunk){io.github.astelev.around_planet.compat.SpaceCargo.saved(level,chunk);}
}
