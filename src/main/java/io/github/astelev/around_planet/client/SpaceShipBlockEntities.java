package io.github.astelev.around_planet.client;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.block.entity.BlockEntity;
public final class SpaceShipBlockEntities {
    public static boolean isShipBlock(BlockEntity be){return be.getLevel()!=null && Sable.HELPER.getContaining(be.getLevel(),be.getBlockPos())!=null;}
    private SpaceShipBlockEntities(){}
}
