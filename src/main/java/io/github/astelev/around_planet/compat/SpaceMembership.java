package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.world.entity.Entity;
/** Optional native attachment lookup also covers outboard passengers beyond a cell's central bounds. */
public final class SpaceMembership {
    public static boolean attached(Entity entity){var ship=SablePassengers.ship(entity);return ship!=null && SpaceCells.containsShip(entity.level(),ship.getUniqueId());}
    private SpaceMembership(){}
}
