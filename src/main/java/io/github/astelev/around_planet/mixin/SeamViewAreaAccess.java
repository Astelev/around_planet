package io.github.astelev.around_planet.mixin;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ViewArea.class)
public interface SeamViewAreaAccess {
    @Invoker("getRenderSectionAt") SectionRenderDispatcher.RenderSection around_planet$section(BlockPos pos);
}
