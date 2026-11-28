package io.github.astelev.around_planet.manufacturing;

import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class ManufacturingBlock extends BaseEntityBlock {
  public final MachineKind kind;

  public ManufacturingBlock(Properties p, MachineKind kind) {
    super(p);
    this.kind = kind;
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.LIT, false));
  }

  @Override
  protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
    return simpleCodec(p -> new ManufacturingBlock(p, kind));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.LIT);
  }

  @Override
  public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c) {
    return defaultBlockState()
        .setValue(BlockStateProperties.HORIZONTAL_FACING, c.getHorizontalDirection().getOpposite());
  }

  @Override
  protected BlockState rotate(BlockState s, Rotation r) {
    return s.setValue(
        BlockStateProperties.HORIZONTAL_FACING,
        r.rotate(s.getValue(BlockStateProperties.HORIZONTAL_FACING)));
  }

  @Override
  protected BlockState mirror(BlockState s, Mirror m) {
    return s.rotate(m.getRotation(s.getValue(BlockStateProperties.HORIZONTAL_FACING)));
  }

  @Override
  protected RenderShape getRenderShape(BlockState s) {
    return RenderShape.MODEL;
  }

  @Override
  public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
      BlockPos p, BlockState s) {
    return new ManufacturingBlockEntity(p, s);
  }

  @Override
  public <T extends net.minecraft.world.level.block.entity.BlockEntity>
      net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
          net.minecraft.world.level.Level l,
          BlockState s,
          net.minecraft.world.level.block.entity.BlockEntityType<T> t) {
    return l.isClientSide
        ? null
        : createTickerHelper(t, ManufacturingRegistry.TYPE.get(), ManufacturingBlockEntity::tick);
  }

  @Override
  protected net.minecraft.world.ItemInteractionResult useItemOn(
      net.minecraft.world.item.ItemStack stack,
      BlockState state,
      net.minecraft.world.level.Level level,
      BlockPos pos,
      net.minecraft.world.entity.player.Player player,
      net.minecraft.world.InteractionHand hand,
      net.minecraft.world.phys.BlockHitResult hit) {
    if (stack.getItem() instanceof PartItem
        || net.neoforged.fml.ModList.get().isLoaded("create")
            && io.github.astelev.around_planet.compat.ManufacturingCreateFilter.isFilter(stack)) {
      if (!player.isSpectator()
          && player.mayBuild()
          && level.getBlockEntity(pos) instanceof ManufacturingBlockEntity be) {
        if (!level.isClientSide) {
          be.setFilter(stack);
          player.displayClientMessage(
              net.minecraft.network.chat.Component.translatable(
                  "around_planet.manufacturing.filter_set", stack.getHoverName()),
              true);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
      }
    }
    return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }

  @Override
  protected net.minecraft.world.InteractionResult useWithoutItem(
      BlockState s,
      net.minecraft.world.level.Level l,
      BlockPos p,
      net.minecraft.world.entity.player.Player player,
      net.minecraft.world.phys.BlockHitResult hit) {
    if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())
      return net.minecraft.world.InteractionResult.PASS;
    if (!l.isClientSide && l.getBlockEntity(p) instanceof ManufacturingBlockEntity be)
      player.openMenu(be, p);
    return net.minecraft.world.InteractionResult.sidedSuccess(l.isClientSide);
  }

  @Override
  protected void onRemove(
      BlockState s,
      net.minecraft.world.level.Level l,
      BlockPos p,
      BlockState next,
      boolean moving) {
    if (!s.is(next.getBlock())
        && l.getBlockEntity(p) instanceof ManufacturingBlockEntity be
        && !l.isClientSide)
      for (int i = 0; i < be.items.getSlots(); i++)
        net.minecraft.world.Containers.dropItemStack(
            l, p.getX(), p.getY(), p.getZ(), be.items.getStackInSlot(i));
    super.onRemove(s, l, p, next, moving);
  }
}
