package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.*;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
/** Read-only observing sessions. The telescope never adds a vessel or loads an observed object's chunks. */
public final class TelescopeAccess {
    private record Session(BlockPos block,net.minecraft.resources.ResourceLocation dimension){}
    private static final Map<ServerPlayer,Session> sessions=new WeakHashMap<>();
    public static void close(ServerPlayer p){sessions.remove(p);}
    public static boolean active(ServerPlayer p){var s=sessions.get(p);if(s==null)return false;
        var level=p.serverLevel();if(!level.dimension().location().equals(s.dimension)||!level.hasChunkAt(s.block)||!level.getBlockState(s.block).is(io.github.astelev.around_planet.maneuver.ManeuverRegistry.TELESCOPE.get())){close(p);return false;}
        var target=SpaceSensorNavigation.worldPoint(level,s.block);var viewer=SpaceSensorNavigation.worldPoint(level,p.blockPosition());
        if(target.distanceSquared(viewer)>64){close(p);return false;}return true;
    }
    public static boolean authorize(ServerPlayer p,BlockPos block){
        if(!p.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)&&!p.level().dimension().equals(MoonSurface.dimension()))return false;
        sessions.put(p,new Session(block.immutable(),p.level().dimension().location()));if(!active(p))return false;
        var data=SpaceOrbitData.get(p.getServer());if(data.system()==null){double r=new PlanetScale(PlanetConfig.PLANET_SIZE.get()).radius();data.system(new CelestialSystem(r,Math.max(.000001,PlanetConfig.SPACE_GRAVITY.get())*r*r,0,PlanetConfig.SPACE_ENTRY.get(),PlanetConfig.YEAR_LENGTH_DAYS.get()*PlanetDaylight.periodSeconds()));}
        return true;
    }
    public static void open(ServerPlayer p,BlockPos block){if(!authorize(p,block))return;
        context(p);if(p.connection.hasChannel(SpaceMapPayload.TYPE))PacketDistributor.sendToPlayer(p,snapshot(p));
        if(p.connection.hasChannel(SpaceMapPayload.OpenTelescope.TYPE))PacketDistributor.sendToPlayer(p,new SpaceMapPayload.OpenTelescope());
    }
    public static void context(ServerPlayer p){if(!active(p))return;var status=SpaceTelemetry.surface(p);var now=p.level().getGameTime();
        if(p.connection.hasChannel(SpaceStatusPayload.TYPE))PacketDistributor.sendToPlayer(p,status);
        if(p.connection.hasChannel(ManeuverPayload.TYPE))PacketDistributor.sendToPlayer(p,new ManeuverPayload(p.getUUID(),now,new SpaceTrajectory(status.primary(),now,new SpaceState(new SpaceVector(status.x(),status.y(),status.z()),SpaceVector.ZERO),status.mu()),List.of(),ManeuverPayload.NONE,0,false,"",0,status.system(),1,1));
    }
    public static SpaceMapPayload snapshot(ServerPlayer p){
        var data=SpaceOrbitData.get(p.getServer());var list=new ArrayList<SpaceMapPayload.Body>();long now=p.level().getGameTime();
        var cells=SpaceCells.get(p.getServer().overworld());var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(p.getServer().overworld());
        for(var id:new TreeSet<>(data.ids())){
            var path=data.get(id).orElseThrow();var state=data.sample(id,now);var r=state.position();var v=state.velocity();
            var cell=cells.all().stream().filter(c->c.id().equals(id)).findFirst().orElse(null);var dormant=SpaceDormantData.get(p.getServer()).get(id);
            boolean loaded=cell!=null&&container!=null&&cell.ships().stream().allMatch(ship->container.getSubLevel(ship)!=null);
            String name=dormant==null?"Object "+id.toString().substring(0,8):dormant.name();
            if(loaded&&!cell.ships().isEmpty()){var ship=container.getSubLevel(cell.ships().getFirst());if(ship.getName()!=null&&!ship.getName().isBlank())name=ship.getName();}
            if(name.length()>80)name=name.substring(0,80);
            list.add(new SpaceMapPayload.Body(id,name,r.x(),r.y(),r.z(),v.x(),v.y(),v.z(),path.mu(),0,data.reentry(id).isPresent(),loaded,path.primary()));
        }
        return new SpaceMapPayload(p.getUUID(),now,list.size(),List.copyOf(list.subList(0,Math.min(128,list.size()))));
    }
    private TelescopeAccess(){}
}
