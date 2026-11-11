package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.network.SpaceTransferPayload;
import io.github.astelev.around_planet.space.SpaceCells;
import io.github.astelev.around_planet.compat.*;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import org.joml.Vector3d;

public final class SpaceShipTransferClient {
    public static void accept(SpaceTransferPayload packet) {
        var mc=Minecraft.getInstance();var level=mc.level;if(level==null || !level.dimension().location().equals(packet.dimension()))return;
        SpaceCells.view(level,packet.cells());if(packet.moves().isEmpty())return;
        var container=SubLevelContainer.getContainer(level);if(container==null)return;
        for(var m:packet.moves())if(container.getSubLevel(m.ship()) instanceof ClientSubLevel ship) {
            var old=new Vector3d(m.x(),m.y(),m.z());var delta=new Vector3d(m.dx(),m.dy(),m.dz());
            ((ShipInterpolation)ship.getInterpolator()).around_planet$spaceRebase(old,delta);
            if(ship.logicalPose().position().distanceSquared(old)<ship.logicalPose().position().distanceSquared(new Vector3d(old).add(delta))) {
                ship.logicalPose().position().add(delta);((ShipPoseHistory)ship).around_planet$shiftHistory(delta.x,delta.y,delta.z);
                ((ShipClientHistory)ship).around_planet$shiftRenderCache(delta.x,delta.y,delta.z);
            }ship.forceUpdateBounds();
        }
    }
    private SpaceShipTransferClient(){}
}
