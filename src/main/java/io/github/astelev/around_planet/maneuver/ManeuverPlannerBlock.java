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
public final class ManeuverPlannerBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED=BlockStateProperties.POWERED;
    public ManeuverPlannerBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.UP).setValue(POWERED,false));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(ManeuverPlannerBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.FACING,POWERED);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.FACING,c.getClickedFace());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(BlockStateProperties.FACING,r.rotate(s.getValue(BlockStateProperties.FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(BlockStateProperties.FACING)));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ManeuverPlannerBlockEntity(p,s);}
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level l,BlockState s,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,ManeuverRegistry.TYPE.get(),ManeuverPlannerBlockEntity::tick);}
    @Override protected boolean isSignalSource(BlockState s){return true;}
    @Override protected int getSignal(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,Direction d){return s.getValue(POWERED)?15:0;}
    @Override protected int getDirectSignal(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,Direction d){return getSignal(s,l,p,d);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){super.onRemove(s,l,p,next,moving);if(!s.is(next.getBlock())&&s.getValue(POWERED))l.updateNeighborsAt(p,this);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!player.getMainHandItem().isEmpty()||!player.getOffhandItem().isEmpty())return InteractionResult.PASS;if(player instanceof net.minecraft.server.level.ServerPlayer server)io.github.astelev.around_planet.network.ManeuverPayload.open(server,p);return InteractionResult.sidedSuccess(l.isClientSide);}
}
