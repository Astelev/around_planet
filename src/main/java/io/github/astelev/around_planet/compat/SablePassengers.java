package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;

/** Native attachment first; last-tracking fallback is limited to the current/previous hull, never its swept corridor. */
public final class SablePassengers {
    public static SubLevel ship(Entity entity) {
        if(entity.isRemoved())return null;
        // A native plot-local seat can already be visible before its world-space rider is registered.
        var storage=Sable.HELPER.getContaining(entity);if(storage!=null && !storage.isRemoved())return storage;
        for(var vehicle=entity.getVehicle();vehicle!=null;vehicle=vehicle.getVehicle()) {
            var containing=Sable.HELPER.getContaining(vehicle);
            if(containing!=null && !containing.isRemoved())return containing;
        }
        var current=Sable.HELPER.getTrackingSubLevel(entity);if(current!=null && !current.isRemoved())return current;
        for(var vehicle=entity.getVehicle();vehicle!=null;vehicle=vehicle.getVehicle()) {
            var tracked=Sable.HELPER.getTrackingSubLevel(vehicle);if(tracked!=null && !tracked.isRemoved())return tracked;
        }
        if(entity instanceof EntityMovementExtension movement && movement.sable$getLastTrackingSubLevelID()!=null) {
            var container=SubLevelContainer.getContainer(entity.level());
            var old=container==null?null:container.getSubLevel(movement.sable$getLastTrackingSubLevelID());
            if(old!=null && !old.isRemoved() && (near(old,entity,false) || near(old,entity,true)))return old;
        }
        return null;
    }
    private static boolean near(SubLevel ship,Entity entity,boolean previous) {
        var b=ship.getPlot().getBoundingBox();if(b==null)return false;
        var box=new BoundingBox3d(b.minX(),b.minY(),b.minZ(),b.maxX()+1.0,b.maxY()+1.0,b.maxZ()+1.0).transform(previous?ship.lastPose():ship.logicalPose());
        return entity.getX()>=box.minX()-1 && entity.getX()<=box.maxX()+1
                && entity.getZ()>=box.minZ()-1 && entity.getZ()<=box.maxZ()+1
                && entity.getY()>=box.minY()-1 && entity.getY()<=box.maxY()+3;
    }
    private SablePassengers(){}
}
