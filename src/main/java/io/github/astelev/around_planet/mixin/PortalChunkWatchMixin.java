package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.astelev.around_planet.portal.PortalChunkPolicy;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;

/** Filter subscription creation, not ChunkLoader's generic iteration/count contract or generation dependencies. */
@Mixin(value=ImmPtlChunkTracking.class,remap=false)
public abstract class PortalChunkWatchMixin {
    @WrapOperation(method="updateForPlayer",at=@At(value="INVOKE",target="Lqouteall/imm_ptl/core/chunk_loading/ChunkLoader;foreachChunkPos(Lqouteall/imm_ptl/core/chunk_loading/ChunkLoader$ChunkPosConsumer;)V"))
    private static void around_planet$sourceSubscriptions(ChunkLoader loader,ChunkLoader.ChunkPosConsumer consumer,
                                                         Operation<Void> original,@Local(argsOnly=true) ServerPlayer viewer) {
        original.call(loader,(ChunkLoader.ChunkPosConsumer)(dimension,x,z,distance)->{
            var level=viewer.server.getLevel(dimension);
            if(level==null || PortalChunkPolicy.allow(level,x,z))consumer.consume(dimension,x,z,distance);
        });
    }
}
