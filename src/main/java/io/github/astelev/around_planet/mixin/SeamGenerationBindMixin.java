package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.generation.GenerationSeams;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkCache.class)
public abstract class SeamGenerationBindMixin {
    @Shadow @Final private ServerLevel level;
    @Inject(method="<init>",at=@At("RETURN"))
    private void around_planet$bind(CallbackInfo ci){GenerationSeams.bind(level,(ServerChunkCache)(Object)this);}
}
