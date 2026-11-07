package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.transfer.SeamCollisionChunks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientLevel.class)
public abstract class SeamClientCollisionMixin implements SeamCollisionChunks {
    @Override public BlockGetter around_planet$collisionImage(int x,int z) {
        return SeamImages.get((ClientLevel)(Object)this,x,z);
    }
}
