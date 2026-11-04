package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.portal.PlanetPortals;
import io.github.astelev.around_planet.portal.PortalLoadBudget;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.ChunkVisibility;
import qouteall.imm_ptl.core.portal.Portal;
import java.util.List;
import java.util.function.Predicate;

@Mixin(value=ChunkVisibility.class,remap=false)
public abstract class SeamPortalLoadingMixin {
    @ModifyReturnValue(method="getGeneralDirectPortalLoader",at=@At("RETURN"))
    private static ChunkLoader around_planet$radius(ChunkLoader original,ServerPlayer player,Portal portal) {
        if(!PlanetPortals.owned(portal))return original;
        int radius=PortalLoadBudget.radius(McHelper.getPlayerLoadDistance(player),PlanetConfig.CHUNK_RADIUS.getAsInt(),
                PlanetConfig.CHUNK_PRELOAD_DISTANCE.getAsInt(),portal.getDistanceToNearestPointInPortal(player.position()));
        // The transformed camera is outside the canonical opposite edge while
        // approaching. Anchor tickets just inside the edge, otherwise our source
        // mask discards the entire early loading region before it reaches land.
        int x=PortalLoadBudget.destinationChunkX(portal.getDestPos().x,PlanetPortals.EAST.equals(portal.portalTag));
        return new ChunkLoader(original.dimension(),x,original.z(),radius);
    }
    @ModifyReturnValue(method="getNearbyPortals",at=@At("RETURN"))
    private static List<Portal> around_planet$approach(List<Portal> original,ServerLevel world,Vec3 pos,
                                                      Predicate<Portal> predicate,int ordinaryRange,int globalRange) {
        var filtered=new java.util.ArrayList<>(original);
        filtered.removeIf(p->PlanetPortals.owned(p)
                && p.getDistanceToNearestPointInPortal(pos)>PlanetConfig.CHUNK_PRELOAD_DISTANCE.getAsInt());
        return filtered;
    }
}
