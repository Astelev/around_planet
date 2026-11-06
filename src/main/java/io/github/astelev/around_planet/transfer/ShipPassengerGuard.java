package io.github.astelev.around_planet.transfer;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.compat.SablePassengers;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

/** A carrier owns the coordinate change of its current passengers, including a local pose handoff. */
final class ShipPassengerGuard {
    private static boolean unavailable;
    static boolean attached(Entity entity) {
        if(!ModList.get().isLoaded("sable"))return false;
        if(unavailable)return true;
        try{return SablePassengers.ship(entity)!=null;}
        catch(RuntimeException | LinkageError failure) {
            if(!unavailable)AroundPlanet.LOGGER.warn("Sable passenger guard unavailable; ordinary entity transfers are suspended",failure);
            unavailable=true;return true;
        }
    }
    private ShipPassengerGuard(){}
}
