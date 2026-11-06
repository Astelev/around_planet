package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.transfer.SeamCollisions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.BiFunction;

/** Covers movement, support/suffocation checks and the native dismount search on both sides. */
@Mixin(BlockCollisions.class)
public abstract class SeamBlockCollisionsMixin {
    @Shadow @Final @Mutable private CollisionGetter collisionGetter;
    @Inject(method="<init>",at=@At("RETURN"))
    private void around_planet$stitchedCollision(CollisionGetter world,Entity entity,AABB box,boolean suffocating,
            BiFunction<BlockPos.MutableBlockPos,VoxelShape,?> result,CallbackInfo ci) {
        collisionGetter=SeamCollisions.view(world,entity,box);
    }
}
