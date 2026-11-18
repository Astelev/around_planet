package io.github.astelev.around_planet.solid;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
public final class SolidColumnBlock extends BaseEntityBlock {
    public SolidColumnBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.AXIS,Direction.Axis.Y));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(SolidColumnBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.AXIS);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.AXIS,c.getClickedFace().getAxis());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(BlockStateProperties.AXIS,r.rotate(Direction.get(Direction.AxisDirection.POSITIVE,s.getValue(BlockStateProperties.AXIS))).getAxis());}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new SolidColumnBlockEntity(p,s);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return switch(s.getValue(BlockStateProperties.AXIS)){case Y->Block.box(3,0,3,13,16,13);case X->Block.box(0,3,3,16,13,13);case Z->Block.box(3,3,0,13,13,16);};}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!stack.is(SolidRegistry.FUEL.get().asItem()))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(level.getBlockEntity(pos) instanceof SolidColumnBlockEntity be){if(level.isClientSide)return ItemInteractionResult.SUCCESS;if(be.loadFuel()){if(!player.isCreative())stack.shrink(1);}else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("around_planet.solid.column_occupied"),true);}
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!player.getMainHandItem().isEmpty()||!player.getOffhandItem().isEmpty())return InteractionResult.PASS;if(!l.isClientSide&&l.getBlockEntity(p) instanceof SolidColumnBlockEntity be)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("around_planet.solid.remaining",String.format(java.util.Locale.ROOT,"%.1f",be.fraction()*100)),true);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){var drops=super.getDrops(state,params);var be=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);if(be instanceof SolidColumnBlockEntity column&&column.fuel()>0){var n=new net.minecraft.nbt.CompoundTag();n.putDouble("SolidFuelUnits",column.fuel());for(var stack:drops)if(stack.is(asItem()))BlockItem.setBlockEntityData(stack,SolidRegistry.TYPE.get(),n);}return drops;}
}
