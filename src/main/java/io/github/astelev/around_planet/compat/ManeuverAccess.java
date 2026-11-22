package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
public final class ManeuverAccess {
    public static SpaceCells.Cell focus(ServerPlayer p){var cells=SpaceCells.get(p.serverLevel());var ship=SablePassengers.ship(p);var cell=ship==null?null:cells.forShip(ship.getUniqueId());return cell!=null?cell:cells.all().stream().filter(c->c.contains(p.getX(),p.getY(),p.getZ())).findFirst().orElse(null);}
    public static java.util.UUID orbit(net.minecraft.server.level.ServerLevel level,BlockPos pos){var cells=SpaceCells.get(level);var ship=dev.ryanhcode.sable.Sable.HELPER.getContaining(level,pos);var cell=ship==null?null:cells.forShip(ship.getUniqueId());if(cell==null){var p=SpaceSensorNavigation.worldPoint(level,pos);cell=cells.all().stream().filter(c->c.contains(p.x,p.y,p.z)).findFirst().orElse(null);}return cell==null?null:cell.id();}
    public static boolean near(ServerPlayer p,BlockPos pos,java.util.UUID focus){
        var level=p.serverLevel();if(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(io.github.astelev.around_planet.maneuver.ManeuverRegistry.PLANNER.get()))return false;
        var cell=focus(p);if(cell==null||!cell.id().equals(focus))return false;
        var target=SpaceSensorNavigation.worldPoint(level,pos);var viewer=SpaceSensorNavigation.worldPoint(level,p.blockPosition());if(target.distanceSquared(viewer)>64||!level.mayInteract(p,BlockPos.containing(target.x,target.y,target.z)))return false;
        var ship=dev.ryanhcode.sable.Sable.HELPER.getContaining(level,pos);return ship==null?cell.contains(target.x,target.y,target.z):cell.ships().contains(ship.getUniqueId());
    }
    private ManeuverAccess(){}
}
