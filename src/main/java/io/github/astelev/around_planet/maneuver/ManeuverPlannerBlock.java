package io.github.astelev.around_planet.maneuver;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
public final class ManeuverPlannerBlock extends Block {
    public ManeuverPlannerBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.UP));}
    @Override protected MapCodec<? extends Block> codec(){return simpleCodec(ManeuverPlannerBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.FACING,c.getClickedFace());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(BlockStateProperties.FACING,r.rotate(s.getValue(BlockStateProperties.FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(BlockStateProperties.FACING)));}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!player.getMainHandItem().isEmpty()||!player.getOffhandItem().isEmpty())return InteractionResult.PASS;if(player instanceof net.minecraft.server.level.ServerPlayer server)io.github.astelev.around_planet.network.ManeuverPayload.open(server,p);return InteractionResult.sidedSuccess(l.isClientSide);}
}
