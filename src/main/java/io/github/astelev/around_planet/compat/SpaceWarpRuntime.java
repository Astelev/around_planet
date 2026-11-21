package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.*;
import io.github.astelev.around_planet.maneuver.ManeuverPlannerBlockEntity;
import net.minecraft.server.level.ServerLevel;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import java.util.*;
public final class SpaceWarpRuntime {
    public static Map<UUID,Long> warnings(ServerLevel level){var lead=new HashMap<UUID,Long>();var seen=Collections.newSetFromMap(new IdentityHashMap<ManeuverPlannerBlockEntity,Boolean>());for(var ship:SubLevelContainer.getContainer(level).getAllSubLevels())for(var chunk:ship.getPlot().getLoadedChunks())for(var block:chunk.getChunk().getBlockEntities().values())if(block instanceof ManeuverPlannerBlockEntity planner&&!planner.isRemoved()&&seen.add(planner)&&level.getBlockEntity(planner.getBlockPos())==planner){var id=ManeuverAccess.orbit(level,planner.getBlockPos());if(id!=null)lead.merge(id,planner.warningSeconds()*20L,Math::max);}return lead;}
    public static void refresh(ServerLevel level){SpaceSolarImpact.check(level,SubLevelContainer.getContainer(level));var data=SpaceOrbitData.get(level.getServer());var contacts=data.followContacts(PlanetDaylight.spin(level),level.getGameTime());for(var cell:SpaceCells.get(level).all()){var delta=contacts.get(cell.id());if(delta!=null)SpaceLocalPhysics.externalImpulse(level,cell,SpaceLocalPhysics.navigation(level,cell).frame().toLocal(delta));if(data.get(cell.id()).isPresent())SpaceReentry.entered(level,cell.id(),data.sample(cell.id(),level.getGameTime()).position());}SpaceDormant.resetSamples(level);SpaceUnloadedImpact.resetSamples(level);SpaceAtmosphere.clear(level);
        for(var p:level.players()){if(p.connection.hasChannel(SpaceStatusPayload.TYPE))net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,SpaceTelemetry.sample(p));RendezvousPayload.send(p);}SpaceMapPayload.refreshNow(level);
    }
    private SpaceWarpRuntime(){}
}
