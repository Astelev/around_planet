package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.portal.PlanetPortals;
import io.github.astelev.around_planet.portal.PortalLoadBudget;
import net.minecraft.server.level.ServerPlayer;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.ChunkVisibility;
import qouteall.imm_ptl.core.portal.Portal;

final class PortalLoadingRegression {
    static void verify(ServerPlayer player) throws ReflectiveOperationException {
        var east=PlanetPortals.managed(player.serverLevel()).stream().filter(p->PlanetPortals.EAST.equals(p.portalTag)).findFirst().orElseThrow();
        int approach=PlanetConfig.CHUNK_PRELOAD_DISTANCE.getAsInt();
        var far=east.getOriginPos().add(east.getNormal().scale(approach+16));
        var near=east.getOriginPos().add(east.getNormal().scale(approach-16));
        require(ChunkVisibility.getNearbyPortals(player.serverLevel(),far,p->true,1,256).stream().noneMatch(p->p==east),"Managed portal starts loading outside approach band");
        require(ChunkVisibility.getNearbyPortals(player.serverLevel(),near,p->true,1,256).contains(east),"Managed portal missing inside approach band");
        var method=ChunkVisibility.class.getDeclaredMethod("getGeneralDirectPortalLoader",ServerPlayer.class,Portal.class);
        method.setAccessible(true);
        var actual=(ChunkLoader)method.invoke(null,player,east);
        int view=McHelper.getPlayerLoadDistance(player);
        double distance=east.getDistanceToNearestPointInPortal(player.position());
        int expected=PortalLoadBudget.radius(view,PlanetConfig.CHUNK_RADIUS.getAsInt(),approach,distance);
        require(actual.radius()==expected,"Managed loader radius was not adjusted");
        var destination=east.transformPoint(player.position());
        require(actual.x()==PortalLoadBudget.destinationChunkX(east.getDestPos().x,true)
                && actual.z()==((int)Math.floor(destination.z)>>4),"Loader was not anchored inside the destination seam");
        var foreign=Portal.ENTITY_TYPE.create(player.serverLevel());
        foreign.isGlobalPortal=true;foreign.portalTag="test:foreign";
        foreign.setOriginPos(east.getOriginPos());foreign.setDestinationDimension(east.getDestDim());foreign.setDestination(east.getDestPos());
        foreign.setOrientationAndSize(east.getAxisW(),east.getAxisH(),east.getWidth(),east.getHeight());
        var untouched=(ChunkLoader)method.invoke(null,player,foreign);
        int nativeRadius=Math.min(qouteall.imm_ptl.core.IPGlobal.indirectLoadingRadiusCap*2,Math.max(2,view-Math.floorDiv((int)distance,16)));
        require(untouched.radius()==nativeRadius,"Foreign portal radius was changed");
        AroundPlanet.LOGGER.info("PORTAL_LOADING_REGRESSION PASS: managed radius={} view={} distance={} edgeChunk={}, foreign loader preserved",actual.radius(),view,distance,actual.x());
    }
    private static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
    private PortalLoadingRegression() {}
}
