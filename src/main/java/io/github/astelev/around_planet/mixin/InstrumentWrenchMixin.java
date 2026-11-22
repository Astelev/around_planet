package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.Mixin;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlock;
import io.github.astelev.around_planet.propulsion.PropulsionBlock;
/** Create remains optional; the plugin excludes this API interface without Create. */
@Mixin({OrbitalSensorBlock.class,PropulsionBlock.class,io.github.astelev.around_planet.solid.SolidColumnBlock.class,io.github.astelev.around_planet.maneuver.ManeuverPlannerBlock.class,io.github.astelev.around_planet.loader.ShipLoaderBlock.class})
public abstract class InstrumentWrenchMixin implements com.simibubi.create.content.equipment.wrench.IWrenchable {
    @Override public net.minecraft.world.level.block.state.BlockState getRotatedBlockState(net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.Direction face){if(state.getBlock() instanceof OrbitalSensorBlock)return OrbitalSensorBlock.wrenched(state,face);return com.simibubi.create.content.equipment.wrench.IWrenchable.super.getRotatedBlockState(state,face);}
}
