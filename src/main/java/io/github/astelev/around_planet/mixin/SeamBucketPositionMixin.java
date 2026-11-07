package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.transfer.SeamInteraction;
import io.github.astelev.around_planet.transfer.SeamItemUseScope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(BucketItem.class)
public abstract class SeamBucketPositionMixin {
    @WrapOperation(method="use",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos around_planet$derivedBucketPosition(BlockPos pos,Direction direction,Operation<BlockPos> original) {
        var result=original.call(pos,direction);var player=SeamItemUseScope.actor();
        return player==null?result:SeamInteraction.canonical(player.level(),result);
    }
}
