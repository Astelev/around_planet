package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.seam.SeamGeometry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets={"net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager",
        "org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager"},remap=false)
public abstract class SeamAlternativeSectionMixin {
    @WrapOperation(method="onSectionAdded",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientLevel;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk around_planet$visualSection(ClientLevel level,int x,int z,Operation<LevelChunk> original) {
        return SeamGeometry.clipped(level,x,z)?SeamImages.forRender(level,x,z):original.call(level,x,z);
    }
}
