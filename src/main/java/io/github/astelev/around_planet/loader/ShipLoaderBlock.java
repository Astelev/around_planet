package io.github.astelev.around_planet.loader;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
/** Native ship loading, no stationary-world chunk-loading effect. */
public final class ShipLoaderBlock extends BaseEntityBlock {
 public static final net.minecraft.world.level.block.state.properties.BooleanProperty ENABLED=net.minecraft.world.level.block.state.properties.BlockStateProperties.ENABLED;
 public ShipLoaderBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(ENABLED,true));}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(ShipLoaderBlock::new);}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ENABLED);}
 @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ShipLoaderBlockEntity(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,ShipLoaderRegistry.TYPE.get(),ShipLoaderBlockEntity::tick);}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!player.getMainHandItem().isEmpty()||!player.getOffhandItem().isEmpty())return InteractionResult.PASS;if(!l.isClientSide){boolean on=!s.getValue(ENABLED);l.setBlock(p,s.setValue(ENABLED,on),3);if(l.getBlockEntity(p) instanceof ShipLoaderBlockEntity be)be.changed();player.displayClientMessage(net.minecraft.network.chat.Component.translatable(net.neoforged.fml.ModList.get().isLoaded("sable")?on?"around_planet.loader.on":"around_planet.loader.off":"around_planet.loader.requires_sable"),true);}return InteractionResult.sidedSuccess(l.isClientSide);}
 @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);lines.add(net.minecraft.network.chat.Component.translatable("around_planet.loader.tooltip"));}
}
