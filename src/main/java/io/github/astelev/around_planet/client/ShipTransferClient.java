package io.github.astelev.around_planet.client;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import io.github.astelev.around_planet.compat.ShipInterpolation;
import io.github.astelev.around_planet.compat.ShipPoseHistory;
import io.github.astelev.around_planet.network.SeamShipTransferPayload;
import net.minecraft.client.Minecraft;
/** Called only when the optional Sable server adapter sends a group transfer. */
public final class ShipTransferClient {
    private static Object world;private static long serial=Long.MIN_VALUE;
    public static void accept(SeamShipTransferPayload p) {
        var level=Minecraft.getInstance().level;if(level==null || !level.dimension().location().equals(p.dimension()))return;
        if(world!=level){world=level;serial=Long.MIN_VALUE;}if(p.serial()<=serial)return;serial=p.serial();
        var container=SubLevelContainer.getContainer(level);if(container==null)return;
        for(var entry:p.ships())if(container.getSubLevel(entry.id()) instanceof ClientSubLevel ship) {
            ((ShipInterpolation)ship.getInterpolator()).around_planet$rebase(entry.oldX(),p.dx(),p.width());
            double dx=Math.abs(ship.logicalPose().position().x-entry.oldX())<p.width()/2.0?p.dx():0;
            ship.logicalPose().position().add(dx,0,0);((ShipPoseHistory)ship).around_planet$shiftHistory(dx);
            ((io.github.astelev.around_planet.compat.ShipClientHistory)ship).around_planet$shiftRenderCache(dx);ship.forceUpdateBounds();
        }
    }
    public static void clear(Object unloading){if(world==unloading){world=null;serial=Long.MIN_VALUE;}}
    private ShipTransferClient(){}
}
