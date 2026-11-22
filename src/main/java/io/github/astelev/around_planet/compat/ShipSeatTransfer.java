package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.Sable;
import io.github.astelev.around_planet.mixin.ChunkTrackingAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** A native seat remains in its plot; only world-space passengers follow the rebased pose. */
public final class ShipSeatTransfer {
    private record Rider(Entity entity, Vec3 position, Vec3 velocity) {}

    static Runnable prepare(Entity root,double dx) {return prepare(root,dx,0,0);}
    public static Runnable prepare(Entity root,double dx,double dy,double dz) {
        List<Rider> riders=root.getSelfAndPassengers()
                .filter(e->Sable.HELPER.getContaining(e)==null)
                .map(e->new Rider(e,e.position(),e.getDeltaMovement())).toList();
        return ()->{
            for(var rider:riders) {
                var entity=rider.entity();var target=rider.position().add(dx,dy,dz);
                // ServerPlayer.teleportTo invokes Sable's mounted-player conversion and sends
                // plot coordinates to its matching client hook. A vanilla vehicle packet would
                // instead put the player/seat into the reserved plot in world space.
                if(entity instanceof ServerPlayer player)
                    player.teleportTo(target.x,target.y,target.z);
                else entity.setPos(target);
                entity.setDeltaMovement(rider.velocity());
            }
            for(var rider:riders)if(rider.entity() instanceof ServerPlayer player) {
                ((ChunkTrackingAccessor)player.serverLevel().getChunkSource().chunkMap)
                        .around_planet$updateChunkTracking(player);
                player.serverLevel().getChunkSource().move(player);
            }
        };
    }
    /** Reentry rotates world riders; native plot-local seat entities stay exactly where they are. */
    public static Runnable prepareRigid(Entity root,org.joml.Vector3dc old,org.joml.Vector3dc target,org.joml.Quaterniondc q,org.joml.Vector3dc boost,boolean attached) {
        record Snapshot(Entity e,org.joml.Vector3d p,org.joml.Vector3d v,org.joml.Vector3d look){}
        var riders=root.getSelfAndPassengers().filter(e->Sable.HELPER.getContaining(e)==null).map(e->new Snapshot(e,
            new org.joml.Vector3d(e.getX(),e.getY(),e.getZ()),new org.joml.Vector3d(e.getDeltaMovement().x,e.getDeltaMovement().y,e.getDeltaMovement().z),new org.joml.Vector3d(e.getLookAngle().x,e.getLookAngle().y,e.getLookAngle().z))).toList();
        return ()->{
            for(var r:riders){var p=SpaceRotation.point(r.p,old,target,q);var look=q.transform(r.look);var v=q.transform(r.v);if(!attached)v.fma(1.0/20,boost);
                if(r.e instanceof ServerPlayer player)player.teleportTo(p.x,p.y,p.z);else r.e.setPos(p.x,p.y,p.z);
                r.e.setYRot((float)Math.toDegrees(Math.atan2(-look.x,look.z)));r.e.setXRot((float)Math.toDegrees(Math.atan2(-look.y,Math.hypot(look.x,look.z))));
                r.e.setDeltaMovement(v.x,v.y,v.z);r.e.fallDistance=0;
            }
            for(var r:riders)if(r.e instanceof ServerPlayer player){
                ((ChunkTrackingAccessor)player.serverLevel().getChunkSource().chunkMap).around_planet$updateChunkTracking(player);player.serverLevel().getChunkSource().move(player);
                player.connection.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
                if(Sable.HELPER.getContaining(root)==null && root!=player)player.connection.send(new net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket(root));
            }
        };
    }
    private ShipSeatTransfer() {}
}
