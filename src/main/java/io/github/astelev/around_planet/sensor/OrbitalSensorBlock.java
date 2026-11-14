package io.github.astelev.around_planet.sensor;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
public final class OrbitalSensorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
    public static final MapCodec<OrbitalSensorBlock> CODEC=simpleCodec(OrbitalSensorBlock::new);
    public OrbitalSensorBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    public static io.github.astelev.around_planet.space.SensorFrame frame(BlockState state){var n=state.getValue(FACING).getNormal();return io.github.astelev.around_planet.space.SensorFrame.of(new io.github.astelev.around_planet.space.SpaceVector(n.getX(),n.getY(),n.getZ()));}
    private static final java.util.Map<Direction,net.minecraft.world.phys.shapes.VoxelShape> SHAPES=new java.util.EnumMap<>(Direction.class);
    static {for(var d:Direction.values()){var frame=io.github.astelev.around_planet.space.SensorFrame.of(new io.github.astelev.around_planet.space.SpaceVector(d.getStepX(),d.getStepY(),d.getStepZ()));var shape=net.minecraft.world.phys.shapes.Shapes.empty();for(var b:net.minecraft.world.phys.shapes.Shapes.or(Block.box(0,0,0,16,5,16),Block.box(1,4,1,15,13,15),Block.box(7,13,7,9,15,9)).toAabbs()){var a=frame.model(new io.github.astelev.around_planet.space.SpaceVector(b.minX-.5,b.minY-.5,b.minZ-.5));var z=frame.model(new io.github.astelev.around_planet.space.SpaceVector(b.maxX-.5,b.maxY-.5,b.maxZ-.5));shape=net.minecraft.world.phys.shapes.Shapes.or(shape,net.minecraft.world.phys.shapes.Shapes.box(Math.min(a.x(),z.x())+.5,Math.min(a.y(),z.y())+.5,Math.min(a.z(),z.z())+.5,Math.max(a.x(),z.x())+.5,Math.max(a.y(),z.y())+.5,Math.max(a.z(),z.z())+.5));}SHAPES.put(d,shape);}}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return SHAPES.get(s.getValue(FACING));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OrbitalSensorBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,OrbitalSensorRegistry.TYPE.get(),OrbitalSensorBlockEntity::tick);}
    @Override protected boolean isSignalSource(BlockState s){return true;}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return l.getBlockEntity(p) instanceof OrbitalSensorBlockEntity be?be.power(d.getOpposite()):0;}
    @Override protected int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return getSignal(s,l,p,d);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(l.isClientSide && l.getBlockEntity(p) instanceof OrbitalSensorBlockEntity be)io.github.astelev.around_planet.client.OrbitalSensorScreen.open(be);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())){super.onRemove(s,l,p,next,moving);l.updateNeighborsAt(p,this);}}
}
