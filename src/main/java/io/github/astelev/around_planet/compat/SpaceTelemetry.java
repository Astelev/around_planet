package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.SpaceStatusPayload;
import net.minecraft.server.level.ServerPlayer;
public final class SpaceTelemetry {
    public static SpaceStatusPayload sample(ServerPlayer p){
        var cells=SpaceCells.get(p.serverLevel());var ship=SablePassengers.ship(p);var cell=ship==null?null:cells.forShip(ship.getUniqueId());
        if(cell==null)cell=cells.all().stream().filter(c->c.contains(p.getX(),p.getY(),p.getZ())).findFirst().orElse(null);
        var data=SpaceOrbitData.get(p.getServer());if(cell==null || data.get(cell.id()).isEmpty())return new SpaceStatusPayload(false,0,0,0,0,0,0,0,false,0,0,0,0,SpaceFrame.IDENTITY,SpaceVector.ZERO);
        var nav=SpaceLocalPhysics.navigation(p.serverLevel(),cell);var state=data.sample(cell.id(),p.serverLevel().getGameTime());var r=state.position();var v=state.velocity();
        return new SpaceStatusPayload(true,r.x(),r.y(),r.z(),v.x(),v.y(),v.z(),r.length()-nav.planet().radius(),data.reentry(cell.id()).isPresent(),nav.planet().radius(),data.get(cell.id()).orElseThrow().mu(),SpaceAtmosphere.returnAltitude(),SpaceAtmosphere.model().top(),nav.frame(),new SpaceVector(cell.x(),cell.y(),cell.z()));
    }
    private SpaceTelemetry(){}
}
