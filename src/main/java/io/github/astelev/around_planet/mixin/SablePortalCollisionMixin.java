package io.github.astelev.around_planet.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/** Late marker: the plugin repairs Sable's already-renamed collision callback. */
@Mixin(value=Entity.class, priority=500)
public abstract class SablePortalCollisionMixin {}
