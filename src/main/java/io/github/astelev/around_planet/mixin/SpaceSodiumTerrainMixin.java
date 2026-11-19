package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.astelev.around_planet.client.SpaceRenderView;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer",remap=false)
public abstract class SpaceSodiumTerrainMixin {
    @WrapOperation(method="drawChunkLayer",at=@At(value="INVOKE",target="Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;renderLayer(Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;DDD)V"))
    private void space$onlyShipLayers(@Coerce Object manager,@Coerce Object matrices,@Coerce Object pass,double x,double y,double z,Operation<Void> original){if(SpaceRenderView.active())SpaceRenderView.terrainSkips++;else original.call(manager,matrices,pass,x,y,z);}
}
