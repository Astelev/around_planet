package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.portal.PortalMobDistance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Extends only vanilla despawn distance inside the managed planes, not positions or AI navigation. */
@Mixin(Mob.class)
public abstract class PortalMobDespawnMixin {
    @WrapOperation(method="checkDespawn",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getNearestPlayer(Lnet/minecraft/world/entity/Entity;D)Lnet/minecraft/world/entity/player/Player;"))
    private Player around_planet$portalViewer(Level level,Entity mob,double range,Operation<Player> original) {
        var nearest=original.call(level,mob,range);var topology=PlanetBorders.get(level.getWorldBorder());
        if(topology==null || topology.outside(mob.getX())
                || topology.distanceToSeam(mob.getX())>mob.getType().getCategory().getDespawnDistance())return nearest;
        double best=nearest==null?Double.MAX_VALUE:PortalMobDistance.forDespawn(nearest,mob,nearest.distanceToSqr(mob));
        for(var player:level.players()) {
            double distance=PortalMobDistance.forDespawn(player,mob,Double.MAX_VALUE);
            if(distance<best){best=distance;nearest=player;}
        }
        return nearest;
    }
    @WrapOperation(method="checkDespawn",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"))
    private double around_planet$despawnDistance(Entity viewer,Entity mob,Operation<Double> original) {
        return PortalMobDistance.forDespawn(viewer,mob,original.call(viewer,mob));
    }
}
