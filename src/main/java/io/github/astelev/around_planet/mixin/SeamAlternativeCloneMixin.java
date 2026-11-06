package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.seam.SeamGeometry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets={"net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSectionCache",
        "org.embeddedt.embeddium.impl.world.cloned.ClonedChunkSectionCache"},remap=false)
public abstract class SeamAlternativeCloneMixin {
    @WrapOperation(method="clone",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk around_planet$visualClone(Level level,int x,int z,Operation<LevelChunk> original) {
        return level instanceof ClientLevel client && SeamGeometry.clipped(level,x,z)
                ? SeamImages.forRender(client,x,z):original.call(level,x,z);
    }
}
