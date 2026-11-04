package io.github.astelev.around_planet.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Rebase vanilla validation after an authoritative wrap, without relaxing movement checks. */
@Mixin(ServerGamePacketListenerImpl.class)
public interface VehicleMovementAccessor {
    @Accessor("lastVehicle") void around_planet$vehicle(Entity entity);
    @Accessor("vehicleFirstGoodX") void around_planet$firstX(double value);
    @Accessor("vehicleFirstGoodY") void around_planet$firstY(double value);
    @Accessor("vehicleFirstGoodZ") void around_planet$firstZ(double value);
    @Accessor("vehicleLastGoodX") void around_planet$lastX(double value);
    @Accessor("vehicleLastGoodY") void around_planet$lastY(double value);
    @Accessor("vehicleLastGoodZ") void around_planet$lastZ(double value);
    @Accessor("clientVehicleIsFloating") void around_planet$floating(boolean value);
    @Accessor("aboveGroundVehicleTickCount") void around_planet$floatingTicks(int value);
}
