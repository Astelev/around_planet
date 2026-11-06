package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The acting player still obstructs placement even while its real entity is on the other coordinate image. */
@Mixin(BlockItem.class)
public abstract class SeamPlacementCollisionMixin {
    @Inject(method="canPlace",at=@At("RETURN"),cancellable=true)
    private void around_planet$actorObstruction(BlockPlaceContext context,BlockState state,CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue())return;
        var player=context.getPlayer();var pos=context.getClickedPos();var t=PlanetBorders.get(context.getLevel().getWorldBorder());
        if(player==null || t==null || t.outside(pos.getX()))return;
        double shift=Math.floor((player.getX()-pos.getX())/t.width()+0.5)*t.width();if(shift==0)return;
        var shape=state.getCollisionShape(context.getLevel(),pos,CollisionContext.of(player)).move(pos.getX(),pos.getY(),pos.getZ());
        if(Shapes.joinIsNotEmpty(shape,Shapes.create(player.getBoundingBox().move(-shift,0,0)),BooleanOp.AND))cir.setReturnValue(false);
    }
}
