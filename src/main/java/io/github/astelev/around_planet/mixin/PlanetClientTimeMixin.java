package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ClientLevel.class)
public abstract class PlanetClientTimeMixin {
    /** ClientLevel inherits the default interface clock; resolve it at the actual render camera. */
    public float getTimeOfDay(float partial){var level=(ClientLevel)(Object)this;if(!PlanetDaylight.enabled(level) || io.github.astelev.around_planet.client.SpaceRenderView.active())return level.dimensionType().timeOfDay(level.dayTime());return (float)PlanetDaylight.time(level,Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x,partial);}
}
