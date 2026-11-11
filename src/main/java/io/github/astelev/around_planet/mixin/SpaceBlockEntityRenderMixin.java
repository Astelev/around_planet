package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.client.SpaceRenderView;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class SpaceBlockEntityRenderMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void space$onlyShipBlockEntities(BlockEntity be,float tick,PoseStack pose,MultiBufferSource buffer,CallbackInfo ci){
        if(SpaceRenderView.active() && !(net.neoforged.fml.ModList.get().isLoaded("sable") && io.github.astelev.around_planet.client.SpaceShipBlockEntities.isShipBlock(be)))ci.cancel();
    }
}
