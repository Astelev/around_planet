package io.github.astelev.around_planet.maneuver;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import io.github.astelev.around_planet.space.*;
/** Loaded planners only; syncs next-node identity data, not a packet every countdown tick. */
public final class ManeuverPlannerBlockEntity extends BlockEntity {
    private int warningSeconds=30;private long nextTick=-1;private double deltaV;
    public ManeuverPlannerBlockEntity(BlockPos p,BlockState s){super(ManeuverRegistry.TYPE.get(),p,s);}
    public int warningSeconds(){return warningSeconds;}public long nextTick(){return nextTick;}public double deltaV(){return deltaV;}
    public void configure(int seconds){warningSeconds=Math.max(0,seconds);setChanged();if(level instanceof net.minecraft.server.level.ServerLevel)tick(level,worldPosition,getBlockState(),this);sync();}
    public static void tick(Level level,BlockPos pos,BlockState state,ManeuverPlannerBlockEntity be){
        long tick=-1;double dv=0;if(net.neoforged.fml.ModList.get().isLoaded("sable")){var id=io.github.astelev.around_planet.compat.ManeuverAccess.orbit((net.minecraft.server.level.ServerLevel)level,pos);if(id!=null){var data=SpaceOrbitData.get(level.getServer());var next=ManeuverGuidance.next(data.maneuvers(id),level.getGameTime());if(next.isPresent()){tick=next.get().tick();dv=next.get().magnitude();}}}
        boolean powered=ManeuverGuidance.warning(level.getGameTime(),tick,be.warningSeconds);
        if(state.getValue(ManeuverPlannerBlock.POWERED)!=powered){state=state.setValue(ManeuverPlannerBlock.POWERED,powered);level.setBlock(pos,state,3);be.setBlockState(state);level.updateNeighborsAt(pos,state.getBlock());}
        if(be.nextTick!=tick||be.deltaV!=dv){be.nextTick=tick;be.deltaV=dv;be.sync();}
    }
    private void sync(){if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.putInt("WarningSeconds",warningSeconds);tag.putLong("NextManeuverTick",nextTick);tag.putDouble("NextManeuverDv",deltaV);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);warningSeconds=tag.contains("WarningSeconds")?Math.max(0,tag.getInt("WarningSeconds")):30;nextTick=tag.contains("NextManeuverTick")?tag.getLong("NextManeuverTick"):-1;double value=tag.getDouble("NextManeuverDv");deltaV=Double.isFinite(value)&&value>=0?value:0;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
