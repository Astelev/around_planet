package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.Mixin;
import io.github.astelev.around_planet.maneuver.ManeuverPlannerBlockEntity;
import net.minecraft.network.chat.Component;
import java.util.List;
/** Also makes the planner a native AeroEngine helmet target, including native target pruning. */
@Mixin(ManeuverPlannerBlockEntity.class)
public abstract class PlannerGogglesMixin implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    @Override public boolean addToGoggleTooltip(List<Component> lines,boolean sneaking){var be=(ManeuverPlannerBlockEntity)(Object)this;lines.add(Component.translatable("block.around_planet.maneuver_planner"));lines.add(Component.translatable("around_planet.maneuver.warning",be.warningSeconds()));return true;}
}
