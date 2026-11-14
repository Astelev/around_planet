package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.Mixin;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlockEntity;
import net.minecraft.network.chat.Component;
import java.util.List;
@Mixin(OrbitalSensorBlockEntity.class)
public abstract class SensorGogglesMixin implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    @Override public boolean addToGoggleTooltip(List<Component> lines,boolean sneaking){var be=(OrbitalSensorBlockEntity)(Object)this;
        lines.add(Component.translatable("block.around_planet.orbital_sensor"));lines.add(Component.translatable("around_planet.sensor.mode."+be.mode().name().toLowerCase(java.util.Locale.ROOT)));
        lines.add(Component.translatable(be.active()?"around_planet.sensor.error":"around_planet.sensor.inactive",String.format(java.util.Locale.ROOT,"%.1f",be.yaw()),String.format(java.util.Locale.ROOT,"%.1f",be.pitch())));return true;
    }
}
