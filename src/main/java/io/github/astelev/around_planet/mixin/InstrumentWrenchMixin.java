package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.Mixin;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlock;
import io.github.astelev.around_planet.propulsion.PropulsionBlock;
/** Create remains optional; the plugin excludes this API interface without Create. */
@Mixin({OrbitalSensorBlock.class,PropulsionBlock.class})
public abstract class InstrumentWrenchMixin implements com.simibubi.create.content.equipment.wrench.IWrenchable {}
