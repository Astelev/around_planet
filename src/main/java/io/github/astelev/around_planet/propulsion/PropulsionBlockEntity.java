package io.github.astelev.around_planet.propulsion;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlock;
public final class PropulsionBlockEntity extends BlockEntity {
    private double actualThrust;
    public final io.github.astelev.around_planet.fuel.RocketReservoir rocket=new io.github.astelev.around_planet.fuel.RocketReservoir(this::fuelChanged);
    private double thrust=10000,strength=2;private final ItemStack[] frequencies=new ItemStack[8];private final int[] signals=new int[4];private int redstone;private Object links;private int sentEnergy=-1,sentSignal=-1;private final int[] sentSignals=new int[4];
    public final Reservoir energy=new Reservoir();
    public final class Reservoir extends net.neoforged.neoforge.energy.EnergyStorage {
        private Reservoir(){super(PropulsionMath.CAPACITY,100000,0);}
        public void stored(int value){energy=Math.clamp(value,0,PropulsionMath.CAPACITY);}
        private void spend(int value){energy=Math.max(0,energy-Math.max(0,value));}
        @Override public int receiveEnergy(int amount,boolean simulate){if(amount<=0)return 0;int n=super.receiveEnergy(amount,simulate);if(!simulate&&n>0)setChanged();return n;}
    }
    public PropulsionBlockEntity(BlockPos p,BlockState s){super(PropulsionRegistry.TYPE.get(),p,s);java.util.Arrays.fill(frequencies,ItemStack.EMPTY);if(kind()==PropulsionRegistry.Kind.RSU)thrust=PropulsionMath.RSU_MAX_THRUST;else if(kind()==PropulsionRegistry.Kind.LIQUID)thrust=50;}
    public PropulsionRegistry.Kind kind(){return ((PropulsionBlock)getBlockState().getBlock()).kind;}
    public double maximumThrust(){return kind()==PropulsionRegistry.Kind.LIQUID?1000:kind()==PropulsionRegistry.Kind.RSU?PropulsionMath.RSU_MAX_THRUST:10_000_000;}
    public double controlValue(){return thrust;}public double thrust(){return kind()==PropulsionRegistry.Kind.LIQUID?actualThrust*15/Math.max(1,redstone):thrust;}
    public double actualThrust(){return actualThrust;}
    public double burn(double dt){if(kind()!=PropulsionRegistry.Kind.LIQUID||!Double.isFinite(dt)||dt<=0)return 0;var result=rocket.burn(thrust*Math.clamp(redstone,0,15)/15.0*dt,io.github.astelev.around_planet.fuel.FuelMassBridge.kgPerMb(this,false),io.github.astelev.around_planet.fuel.FuelMassBridge.kgPerMb(this,true));actualThrust=result.impulse()/dt;return result.impulse();}
    private void fuelChanged(double fuelDelta,double oxygenDelta){double delta=fuelDelta*io.github.astelev.around_planet.fuel.FuelMassBridge.kgPerMb(this,false)+oxygenDelta*io.github.astelev.around_planet.fuel.FuelMassBridge.kgPerMb(this,true);if(level!=null&&!level.isClientSide&&net.neoforged.fml.ModList.get().isLoaded("sable"))io.github.astelev.around_planet.compat.RocketMassChange.changed(this,delta);setChanged();}
    public double strength(){return strength;}public int signal(int port){return kind()==PropulsionRegistry.Kind.RSU?signals[port]:redstone;}
    public ItemStack frequency(int index){return frequencies[index].copy();}
    public SpaceVector direction(int port){var f=OrbitalSensorBlock.frame(getBlockState());return kind()==PropulsionRegistry.Kind.RSU?f.model(switch(port){case 0->new SpaceVector(0,0,-1);case 1->new SpaceVector(1,0,0);case 2->new SpaceVector(0,0,1);default->new SpaceVector(-1,0,0);}).scale(-1):f.forward();}
    public void receiveSignal(int port,int value){signals[port]=Math.clamp(value,0,15);}
    public void configure(double value){if(!Double.isFinite(value))return;if(kind()==PropulsionRegistry.Kind.DAMPER)strength=Math.clamp(value,.1,20);else thrust=Math.clamp(value,0,maximumThrust());setChanged();sync();}
    public void frequency(int index,ItemStack stack){if(index<0||index>=8)return;unlink();frequencies[index]=stack.copyWithCount(1);setChanged();ensureLinks();sync();}
    public void spend(int value){energy.spend(value);setChanged();}
    public static void tick(Level l,BlockPos p,BlockState s,PropulsionBlockEntity be){
        PropulsionRuntime.add(be);be.redstone=l.getBestNeighborSignal(p);be.ensureLinks();boolean periodic=l.getGameTime()%10==0;
        if(periodic&&be.links!=null)io.github.astelev.around_planet.compat.RsuLinkBridge.refresh(be.links);
        if(be.kind()==PropulsionRegistry.Kind.LIQUID&&periodic||be.sentSignal!=be.redstone||!java.util.Arrays.equals(be.sentSignals,be.signals)||periodic&&be.sentEnergy!=be.energy.getEnergyStored())be.sync();
    }
    private void ensureLinks(){if(links==null&&level!=null&&!level.isClientSide&&kind()==PropulsionRegistry.Kind.RSU&&net.neoforged.fml.ModList.get().isLoaded("create"))links=io.github.astelev.around_planet.compat.RsuLinkBridge.attach(this);}
    public void stop(){unlink();}
    private void unlink(){if(links!=null){io.github.astelev.around_planet.compat.RsuLinkBridge.detach(links);links=null;}java.util.Arrays.fill(signals,0);}
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide){if(kind()==PropulsionRegistry.Kind.LIQUID)io.github.astelev.around_planet.fuel.FuelMassBridge.mass(this);PropulsionRuntime.add(this);}}
    @Override public void setRemoved(){unlink();PropulsionRuntime.remove(this);super.setRemoved();}
    @Override public void onChunkUnloaded(){unlink();PropulsionRuntime.remove(this);super.onChunkUnloaded();}
    private void sync(){if(level!=null&&!level.isClientSide){sentEnergy=energy.getEnergyStored();sentSignal=redstone;System.arraycopy(signals,0,sentSignals,0,4);level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}}

    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider r){super.saveAdditional(n,r);n.putDouble("Thrust",thrust);if(kind()==PropulsionRegistry.Kind.LIQUID){n.putDouble("HydrogenMb",rocket.fuel());n.putDouble("OxygenMb",rocket.oxidizer());n.putDouble("ActualThrust",actualThrust);}n.putDouble("Damping",strength);n.putInt("Energy",energy.getEnergyStored());n.putInt("Signal",redstone);for(int i=0;i<8;i++)if(!frequencies[i].isEmpty())n.put("Frequency"+i,frequencies[i].save(r));for(int i=0;i<4;i++)n.putInt("Signal"+i,signals[i]);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider r){super.loadAdditional(n,r);if(level!=null&&!level.isClientSide)unlink();thrust=n.contains("Thrust")&&Double.isFinite(n.getDouble("Thrust"))?Math.clamp(n.getDouble("Thrust"),0,maximumThrust()):kind()==PropulsionRegistry.Kind.LIQUID?50:kind()==PropulsionRegistry.Kind.RSU?PropulsionMath.RSU_MAX_THRUST:10000;strength=n.contains("Damping")&&Double.isFinite(n.getDouble("Damping"))?Math.clamp(n.getDouble("Damping"),.1,20):2;if(kind()==PropulsionRegistry.Kind.LIQUID){rocket.restore(n.getDouble("HydrogenMb"),n.getDouble("OxygenMb"));actualThrust=Double.isFinite(n.getDouble("ActualThrust"))?Math.clamp(n.getDouble("ActualThrust"),0,10_000_000):0;}if(n.contains("Energy"))energy.stored(n.getInt("Energy"));for(int i=0;i<8;i++)frequencies[i]=n.contains("Frequency"+i)?ItemStack.parseOptional(r,n.getCompound("Frequency"+i)):ItemStack.EMPTY;redstone=Math.clamp(n.getInt("Signal"),0,15);for(int i=0;i<4;i++)signals[i]=Math.clamp(n.getInt("Signal"+i),0,15);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
