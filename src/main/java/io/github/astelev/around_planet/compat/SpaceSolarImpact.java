package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.astelev.around_planet.space.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import java.util.*;

/** Terminal cleanup uses native removal for awake bodies and native holding deletion for dormant ones. */
public final class SpaceSolarImpact {
    public static void check(ServerLevel level,ServerSubLevelContainer container){
        var data=SpaceOrbitData.get(level.getServer());var cells=SpaceCells.get(level);var dormant=SpaceDormantData.get(level.getServer());var cargo=SpaceCargoData.get(level.getServer());
        for(var id:data.solarImpacts()){
            var cell=cells.all().stream().filter(c->c.id().equals(id)).findFirst().orElse(null);var binding=dormant.get(id);var ships=new HashSet<UUID>();var addresses=new HashMap<UUID,ChunkPos>();
            if(cell!=null){ships.addAll(cell.ships());for(var ship:container.getAllSubLevels())if(SpaceLocalPhysics.inCell(ship,cell))ships.add(ship.getUniqueId());}
            if(binding!=null)for(var member:binding.members())ships.add(member.ship());
            for(var record:cargo.all())if(record.cell().equals(id)){ships.add(record.ship());addresses.put(record.ship(),new ChunkPos(record.chunk()));}
            for(var group:ShipAssemblies.groups(level,container.getAllSubLevels()))if(group.ships().stream().anyMatch(ship->ships.contains(ship.getUniqueId())))for(var ship:group.ships())ships.add(ship.getUniqueId());
            var victims=new ArrayList<Entity>();for(var entity:level.getAllEntities()){var ship=SablePassengers.ship(entity);if(ship==null)ship=Sable.HELPER.getContaining(entity);if(cell!=null&&cell.contains(entity.getX(),entity.getY(),entity.getZ())||ship!=null&&ships.contains(ship.getUniqueId()))victims.add(entity);}
            for(var entity:victims){entity.stopRiding();if(entity instanceof ServerPlayer player){if(!player.isDeadOrDying()){player.setHealth(0);player.die(player.damageSources().genericKill());}}else entity.discard();}
            for(var shipId:ships)if(container.getSubLevel(shipId) instanceof ServerSubLevel ship)container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
            boolean removed=true;var holding=(SpaceHoldingAccess)container.getHoldingChunkMap();for(var shipId:ships)removed&=holding.around_planet$discard(shipId,addresses.get(shipId));
            if(!removed)continue;
            // Death drops belong to the destroyed volume too; never discard player entities.
            if(cell!=null){var debris=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!(entity instanceof ServerPlayer)&&cell.contains(entity.getX(),entity.getY(),entity.getZ()))debris.add(entity);for(var entity:debris)entity.discard();}
            for(var shipId:ships)cargo.remove(shipId);dormant.remove(id);MoonLandingData.get(level.getServer()).remove(id);data.remove(id);if(cell!=null)cells.remove(level,cell);SpaceDormant.forget(level,id);SpaceAtmosphere.forget(level,id);
            io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_SOLAR_IMPACT orbit={} bodies={} entities={}",id,ships.size(),victims.size());
        }
    }
    private SpaceSolarImpact(){}
}
