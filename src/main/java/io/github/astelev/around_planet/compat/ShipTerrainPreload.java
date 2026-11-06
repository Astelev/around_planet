package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;

/** Ordinary expiring canonical chunk tickets, bounded across all ships in this level. */
public final class ShipTerrainPreload {
    private static final int LIMIT=64;
    private static final TicketType<ChunkPos> TICKET=TicketType.create("around_planet_ship_seam",Comparator.comparingLong(ChunkPos::toLong),60);
    private final Map<ChunkPos,Long> requested=new HashMap<>();
    public void tick(ServerLevel level,ServerSubLevelContainer container) {
        var topology=ShipSeams.topology(level);if(topology==null)return;
        long now=level.getGameTime();requested.entrySet().removeIf(e->now-e.getValue()>=60);
        var wanted=new LinkedHashSet<ChunkPos>();
        for(var ship:container.getAllSubLevels()) {
            if(ship.isRemoved())continue;
            var bounds=ship.getPlot().getBoundingBox();if(bounds==null)continue;
            var box=new BoundingBox3d(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX()+1.0,bounds.maxY()+1.0,bounds.maxZ()+1.0).transform(ship.logicalPose());
            if(box.minY()>=level.getMaxBuildHeight() || box.maxY()<level.getMinBuildHeight())continue;
            int half=topology.width()/32,band=io.github.astelev.around_planet.PlanetConfig.SHIP_BAND.getAsInt();
            double approach=Math.min(512,Math.max(32,Math.abs(ship.latestLinearVelocity.x)*40));
            boolean east=box.maxX()>=topology.east()-approach && box.minX()<topology.east()+band*16;
            boolean west=box.minX()<=topology.west()+approach && box.maxX()>topology.west()-band*16;
            if(!east && !west)continue;
            int minZ=(int)Math.floor((box.minZ()-16)/16),maxZ=(int)Math.floor((box.maxZ()+16)/16);
            for(int depth=0;depth<band && wanted.size()<LIMIT;depth++) {
                boolean needed=east?half+depth<=(int)Math.floor((box.maxX()+approach)/16)
                        :-half-1-depth>=(int)Math.floor((box.minX()-approach)/16);
                if(!needed)break;
                int x=east?-half+depth:half-1-depth;
                for(int z=minZ;z<=maxZ && wanted.size()<LIMIT;z++)wanted.add(new ChunkPos(x,z));
            }
        }
        for(var pos:wanted) {
            var previous=requested.get(pos);if(previous!=null && now-previous<20)continue;
            if(previous==null && requested.size()>=LIMIT)continue;
            level.getChunkSource().addRegionTicket(TICKET,pos,1,pos);requested.put(pos,now);
        }
    }
}
