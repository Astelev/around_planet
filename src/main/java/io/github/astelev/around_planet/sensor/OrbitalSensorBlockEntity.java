package io.github.astelev.around_planet.sensor;
import io.github.astelev.around_planet.space.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
public final class OrbitalSensorBlockEntity extends BlockEntity {
    private OrbitalAttitude.Mode mode=OrbitalAttitude.Mode.PROGRADE;
    private boolean sentActive;private double sentYaw,sentPitch;
    private int scale=45;private final int[] outputs=new int[4];private boolean active;private double yaw,pitch;
    public OrbitalSensorBlockEntity(BlockPos p,BlockState s){super(OrbitalSensorRegistry.TYPE.get(),p,s);}
    public OrbitalAttitude.Mode mode(){return mode;}public int scale(){return scale;}public boolean active(){return active;}public double yaw(){return yaw;}public double pitch(){return pitch;}
    public void configure(OrbitalAttitude.Mode mode,int scale){this.mode=mode;this.scale=Math.clamp(scale,5,180);setChanged();if(level instanceof net.minecraft.server.level.ServerLevel)tick(level,worldPosition,getBlockState(),this);sync();}
    public int power(Direction side){var front=getBlockState().getValue(OrbitalSensorBlock.FACING);return side==front?outputs[0]:side==front.getOpposite()?outputs[1]:side==front.getClockWise()?outputs[2]:side==front.getCounterClockWise()?outputs[3]:0;}
    public static void tick(Level level,BlockPos p,BlockState s,OrbitalSensorBlockEntity be){
        var target=net.neoforged.fml.ModList.get().isLoaded("sable")?io.github.astelev.around_planet.compat.SpaceSensorNavigation.direction((net.minecraft.server.level.ServerLevel)level,p,be.mode):Optional.<SpaceVector>empty();
        be.active=target.isPresent();be.yaw=be.pitch=0;int[] powers=new int[4];
        if(target.isPresent()){var n=s.getValue(OrbitalSensorBlock.FACING).getNormal();var forward=new SpaceVector(n.getX(),0,n.getZ());var error=OrbitalAttitude.error(target.get(),forward,forward.cross(new SpaceVector(0,1,0)));be.yaw=error.yaw();be.pitch=error.pitch();powers[0]=be.pitch>0?OrbitalAttitude.power(be.pitch,be.scale):0;powers[1]=be.pitch<0?OrbitalAttitude.power(be.pitch,be.scale):0;powers[2]=be.yaw>0?OrbitalAttitude.power(be.yaw,be.scale):0;powers[3]=be.yaw<0?OrbitalAttitude.power(be.yaw,be.scale):0;}
        if(!Arrays.equals(powers,be.outputs)){System.arraycopy(powers,0,be.outputs,0,4);level.updateNeighborsAt(p,s.getBlock());for(var d:Direction.Plane.HORIZONTAL)level.updateNeighborsAt(p.relative(d),s.getBlock());}
        if(level.getGameTime()%10==0 && (be.active!=be.sentActive || Math.abs(be.yaw-be.sentYaw)>.25 || Math.abs(be.pitch-be.sentPitch)>.25))be.sync();
    }
    private void sync(){if(level!=null && !level.isClientSide){sentActive=active;sentYaw=yaw;sentPitch=pitch;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}}
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider r){super.saveAdditional(n,r);n.putString("Mode",mode.name());n.putInt("Scale",scale);n.putBoolean("Active",active);n.putDouble("Yaw",yaw);n.putDouble("Pitch",pitch);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider r){super.loadAdditional(n,r);try{mode=OrbitalAttitude.Mode.valueOf(n.getString("Mode"));}catch(IllegalArgumentException e){mode=OrbitalAttitude.Mode.PROGRADE;}scale=n.contains("Scale")?Math.clamp(n.getInt("Scale"),5,180):45;active=n.getBoolean("Active");yaw=n.getDouble("Yaw");pitch=n.getDouble("Pitch");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
