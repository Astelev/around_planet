package io.github.astelev.around_planet.transfer;

import net.minecraft.world.level.BlockGetter;

/** Client-side read-only previews; implemented without exposing client classes to the server. */
public interface SeamCollisionChunks {
    BlockGetter around_planet$collisionImage(int x, int z);
}
