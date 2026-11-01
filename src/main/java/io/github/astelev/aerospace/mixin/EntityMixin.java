package io.github.astelev.aerospace.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.aerospace.flight.FlightHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @WrapOperation(method = "setPosRaw", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk aerospace$temporaryMovementChunk(Level level, int x, int z, Operation<LevelChunk> original) {
        var temporary = FlightHooks.movementChunk((Entity) (Object) this, level, x, z);
        return temporary != null ? temporary : original.call(level, x, z);
    }
}
