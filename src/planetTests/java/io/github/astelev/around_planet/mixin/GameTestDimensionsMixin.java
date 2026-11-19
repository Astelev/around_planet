package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.dimension.LevelStem;
/** GameTestServer discards datapack dimensions; retain them as a normal server does. Tests only. */
@Mixin(net.minecraft.gametest.framework.GameTestServer.class)
public abstract class GameTestDimensionsMixin {
 @Redirect(method="lambda$create$1",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
 private static WorldDimensions.Complete around_planet$dimensions(WorldDimensions dims,Registry<LevelStem> ignored,net.minecraft.world.level.LevelSettings settings,WorldLoader.DataLoadContext context){return dims.bake(context.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM));}
}
