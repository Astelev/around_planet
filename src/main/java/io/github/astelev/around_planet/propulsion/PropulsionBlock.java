package io.github.astelev.around_planet.propulsion;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlock;
public final class PropulsionBlock extends BaseEntityBlock {
    public final PropulsionRegistry.Kind kind;
    public PropulsionBlock(Properties p,PropulsionRegistry.Kind kind){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(OrbitalSensorBlock.FACING,Direction.UP));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new PropulsionBlock(p,kind));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(OrbitalSensorBlock.FACING,c.getClickedFace());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(OrbitalSensorBlock.FACING,r.rotate(s.getValue(OrbitalSensorBlock.FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(OrbitalSensorBlock.FACING)));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PropulsionBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,PropulsionRegistry.TYPE.get(),PropulsionBlockEntity::tick);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){if(kind!=PropulsionRegistry.Kind.RSU)return net.minecraft.world.phys.shapes.Shapes.block();var f=OrbitalSensorBlock.frame(s);var a=f.model(new io.github.astelev.around_planet.space.SpaceVector(-.25,-.5,-.25));var b=f.model(new io.github.astelev.around_planet.space.SpaceVector(.25,-.25,.25));return net.minecraft.world.phys.shapes.Shapes.box(Math.min(a.x(),b.x())+.5,Math.min(a.y(),b.y())+.5,Math.min(a.z(),b.z())+.5,Math.max(a.x(),b.x())+.5,Math.max(a.y(),b.y())+.5,Math.max(a.z(),b.z())+.5);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(l.isClientSide&&l.getBlockEntity(p) instanceof PropulsionBlockEntity be)io.github.astelev.around_planet.client.PropulsionScreen.open(be);return InteractionResult.sidedSuccess(l.isClientSide);}
}
