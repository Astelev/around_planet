package io.github.astelev.around_planet.maneuver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionResult;
public final class TelescopeBlock extends Block {
    public TelescopeBlock(Properties properties){super(properties);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!net.neoforged.fml.ModList.get().isLoaded("sable"))return InteractionResult.PASS;
        if(!player.getMainHandItem().isEmpty()||!player.getOffhandItem().isEmpty())return InteractionResult.PASS;
        if(player instanceof net.minecraft.server.level.ServerPlayer server)io.github.astelev.around_planet.compat.TelescopeAccess.open(server,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
