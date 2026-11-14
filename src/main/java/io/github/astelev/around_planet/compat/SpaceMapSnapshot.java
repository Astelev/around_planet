package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.SpaceMapPayload;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
public final class SpaceMapSnapshot {
    public static SpaceMapPayload sample(ServerPlayer p){
        var level=p.serverLevel();var cells=SpaceCells.get(level);var attached=SablePassengers.ship(p);var focus=attached==null?null:cells.forShip(attached.getUniqueId());
        if(focus==null)focus=cells.all().stream().filter(c->c.contains(p.getX(),p.getY(),p.getZ())).findFirst().orElse(null);if(focus==null)return null;
        var data=SpaceOrbitData.get(p.getServer());var nav=data.navigation(focus.id());if(nav.isEmpty())return null;var container=SubLevelContainer.getContainer(level);
        var list=new ArrayList<SpaceMapPayload.Body>();
        for(var id:new TreeSet<>(data.ids())){
            if(id.equals(focus.id()))continue;var other=data.navigation(id);var orbit=data.get(id).orElseThrow();
            if(other.isEmpty() || !other.get().planet().equals(nav.get().planet()) || !orbit.primary().equals(data.get(focus.id()).orElseThrow().primary()))continue;
            var cell=cells.all().stream().filter(c->c.id().equals(id)).findFirst().orElse(null);var dormant=SpaceDormantData.get(p.getServer()).get(id);boolean loaded=cell!=null && cell.ships().stream().allMatch(ship->container.getSubLevel(ship)!=null);
            String name=dormant==null?"Object "+id.toString().substring(0,8):dormant.name();if(loaded){var ship=container.getSubLevel(cell.ships().getFirst());if(ship.getName()!=null && !ship.getName().isBlank())name=ship.getName();}if(name.length()>80)name=name.substring(0,80);
            var state=data.sample(id,level.getGameTime());var r=state.position();var v=state.velocity();list.add(new SpaceMapPayload.Body(id,name,r.x(),r.y(),r.z(),v.x(),v.y(),v.z(),orbit.mu(),cell==null?(dormant==null?focus.originY():dormant.returnAltitude()):cell.originY(),data.reentry(id).isPresent(),loaded));
        }
        return new SpaceMapPayload(focus.id(),list.size(),List.copyOf(list.subList(0,Math.min(128,list.size()))));
    }
    private SpaceMapSnapshot(){}
}
