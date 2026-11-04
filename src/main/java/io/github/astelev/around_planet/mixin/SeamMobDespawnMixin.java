package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.seam.SeamEntityDistance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve ordinary peaceful/event/persistence rules while recognizing a viewer across the seam. */
@Mixin(Mob.class)
public abstract class SeamMobDespawnMixin {
    @WrapOperation(method="checkDespawn",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getNearestPlayer(Lnet/minecraft/world/entity/Entity;D)Lnet/minecraft/world/entity/player/Player;"))
    private Player around_planet$nearestViewer(Level level,Entity mob,double range,Operation<Player> original) {
        var nearest=original.call(level,mob,range);
        var topology=io.github.astelev.around_planet.border.PlanetBorders.get(level.getWorldBorder());
        if(topology==null || topology.outside(mob.getX())
                || topology.distanceToSeam(mob.getX())>(io.github.astelev.around_planet.PlanetConfig.RADIUS.getAsInt()+2)*16)return nearest;
        double best=nearest==null?Double.MAX_VALUE:SeamEntityDistance.forDespawn(nearest,mob,nearest.distanceToSqr(mob));
        for(var player:level.players()) {
            double distance=SeamEntityDistance.forDespawn(player,mob,Double.MAX_VALUE);
            if(distance<best){best=distance;nearest=player;}
        }
        return nearest;
    }
    @WrapOperation(method="checkDespawn",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"))
    private double around_planet$despawnDistance(Entity viewer,Entity mob,Operation<Double> original) {
        double distance=original.call(viewer,mob);
        return viewer instanceof Player player?SeamEntityDistance.forDespawn(player,mob,distance):distance;
    }
}
