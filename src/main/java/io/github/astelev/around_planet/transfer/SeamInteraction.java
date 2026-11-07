package io.github.astelev.around_planet.transfer;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.compat.ShipyardSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** Coordinate translation only. The original game mode, permissions, reach attributes and packet validation still run. */
public final class SeamInteraction {
    public static boolean image(Level level,BlockPos pos) {
        var t=PlanetBorders.get(level.getWorldBorder());
        return t!=null && t.outside(pos.getX()) && !ShipyardSpace.reserved(level,pos.getX()>>4,pos.getZ()>>4);
    }
    public static BlockPos canonical(Level level,BlockPos pos) {
        var t=PlanetBorders.get(level.getWorldBorder());
        return image(level,pos)?new BlockPos((int)t.canonicalX(pos.getX()),pos.getY(),pos.getZ()):pos;
    }
    public static boolean requestReady(ServerPlayer player,BlockPos image) {
        if(!image(player.level(),image))return true;
        var source=canonical(player.level(),image);
        var t=PlanetBorders.get(player.level().getWorldBorder());
        double shift=Math.floor((player.getX()-source.getX())/t.width()+0.5)*t.width();
        double reach=player.blockInteractionRange()+1;
        return new AABB(source).move(shift,0,0).distanceToSqr(player.getEyePosition())<reach*reach
                && player.serverLevel().getChunkSource().getChunkNow(source.getX()>>4,source.getZ()>>4)!=null;
    }
    public static boolean sourceReady(ServerPlayer player,BlockPos pos) {
        var t=PlanetBorders.get(player.level().getWorldBorder());if(t==null)return false;
        var source=canonical(player.level(),pos);double shift=Math.floor((player.getX()-source.getX())/t.width()+0.5)*t.width();
        double reach=player.blockInteractionRange()+3;
        return new AABB(source).move(shift,0,0).distanceToSqr(player.getEyePosition())<reach*reach
                && player.serverLevel().getChunkSource().getChunkNow(source.getX()>>4,source.getZ()>>4)!=null;
    }
    public static void feedback(ServerPlayer player,BlockHitResult hit) {
        var t=PlanetBorders.get(player.level().getWorldBorder());if(t==null || hit==null)return;
        if(Math.abs(player.getX()-hit.getBlockPos().getX())<t.width()/2.0)return;
        for(var pos:java.util.List.of(hit.getBlockPos(),hit.getBlockPos().relative(hit.getDirection()))) {
            var source=canonical(player.level(),pos);
            if(player.serverLevel().getChunkSource().getChunkNow(source.getX()>>4,source.getZ()>>4)!=null)
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.serverLevel(),source));
        }
    }
    public static boolean periodicReach(Player player,BlockPos source,double extra) {
        var t=PlanetBorders.get(player.level().getWorldBorder());
        if(t==null || t.outside(source.getX()) || ShipyardSpace.reserved(player.level(),source.getX()>>4,source.getZ()>>4))return false;
        double shift=Math.floor((player.getX()-source.getX())/t.width()+0.5)*t.width();if(shift==0)return false;
        if(player instanceof ServerPlayer server && server.serverLevel().getChunkSource().getChunkNow(source.getX()>>4,source.getZ()>>4)==null)return false;
        double reach=player.blockInteractionRange()+extra;
        return new AABB(source).move(shift,0,0).distanceToSqr(player.getEyePosition())<reach*reach;
    }
    public static BlockHitResult canonical(Level level,BlockHitResult hit) {
        var pos=canonical(level,hit.getBlockPos());int dx=pos.getX()-hit.getBlockPos().getX();
        return dx==0?hit:new BlockHitResult(hit.getLocation().add(dx,0,0),hit.getDirection(),pos,hit.isInside());
    }
    private SeamInteraction(){}
}
