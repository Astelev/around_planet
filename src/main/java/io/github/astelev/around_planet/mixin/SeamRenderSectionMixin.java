package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.seam.SeamGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection.class)
public abstract class SeamRenderSectionMixin {
    @Shadow public abstract BlockPos getOrigin();
    @Shadow protected abstract double getDistToPlayerSqr();
    @Inject(method="hasAllNeighbors",at=@At("HEAD"),cancellable=true)
    private void around_planet$visualNeighbors(CallbackInfoReturnable<Boolean> cir) {
        var level=Minecraft.getInstance().level; if(level==null) return;
        var topology=PlanetBorders.get(level.getWorldBorder()); var pos=getOrigin();
        if(topology==null) return;
        int x=pos.getX()>>4,z=pos.getZ()>>4,half=topology.width()/32;
        if(SeamGeometry.canonical(topology,x) && x!=-half && x!=half-1)return;
        if(io.github.astelev.around_planet.compat.ShipyardSpace.reserved(level,x,z))return;
        if(!SeamGeometry.canonical(topology,x)) {
            // Missing/evicted images must compile to empty too, otherwise their previous GPU geometry survives.
            cir.setReturnValue(true);
        } else if((x==-half || x==half-1) && getDistToPlayerSqr()>576) {
            // Vanilla requires a real neighbor outside the cut and otherwise leaves the last canonical column uncompiled.
            var cache=level.getChunkSource();var full=net.minecraft.world.level.chunk.status.ChunkStatus.FULL;
            boolean ready=(x==-half || cache.getChunk(x-1,z,full,false)!=null)
                    && (x==half-1 || cache.getChunk(x+1,z,full,false)!=null)
                    && cache.getChunk(x,z-1,full,false)!=null && cache.getChunk(x,z+1,full,false)!=null;
            cir.setReturnValue(ready);
        }
    }
}
