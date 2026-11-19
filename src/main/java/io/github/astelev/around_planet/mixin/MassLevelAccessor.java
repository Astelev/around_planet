package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Pseudo @Mixin(targets="dev.ryanhcode.sable.util.LevelAccelerator",remap=false)
public interface MassLevelAccessor {
    @Accessor("level") net.minecraft.world.level.Level around_planet$massLevel();
}
