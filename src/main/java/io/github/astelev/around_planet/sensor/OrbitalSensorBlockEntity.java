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
    private boolean legacyMount;private boolean sentActive;private double sentYaw,sentPitch;
    private SpaceVector visualTarget=new SpaceVector(0,0,-1),visualUp=new SpaceVector(0,1,0),sentTarget=SpaceVector.ZERO,sentUp=SpaceVector.ZERO;
    public SpaceVector visualTarget(){return visualTarget;}public SpaceVector visualUp(){return visualUp;}
    private int scale=45;private final int[] outputs=new int[6];private boolean active;private double yaw,pitch;
    public OrbitalSensorBlockEntity(BlockPos p,BlockState s){super(OrbitalSensorRegistry.TYPE.get(),p,s);}
    public OrbitalAttitude.Mode mode(){return mode;}public int scale(){return scale;}public boolean active(){return active;}public double yaw(){return yaw;}public double pitch(){return pitch;}
    public void configure(OrbitalAttitude.Mode mode,int scale){this.mode=mode;this.scale=Math.clamp(scale,5,180);setChanged();if(level instanceof net.minecraft.server.level.ServerLevel)tick(level,worldPosition,getBlockState(),this);sync();}
    public int power(Direction side){return outputs[side.ordinal()];}
    public static void tick(Level level,BlockPos p,BlockState s,OrbitalSensorBlockEntity be){
        if(be.legacyMount){be.legacyMount=false;s=s.setValue(OrbitalSensorBlock.FACING,Direction.UP);level.setBlockAndUpdate(p,s);be.setBlockState(s);be.setChanged();be.sync();}
        var target=net.neoforged.fml.ModList.get().isLoaded("sable")?io.github.astelev.around_planet.compat.SpaceSensorNavigation.sample((net.minecraft.server.level.ServerLevel)level,p,be.mode):Optional.<io.github.astelev.around_planet.compat.SpaceSensorNavigation.Sample>empty();
        be.active=target.isPresent();be.yaw=be.pitch=0;int[] powers=new int[6];
        if(target.isPresent()){be.visualTarget=target.get().worldTarget();be.visualUp=target.get().worldUp();var frame=OrbitalSensorBlock.frame(s);var error=OrbitalAttitude.error(target.get().localTarget(),frame.forward(),frame.right());be.yaw=error.yaw();be.pitch=error.pitch();for(var d:Direction.values())powers[d.ordinal()]=frame.power(target.get().localTarget(),new SpaceVector(d.getStepX(),d.getStepY(),d.getStepZ()),be.scale);}

        if(!Arrays.equals(powers,be.outputs)){System.arraycopy(powers,0,be.outputs,0,powers.length);level.updateNeighborsAt(p,s.getBlock());for(var d:Direction.values())level.updateNeighborsAt(p.relative(d),s.getBlock());}
        if(level.getGameTime()%10==0 && (be.active!=be.sentActive || Math.abs(be.yaw-be.sentYaw)>.25 || Math.abs(be.pitch-be.sentPitch)>.25 || be.visualTarget.subtract(be.sentTarget).length()>.002 || be.visualUp.subtract(be.sentUp).length()>.002))be.sync();
    }
    private void sync(){if(level!=null && !level.isClientSide){sentTarget=visualTarget;sentUp=visualUp;sentActive=active;sentYaw=yaw;sentPitch=pitch;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}}
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider r){super.saveAdditional(n,r);n.putInt("MountLayout",1);n.putString("Mode",mode.name());n.putInt("Scale",scale);n.putBoolean("Active",active);n.putDouble("Yaw",yaw);n.putDouble("Pitch",pitch);writeVector(n,"VisualTarget",visualTarget);writeVector(n,"VisualUp",visualUp);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider r){super.loadAdditional(n,r);legacyMount=!n.contains("MountLayout");try{mode=OrbitalAttitude.Mode.valueOf(n.getString("Mode"));}catch(IllegalArgumentException e){mode=OrbitalAttitude.Mode.PROGRADE;}scale=n.contains("Scale")?Math.clamp(n.getInt("Scale"),5,180):45;active=n.getBoolean("Active");yaw=n.getDouble("Yaw");pitch=n.getDouble("Pitch");visualTarget=readVector(n,"VisualTarget",new SpaceVector(0,0,-1));visualUp=readVector(n,"VisualUp",new SpaceVector(0,1,0));}
    private static void writeVector(CompoundTag tag,String key,SpaceVector v){tag.putDouble(key+"X",v.x());tag.putDouble(key+"Y",v.y());tag.putDouble(key+"Z",v.z());}
    private static SpaceVector readVector(CompoundTag tag,String key,SpaceVector fallback){if(!tag.contains(key+"X"))return fallback;double x=tag.getDouble(key+"X"),y=tag.getDouble(key+"Y"),z=tag.getDouble(key+"Z");return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)?new SpaceVector(x,y,z):fallback;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
