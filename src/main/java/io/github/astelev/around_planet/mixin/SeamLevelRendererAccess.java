package io.github.astelev.around_planet.mixin;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LevelRenderer.class)
public interface SeamLevelRendererAccess {
    @Accessor("viewArea") ViewArea around_planet$viewArea();
}
